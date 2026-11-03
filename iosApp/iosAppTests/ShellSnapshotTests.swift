import FeatureSettings
@testable import LogroBus
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9: More, and Settings' states (LB-403), in light, dark and at 200 % text, in Spanish: the scheme runs the
/// hosted tests in Spanish, because the app's strings follow the process language. Settings reads scripted use cases,
/// not the device's settings, which a UI test may have changed.
@MainActor
struct ShellSnapshotTests {
    @Test(arguments: ShellLook.allCases)
    func lb012_more(look: ShellLook) {
        assertSnapshot(of: look.apply(to: NavigationStack { MoreView() }), as: look.strategy, named: "more-\(look)")
    }

    /// Settings' states (LB-403): its first load, its content, and its content when the sync status cannot be read. At
    /// large text, on a screen tall enough for the Data section.
    @Test(arguments: SettingsState.allCases, ShellLook.allCases)
    func lb403_settings(state: SettingsState, look: ShellLook) async throws {
        // The content once the ViewModel has read the scripted use cases, as the screen's own task would.
        let settings = state.viewModel
        let observation = Task { await settings.observe() }
        defer { observation.cancel() }
        if state != .loading {
            try await waitUntil { settings.state is SettingsUiStateContent }
        }

        let view = look.apply(to: NavigationStack { SettingsView(viewModel: settings) })
        let strategy = look == .largeText ? look.strategy(height: 1_400) : look.strategy
        assertSnapshot(of: view, as: strategy, named: "\(state)-\(look)")
    }
}

/// Settings' states, each over scripted use cases, in Spanish, as the scheme runs the hosted tests.
enum SettingsState: CaseIterable, CustomStringConvertible, Sendable {
    case loading, content, unknownStatus

    var description: String {
        switch self {
        case .loading: "loading"
        case .content: "content"
        case .unknownStatus: "unknownStatus"
        }
    }

    /// A first load that never ends; the theme and clock times chosen, with the preview's data synced at 10:30 in
    /// Madrid; or a sync status that cannot be read.
    @MainActor var viewModel: SettingsViewModel {
        let useCases =
            switch self {
            case .loading: SharedDependencies.shared.preview().loading()
            case .content: SharedDependencies.shared.preview().settings(theme: .dark, etaFormat: .clockTime)
            case .unknownStatus: SharedDependencies.shared.preview().failing()
            }
        return SettingsViewModel(useCases: useCases, language: .spanish)
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
        strategy(layout: .device(config: .iPhone13))
    }

    /// The same, on an iPhone's width and `height` points, for a screen taller than the device.
    @MainActor func strategy(height: CGFloat) -> Snapshotting<AnyView, UIImage> {
        strategy(layout: .fixed(width: 390, height: height))
    }

    @MainActor private func strategy(layout: SwiftUISnapshotLayout) -> Snapshotting<AnyView, UIImage> {
        .image(
            precision: 0.9995,
            perceptualPrecision: 0.98,
            layout: layout,
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
