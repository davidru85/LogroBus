import Shared
import SwiftUI

@main
struct LogroBusApp: App {
    init() {
        // ADR-0014: the shared dependency graph starts once, on the main thread, before any screen.
        doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            StartDestinationPlaceholder()
        }
    }
}
