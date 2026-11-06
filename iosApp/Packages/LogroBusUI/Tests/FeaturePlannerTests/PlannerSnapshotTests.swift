import DesignSystem
@testable import FeaturePlanner
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9: How to Get There's root in light, dark and large text, in Spanish, with and without the location
/// permission. References live in __Snapshots__; a missing one is recorded, and that run fails until it is committed.
@MainActor
struct PlannerSnapshotTests {
    @Test(arguments: Appearance.allCases)
    func lb301_withLocation(appearance: Appearance) {
        let screen = screen(SharedDependencies.shared.preview().locationOn())
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    @Test(arguments: Appearance.allCases)
    func lb301_withoutLocation(appearance: Appearance) {
        let screen = screen(SharedDependencies.shared.preview())
        assertSnapshot(of: appearance.apply(to: screen), as: appearance.strategy, named: appearance.name)
    }

    /// The screen in its navigation stack, its fields already read, as the view's own `onAppear` reads them.
    private func screen(_ useCases: PreviewUseCases) -> some View {
        let viewModel = PlannerViewModel(useCases: useCases)
        viewModel.onEvent(PlannerEventShown.shared)
        return NavigationStack { PlannerView(viewModel: viewModel) }
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

    /// An iPhone 13 screen, with its safe area.
    @MainActor var strategy: Snapshotting<AnyView, UIImage> {
        let phone = ViewImageConfig.iPhone13
        return .image(
            precision: 0.9995,
            perceptualPrecision: 0.98,
            layout: .device(config: phone),
            traits: UITraitCollection { traits in
                traits.userInterfaceStyle = self == .dark ? .dark : .light
                traits.displayScale = 3
            }
        )
    }

    @MainActor func apply(to view: some View) -> AnyView {
        AnyView(
            view
                .environment(\.dynamicTypeSize, self == .largeText ? .accessibility3 : .large)
                .environment(\.locale, Locale(identifier: "es"))
                // The app's tint, as its root sets it (LB-406).
                .appTint()
        )
    }
}
