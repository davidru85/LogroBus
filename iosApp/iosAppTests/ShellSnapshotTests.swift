import FeatureSettings
@testable import LogroBus
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9: More and the Settings placeholder, pushed on a tab's stack, in light, dark and at 200 % text, in
/// Spanish: the scheme runs the hosted tests in Spanish, because the app's strings follow the process language.
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

    /// The screen as a tab shows it: pushed onto a stack, under its navigation bar.
    @MainActor var view: some View {
        NavigationStack(path: .constant([self == .more ? Route.more : Route.settings])) {
            Color.clear
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .more: MoreView()
                    default: SettingsView()
                    }
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

    /// A whole iPhone screen at a fixed 3× scale, with the component snapshots' precision.
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
        AnyView(view.environment(\.dynamicTypeSize, self == .largeText ? .accessibility3 : .large))
    }
}
