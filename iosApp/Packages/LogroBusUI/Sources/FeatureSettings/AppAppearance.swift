import Observation
@preconcurrency import Shared
import SwiftUI

/// FR-SET-01: the theme chosen in Settings, as the color scheme the app's root prefers; nil follows the system.
@MainActor
@Observable
public final class AppAppearance {
    public private(set) var colorScheme: ColorScheme?
    private let useCases: any UseCases

    public init(useCases: any UseCases = SharedDependencies.shared) {
        self.useCases = useCases
    }

    /// Runs for as long as the app: the root view calls it from `.task {}`.
    public func observe() async {
        for await preferences in useCases.observePreferences().invoke() {
            colorScheme = Self.colorScheme(of: preferences.theme)
        }
    }

    /// The color scheme of `theme`.
    static func colorScheme(of theme: ThemePreference) -> ColorScheme? {
        switch theme {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}
