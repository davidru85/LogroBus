import FeatureSettings
import Shared
import SwiftUI
import UIKit

@main
struct LogroBusApp: App {
    // FR-SET-01: the theme chosen in Settings, from the saved one, so the first frame is in it (ADR-0020).
    @State private var appearance: AppAppearance

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
        // User defaults are read synchronously, so the saved theme is known before the first frame.
        let saved = savedPreferences(defaults: .standard)
        _appearance = State(initialValue: AppAppearance(initial: saved.theme))
    }

    #if DEBUG
    private static let isDebugBuild = true
    #else
    private static let isDebugBuild = false
    #endif

    var body: some Scene {
        WindowGroup {
            AppShell()
                // LB-405: one language on screen, the one iOS gave the app, with the device's region.
                .environment(\.locale, .app)
                .task { await appearance.observe() }
                // The windows' own style, so sheets and alerts follow it too.
                .onChange(of: appearance.interfaceStyle, initial: true) { _, style in
                    for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
                        AppAppearance.apply(style, to: scene.windows)
                    }
                }
        }
    }
}
