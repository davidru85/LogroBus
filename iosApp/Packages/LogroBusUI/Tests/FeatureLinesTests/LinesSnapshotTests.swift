@testable import FeatureLines
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9 and spec §7.3: every state of the Lines list in light, dark and large text, in Spanish. At large text,
/// no code is cut and names wrap (LB-101). References live in __Snapshots__; a missing one is recorded, and that run
/// fails until it is committed.
@MainActor
struct LinesSnapshotTests {
    @Test(arguments: Appearance.allCases)
    func lb101_theLines(appearance: Appearance) async throws {
        let screen = try await screen(SharedDependencies.shared.preview(), settlesOn: LinesUiStateContent.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb101_theFirstLoad(appearance: Appearance) async throws {
        let useCases = SharedDependencies.shared.preview().loading()
        let screen = try await screen(useCases, settlesOn: LinesUiStateLoading.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb101_noLines(appearance: Appearance) async throws {
        let useCases = SharedDependencies.shared.preview().neverSynced()
        let screen = try await screen(useCases, settlesOn: LinesUiStateEmpty.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb101_aFirstLaunchWithNoNetwork(appearance: Appearance) async throws {
        let screen = try await screen(SharedDependencies.shared.preview().offline(), settlesOn: LinesUiStateError.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    /// The screen in its navigation stack, once its view model shows a `State`, so that the capture never races the
    /// first load. The view's own `.task` then finds the same state.
    private func screen<State>(_ useCases: PreviewUseCases, settlesOn _: State.Type) async throws -> some View {
        let viewModel = LinesViewModel(useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is State }
        return NavigationStack { LinesView(viewModel: viewModel) }
    }
}

/// Light, dark, and 200 % text or more (`accessibility3`), on a whole iPhone screen, as the app's ShellSnapshotTests.
enum Appearance: CaseIterable, Sendable {
    case light, dark, largeText

    var name: String {
        switch self {
        case .light: "light"
        case .dark: "dark"
        case .largeText: "largeText"
        }
    }

    @MainActor var strategy: Snapshotting<AnyView, UIImage> {
        .image(
            precision: 0.9995,
            perceptualPrecision: 0.98,
            layout: .device(config: .iPhone13),
            traits: UITraitCollection(traitsFrom: [
                UITraitCollection(userInterfaceStyle: self == .dark ? .dark : .light),
                UITraitCollection(displayScale: 3)
            ])
        )
    }

    @MainActor func apply(to view: some View) -> AnyView {
        AnyView(
            view
                .environment(\.dynamicTypeSize, self == .largeText ? .accessibility3 : .large)
                .environment(\.locale, Locale(identifier: "es"))
        )
    }
}
