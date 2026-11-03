import FeatureSettings
@testable import LogroBus
import Shared
import SnapshotTesting
import SwiftUI
import Testing

/// AGENTS.md §9: More, and Settings' first load and content (LB-403), in light, dark and at 200 % text, in Spanish: the
/// scheme runs the hosted tests in Spanish, because the app's strings follow the process language. Settings reads
/// scripted use cases, not the device's settings, which a UI test may have changed.
@MainActor
struct ShellSnapshotTests {
    @Test(arguments: ShellScreen.allCases, ShellLook.allCases)
    func lb012_theScreensMoreOpens(screen: ShellScreen, look: ShellLook) async throws {
        // Settings' content once its ViewModel has read the scripted use cases, as the screen's own task would.
        let settings = screen.settings
        let observation = settings.map { viewModel in Task { await viewModel.observe() } }
        defer { observation?.cancel() }
        if screen == .settingsContent, let settings {
            try await waitUntil { settings.state is SettingsUiStateContent }
        }

        let view = look.apply(to: screen.view(settings: settings))
        assertSnapshot(of: view, as: look.strategy, named: "\(screen)-\(look)")
    }
}

enum ShellScreen: CaseIterable, CustomStringConvertible, Sendable {
    case more, settingsLoading, settingsContent

    var description: String {
        switch self {
        case .more: "more"
        case .settingsLoading: "settingsLoading"
        case .settingsContent: "settingsContent"
        }
    }

    /// Settings' ViewModel over scripted use cases, in Spanish, as the scheme runs the hosted tests: a first load that
    /// never ends, or the theme and clock times chosen with the preview's data synced at 10:30 in Madrid.
    @MainActor var settings: SettingsViewModel? {
        switch self {
        case .more: nil
        case .settingsLoading:
            SettingsViewModel(useCases: SharedDependencies.shared.preview().loading(), language: .spanish)
        case .settingsContent:
            SettingsViewModel(
                useCases: SharedDependencies.shared.preview().settings(theme: .dark, etaFormat: .clockTime),
                language: .spanish
            )
        }
    }

    /// The screen at the root of a stack, under its navigation bar's large title. As a root it has no back button,
    /// whose Liquid Glass would need the key window; the glass is GlassSnapshotTests' concern.
    @MainActor func view(settings: SettingsViewModel?) -> some View {
        NavigationStack {
            if let settings {
                SettingsView(viewModel: settings)
            } else {
                MoreView()
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
