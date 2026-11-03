import Observation
@preconcurrency import Shared
import SwiftUI
import UIKit

/// FR-SET-01: the theme chosen in Settings, as the color scheme the app's root prefers; nil follows the system.
@MainActor
@Observable
public final class AppAppearance {
    public private(set) var colorScheme: ColorScheme?
    private let useCases: any UseCases

    /// Starts from the `initial` theme, which the app reads at once from the saved settings, so its first frame is in
    /// it; `observe()` then follows the changes.
    public init(useCases: any UseCases = SharedDependencies.shared, initial: ThemePreference = .system) {
        self.useCases = useCases
    }

    /// The windows' interface style for the theme; unspecified follows the system.
    public var interfaceStyle: UIUserInterfaceStyle { .unspecified }

    /// Runs for as long as the app: the root view calls it from `.task {}`.
    public func observe() async {
        for await preferences in useCases.observePreferences().invoke() {
            colorScheme = Self.colorScheme(of: preferences.theme)
        }
    }

    /// Gives `windows` the interface `style`, so their sheets and alerts follow it too.
    public static func apply(_ style: UIUserInterfaceStyle, to windows: [UIWindow]) {}

    /// The color scheme of `theme`.
    static func colorScheme(of theme: ThemePreference) -> ColorScheme? {
        switch theme {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}
