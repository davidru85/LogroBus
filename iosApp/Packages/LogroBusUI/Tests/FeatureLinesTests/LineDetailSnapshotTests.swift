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
        let screen = try await screen(SharedDependencies.shared.preview(), settlesOn: LineDetailUiStateContent.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb103_aFavouriteLine(appearance: Appearance) async throws {
        let useCases = SharedDependencies.shared.preview().favouriteLine(id: 10)
        let screen = try await screen(useCases, settlesOn: LineDetailUiStateContent.self)
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

    /// Line 10's detail in its navigation stack, once its view model shows a `State`.
    private func screen<State>(_ useCases: PreviewUseCases, settlesOn _: State.Type) async throws -> some View {
        let viewModel = LineDetailViewModel(lineId: 10, useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is State }
        return NavigationStack { LineDetailView(viewModel: viewModel) }
    }
}
