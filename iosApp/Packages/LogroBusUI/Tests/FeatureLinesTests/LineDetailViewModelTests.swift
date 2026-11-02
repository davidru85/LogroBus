@testable import FeatureLines
import Shared
import Testing

/// LB-103 (FR-LINE-09) and ADR-0007: the line detail's ViewModel builds its state with the shared use case and mapper,
/// and its toolbar's toggle sets the line as a favourite.
@MainActor
struct LineDetailViewModelTests {
    @Test
    func lb103_theDetailLoads_thenShowsItsLine() async throws {
        let viewModel = LineDetailViewModel(lineId: 3, useCases: SharedDependencies.shared.preview())
        #expect(viewModel.state is LineDetailUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LineDetailUiStateContent }

        let line = try #require((viewModel.state as? LineDetailUiStateContent)?.line)
        #expect(line.code == "3")
        #expect(line.displayName == "Campillo - El Cubo")
        #expect(!line.isFavourite)
    }

    @Test
    func lb103_theToolbarsToggleSetsTheLineAsAFavourite_orNot() async throws {
        let viewModel = LineDetailViewModel(lineId: 3, useCases: SharedDependencies.shared.preview())
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LineDetailUiStateContent }

        viewModel.onEvent(LineDetailEventSetFavourite(isFavourite: true))
        try await waitUntil { isFavourite(viewModel) == true }

        viewModel.onEvent(LineDetailEventSetFavourite(isFavourite: false))
        try await waitUntil { isFavourite(viewModel) == false }
    }

    @Test
    func lb103_aFirstLaunchWithNoNetworkShowsTheNetworkError_andRetryLoadsTheLine() async throws {
        let useCases = SharedDependencies.shared.preview().offline()
        let viewModel = LineDetailViewModel(lineId: 3, useCases: useCases)

        let first = Task { await viewModel.observe() }
        try await waitUntil { viewModel.state is LineDetailUiStateError }
        #expect((viewModel.state as? LineDetailUiStateError)?.reason == .network)

        useCases.synced()
        viewModel.onEvent(LineDetailEventRetry.shared)
        #expect(viewModel.state is LineDetailUiStateLoading)
        #expect(viewModel.attempt == 1)

        // What `.task(id: viewModel.attempt)` does when the attempt changes.
        first.cancel()
        await first.value
        let second = Task { await viewModel.observe() }
        defer { second.cancel() }
        try await waitUntil { viewModel.state is LineDetailUiStateContent }
    }

    private func isFavourite(_ viewModel: LineDetailViewModel) -> Bool? {
        (viewModel.state as? LineDetailUiStateContent)?.line.isFavourite
    }
}
