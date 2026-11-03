@testable import FeatureLines
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9 and spec §7.3: every state of the Lines list in light, dark and large text, in Spanish. At large text,
/// no code is cut and names wrap (LB-101). With no favourites, the carousel shows its hint (LB-103). A search shows its
/// results without the carousel, or a sentence and Clear search (LB-102). References live in __Snapshots__; a missing
/// one is recorded, and that run fails until it is committed.
@MainActor
struct LinesSnapshotTests {
    @Test(arguments: Appearance.allCases)
    func lb101_theLines(appearance: Appearance) async throws {
        // On a screen tall enough for the whole list, so every code, the status chip and the night group show.
        let screen = try await screen(SharedDependencies.shared.preview(), settlesOn: LinesUiStateContent.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy(height: appearance.wholeListHeight),
                       named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb103_theFavouriteLines(appearance: Appearance) async throws {
        // FR-LINE-13: the carousel's cards, in the order they were added, each as tall as the tallest.
        let useCases = SharedDependencies.shared.preview()
            .favouriteLine(id: 10).favouriteLine(id: 4).favouriteLine(id: 101)
        let screen = try await screen(useCases, settlesOn: LinesUiStateContent.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb102_aSearch(appearance: Appearance) async throws {
        // FR-LINE-16: the lines it found, in their groups, without the favourites; line 4 keeps its filled star.
        let useCases = SharedDependencies.shared.preview().favouriteLine(id: 4)
        let screen = try await screen(useCases, searching: "ca", settlesOn: LinesUiStateSearchResults.self)
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb102_aSearchThatFindsNothing(appearance: Appearance) async throws {
        let screen = try await screen(
            SharedDependencies.shared.preview(), searching: "tranvía", settlesOn: LinesUiStateNoMatches.self
        )
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
    /// first load. The view's own `.task` then finds the same state. With `query`, the search field holds it.
    private func screen<State>(
        _ useCases: PreviewUseCases,
        searching query: String = "",
        settlesOn _: State.Type
    ) async throws -> some View {
        let viewModel = LinesViewModel(useCases: useCases)
        viewModel.onEvent(LinesEventSearch(query: query))
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

    /// The height that holds the whole sample list, at this appearance's text size.
    var wholeListHeight: CGFloat {
        self == .largeText ? 3200 : 1400
    }

    @MainActor var strategy: Snapshotting<AnyView, UIImage> {
        strategy(height: ViewImageConfig.iPhone13.size?.height ?? 844)
    }

    /// An iPhone 13 screen, with its safe area, `height` points tall.
    @MainActor func strategy(height: CGFloat) -> Snapshotting<AnyView, UIImage> {
        let phone = ViewImageConfig.iPhone13
        let size = CGSize(width: phone.size?.width ?? 390, height: height)
        let config = ViewImageConfig(safeArea: phone.safeArea, size: size, traits: phone.traits)
        return .image(
            precision: 0.9995,
            perceptualPrecision: 0.98,
            layout: .device(config: config),
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
