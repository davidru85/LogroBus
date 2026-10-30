import Shared
import SwiftUI

@main
struct LogroBusApp: App {
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

    #if DEBUG
    private static let isDebugBuild = true
    #else
    private static let isDebugBuild = false
    #endif

    var body: some Scene {
        WindowGroup {
            AppShell()
        }
    }
}
