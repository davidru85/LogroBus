import Shared
import SwiftUI

@main
struct LogroBusApp: App {
    init() {
        // ADR-0014: the shared dependency graph starts once, on the main thread, before any screen. It gets the
        // version for the User-Agent (spec §8.7) and the build type for the log level (security_privacy.md §4).
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String
        doInitKoin(appVersion: version ?? "unknown", debug: Self.isDebugBuild)
    }

    #if DEBUG
    private static let isDebugBuild = true
    #else
    private static let isDebugBuild = false
    #endif

    var body: some Scene {
        WindowGroup {
            StartDestinationPlaceholder()
        }
    }
}
