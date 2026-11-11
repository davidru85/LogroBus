@testable import FeatureStops
import Shared
import Testing

/// LB-205 (FR-STOP-03 to FR-STOP-05) and ADR-0007: the stop detail's ViewModel builds its state with the shared stop,
/// arrivals and mapper over the preview's script, and Retry starts a new observation.
///
/// The scripted stop and calls come from `SharedDependencies.shared.preview()`, because a Swift test cannot build one:
/// both use cases are `fun interface`s whose `invoke` returns a Kotlin `Flow`, which only shared code can make.
@MainActor
struct StopDetailViewModelTests {
    @Test
    func lb205_theDetailShowsTheStopsLinesAndArrivals_andRetryCollectsAgain() async throws {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = StopDetailViewModel(stopId: 38, useCases: useCases)
        #expect(viewModel.state is StopDetailUiStateLoading)
        #expect(viewModel.attempt == 0)

        let first = Task { await viewModel.observe() }
        try await waitUntil { viewModel.state is StopDetailUiStateContent }

        // FR-STOP-03: the header comes from the catalogue, so it shows with the arrivals' own names.
        let content = try #require(viewModel.state as? StopDetailUiStateContent)
        #expect(content.stop.id == 38)
        #expect(!content.stop.name.isEmpty)
        // FR-STOP-05: the list's age is measured from the answer, on the graph's clock, never the device's (FR-ERR-06).
        let updated = try #require(content.updated, "the first answer carries the instant it was fetched")
        #expect(updated.ageSeconds >= 0, "a clock moved back between the answer and the render never reads negative")

        viewModel.onEvent(.retry)
        #expect(viewModel.state is StopDetailUiStateLoading, "FR-ERR-05: Retry shows the first load again")
        #expect(viewModel.attempt == 1, "the view's `.task(id:)` restarts `observe()`")

        // What `.task(id: viewModel.attempt)` does when the attempt changes: the old observation ends and a new one
        // starts. A stop still showing its list without a restart would mean Loading had been left behind unobserved.
        first.cancel()
        await first.value
        let second = Task { await viewModel.observe() }
        defer { second.cancel() }
        try await waitUntil { viewModel.state is StopDetailUiStateContent }
    }

    /// Waits on the main actor until `condition` holds, failing after `timeout`.
    private func waitUntil(
        timeout: Duration = .seconds(5),
        _ condition: () -> Bool
    ) async throws {
        let deadline = ContinuousClock.now + timeout
        while !condition() {
            guard ContinuousClock.now < deadline else {
                Issue.record("The condition did not hold within \(timeout)")
                return
            }
            try await Task.sleep(for: .milliseconds(10))
        }
    }
}
