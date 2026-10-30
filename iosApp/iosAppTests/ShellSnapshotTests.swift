import FeatureSettings
@testable import LogroBus
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9: More and the Settings placeholder in light, dark and at 200 % text, in Spanish: the scheme runs the
/// hosted tests in Spanish, because the app's strings follow the process language.
@MainActor
struct ShellSnapshotTests {
    @Test(arguments: ShellScreen.allCases, ShellLook.allCases)
    func lb012_theScreensMoreOpens(screen: ShellScreen, look: ShellLook) {
        assertSnapshot(of: look.apply(to: screen.view), as: look.strategy, named: "\(screen)-\(look)")
    }
}

enum ShellScreen: CaseIterable, CustomStringConvertible, Sendable {
    case more, settings

    var description: String {
        switch self {
        case .more: "more"
        case .settings: "settings"
        }
    }

    /// The screen at the root of a stack, under its navigation bar's large title. As a root it has no back button,
    /// whose Liquid Glass would need the key window; the glass is GlassSnapshotTests' concern.
    @MainActor var view: some View {
        NavigationStack {
            switch self {
            case .more: MoreView()
            case .settings: SettingsView()
            }
        }
    }
}

enum ShellLook: CaseIterable, CustomStringConvertible, Sendable {
    case light, dark, largeText

    var description: String {
        switch self {
        case .light: "light"
        case .dark: "dark"
        case .largeText: "largeText"
        }
    }

    /// A whole iPhone screen at a fixed 3× scale, with the component snapshots' precision: at 0.9995 of 1170 × 2532
    /// pixels, a missing title or row label (thousands of pixels) fails.
    @MainActor var strategy: Snapshotting<AnyView, UIImage> {
        .image(
            precision: 0.9995,
            perceptualPrecision: 0.98,
            layout: .device(config: .iPhone13),
            traits: UITraitCollection { traits in
                traits.userInterfaceStyle = self == .dark ? .dark : .light
                traits.displayScale = 3
            }
        )
    }

    @MainActor func apply(to view: some View) -> AnyView {
        AnyView(view.environment(\.dynamicTypeSize, self == .largeText ? .accessibility3 : .large))
    }
}
