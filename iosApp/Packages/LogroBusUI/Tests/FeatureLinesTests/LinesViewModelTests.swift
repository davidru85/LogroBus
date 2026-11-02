@testable import FeatureLines
import Foundation
import Shared
import Testing

/// LB-101, ADR-0007 and architecture.md §5.2: the Lines ViewModel builds its state with the shared use case and mapper,
/// retries on Retry, and leaving its screen stops the use case's collection.
@MainActor
struct LinesViewModelTests {
    @Test
    func lb101_theViewModelLoads_thenShowsDayLinesThenNightLines_eachSortedByCode() async throws {
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview())
        #expect(viewModel.state is LinesUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LinesUiStateContent }

        let content = try #require(viewModel.state as? LinesUiStateContent)
        #expect(content.groups.map(\.kind) == [.day, .night])
        #expect(content.groups.map { $0.lines.map(\.code) } == [
            ["1", "2", "3", "4", "5", "6", "7", "9", "10", "11"],
            ["B1", "B2", "B3"]
        ])
        // The sample's line 11 is not operational, so its row shows the status chip (FR-LINE-02).
        #expect(content.groups[0].lines.filter { !$0.isOperational }.map(\.code) == ["11"])
    }

    @Test
    func lb101_aFirstLaunchWithNoNetworkShowsTheNetworkError_andRetryLoadsTheLines() async throws {
        let useCases = SharedDependencies.shared.preview().offline()
        let viewModel = LinesViewModel(useCases: useCases)

        let first = Task { await viewModel.observe() }
        try await waitUntil { viewModel.state is LinesUiStateError }
        let error = try #require(viewModel.state as? LinesUiStateError)
        #expect(error.reason == .network)

        // Connectivity returns, and the user taps Retry.
        useCases.synced()
        viewModel.onEvent(LinesEventRetry.shared)
        #expect(viewModel.state is LinesUiStateLoading)
        #expect(viewModel.attempt == 1)

        // What `.task(id: viewModel.attempt)` does when the attempt changes: the old load stops, a new one starts.
        first.cancel()
        let second = Task { await viewModel.observe() }
        defer { second.cancel() }
        try await waitUntil { viewModel.state is LinesUiStateContent }
    }

    @Test
    func lb101_noLinesShowsTheEmptyState() async throws {
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview().neverSynced())

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }

        try await waitUntil { viewModel.state is LinesUiStateEmpty }
    }

    @Test
    func lb101_theStateStaysLoadingUntilTheLinesArrive() async throws {
        let useCases = SharedDependencies.shared.preview().loading()
        let viewModel = LinesViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        // Subscribed, and still nothing to show.
        try await waitUntil { useCases.activeCollections == 1 }

        #expect(viewModel.state is LinesUiStateLoading)
    }

    @Test
    func lb101_leavingTheScreenStopsTheUseCasesCollection() async throws {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = LinesViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        try await waitUntil { useCases.activeCollections == 1 }
        // What `.task {}` does when the screen leaves.
        observation.cancel()
        await observation.value

        try await waitUntil { useCases.activeCollections == 0 }
    }
}

/// Waits on the main actor until `condition` holds, failing after `timeout`.
@MainActor
func waitUntil(
    timeout: Duration = .seconds(5),
    _ condition: () -> Bool,
    sourceLocation: SourceLocation = #_sourceLocation
) async throws {
    let deadline = ContinuousClock.now + timeout
    while !condition() {
        guard ContinuousClock.now < deadline else {
            Issue.record("The condition did not hold within \(timeout)", sourceLocation: sourceLocation)
            return
        }
        try await Task.sleep(for: .milliseconds(10))
    }
}
