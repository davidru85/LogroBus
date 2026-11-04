@testable import FeatureSettings
@preconcurrency import Shared
import SwiftUI
import Testing
import UIKit

/// FR-SET-01 on iOS: the app's root follows the theme chosen in Settings.
@MainActor
struct AppAppearanceTests {
    @Test
    func lb403_eachThemeIsItsColorScheme() {
        #expect(AppAppearance.colorScheme(of: .system) == nil)
        #expect(AppAppearance.colorScheme(of: .light) == .light)
        #expect(AppAppearance.colorScheme(of: .dark) == .dark)
    }

    @Test
    func lb403_theFirstFrameIsInTheSavedTheme() {
        // ADR-0020: the app reads the saved theme at once, rather than wait for the store's first value.
        let appearance = AppAppearance(useCases: SharedDependencies.shared.preview(), initial: .dark)

        #expect(appearance.colorScheme == .dark)
        #expect(appearance.interfaceStyle == .dark)
    }

    @Test
    func lb403_theThemeIsTheWindowsStyle_soSheetsAndAlertsFollowIt() {
        let window = UIWindow()

        AppAppearance.apply(.dark, to: [window])
        #expect(window.overrideUserInterfaceStyle == .dark)

        // The system's theme again.
        AppAppearance.apply(.unspecified, to: [window])
        #expect(window.overrideUserInterfaceStyle == .unspecified)
    }

    @Test
    func lb403_theAppFollowsTheThemeChosenInSettings() async throws {
        let useCases = SharedDependencies.shared.preview()
        let appearance = AppAppearance(useCases: useCases)
        let observation = Task { await appearance.observe() }
        defer { observation.cancel() }

        _ = try await useCases.setTheme().invoke(theme: .dark)

        try await waitUntil { appearance.colorScheme == .dark }
        #expect(appearance.interfaceStyle == .dark)
    }
}
