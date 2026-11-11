@testable import FeatureLines
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9 and spec §7.3: every state of the line detail (LB-103) in light, dark and large text, in Spanish.
/// References live in __Snapshots__; a missing one is recorded, and that run fails until it is committed.
@MainActor
struct LineDetailSnapshotTests {
    @Test(arguments: Appearance.allCases)
    func lb103_theLine(appearance: Appearance) async throws {
        let screen = try await screen(
            SharedDependencies.shared.preview().withArrivals(),
            settlesOn: LineDetailUiStateContent.self,
            withArrivals: true
        )
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb103_aFavouriteLine(appearance: Appearance) async throws {
        let useCases = SharedDependencies.shared.preview().withArrivals().favouriteLine(id: 10)
        let screen = try await screen(useCases, settlesOn: LineDetailUiStateContent.self, withArrivals: true)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb103_theFirstLoad(appearance: Appearance) async throws {
        let useCases = SharedDependencies.shared.preview().loading()
        let screen = try await screen(useCases, settlesOn: LineDetailUiStateLoading.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb103_aFirstLaunchWithNoNetwork(appearance: Appearance) async throws {
        let useCases = SharedDependencies.shared.preview().offline()
        let screen = try await screen(useCases, settlesOn: LineDetailUiStateError.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    /// Line 10's detail in its navigation stack, once its view model shows a `State`. With [withArrivals], it also
    /// waits for a row that carries a call, which a second collection delivers: a snapshot taken before them draws
    /// every row's dash, and whether it does is a race, not a property of the screen. Waiting for the map to be merely
    /// non-empty is not enough, because it holds one row per stop from the first frame, each with no ETA (PR #62).
    private func screen<State>(
        _ useCases: PreviewUseCases,
        settlesOn _: State.Type,
        withArrivals: Bool = false
    ) async throws -> some View {
        let viewModel = LineDetailViewModel(lineId: 10, useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is State }
        if withArrivals {
            try await waitUntil { viewModel.stopArrivals.values.contains { $0.eta != nil } }
        }
        return NavigationStack { LineDetailView(viewModel: viewModel) }
    }
}
