import FeatureSettings
import Shared
import SwiftUI
import UIKit

@main
struct LogroBusApp: App {
    // FR-SET-01: the theme chosen in Settings. Its init only keeps the use cases, so it may come before the graph.
    @State private var appearance = AppAppearance()

    init() {
        // ADR-0014: the shared dependency graph starts once, on the main thread, before any screen. It gets the
        // version for the User-Agent (spec §8.7) and the build type for the log level (security_privacy.md §4).
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String
        // LB-409 passes the vendor's analytics tracker and crash reporter in PlatformTelemetry (ADR-0009).
        doInitKoin(
            appVersion: version ?? "unknown",
            debug: Self.isDebugBuild,
            telemetry: PlatformTelemetry(analytics: nil, crashReporter: nil)
        )
    }

    /// The window style of a color scheme; nil follows the system.
    private static func style(of scheme: ColorScheme?) -> UIUserInterfaceStyle {
        switch scheme {
        case .light: .light
        case .dark: .dark
        default: .unspecified
        }
    }

    #if DEBUG
    private static let isDebugBuild = true
    #else
    private static let isDebugBuild = false
    #endif

    var body: some Scene {
        WindowGroup {
            AppShell()
                .task { await appearance.observe() }
                // The window's own style, so sheets and alerts follow it too, and nil gives the system's back.
                .onChange(of: appearance.colorScheme, initial: true) { _, scheme in
                    for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
                        for window in scene.windows { window.overrideUserInterfaceStyle = Self.style(of: scheme) }
                    }
                }
        }
    }
}
