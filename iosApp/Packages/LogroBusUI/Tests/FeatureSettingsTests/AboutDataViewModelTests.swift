@testable import FeatureSettings
import Shared
import Testing

/// ADR-0007 and architecture.md §5.2: a Swift ViewModel builds its state with the shared use case and mapper, and
/// leaving its screen stops the use case's collection.
@MainActor
struct AboutDataViewModelTests {
    @Test
    func lb010_theViewModelLoads_thenShowsWhatTheSharedMapperMakesOfTheUseCase() async throws {
        let viewModel = AboutDataViewModel(useCases: SharedDependencies.shared.preview())
        #expect(viewModel.state is AboutDataUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is AboutDataUiStateContent }

        let content = try #require(viewModel.state as? AboutDataUiStateContent)
        #expect(content.lastSyncs.map(\.dataset) == [.lines, .stops])
        // The preview's syncs happened at 09:30 UTC, which is 10:30 in Madrid (AGENTS.md §5.5).
        #expect(content.lastSyncs.map { $0.lastSuccess?.hour } == [10, 10])
    }

    @Test
    func lb010_leavingTheScreenStopsTheUseCasesCollection() async throws {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = AboutDataViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        try await waitUntil { useCases.activeCollections == 1 }
        // What `.task {}` does when the screen leaves.
        observation.cancel()
        await observation.value

        try await waitUntil { useCases.activeCollections == 0 }
    }

    @Test
    func lb010_theStateStaysLoadingUntilTheUseCaseEmits() async throws {
        let viewModel = AboutDataViewModel(useCases: SharedDependencies.shared.preview().loading())

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await Task.sleep(for: .milliseconds(200))

        #expect(viewModel.state is AboutDataUiStateLoading)
    }
}

/// Waits on the main actor until `condition` holds, failing after `timeout`.
@MainActor
private func waitUntil(
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
