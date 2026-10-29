import SwiftUI

extension EnvironmentValues {
    /// How many times the tab has been re-selected at its root (FR-NAV-03). The app shell sets it on every tab root; a
    /// root with a scrolling list scrolls to its top whenever it changes. Zero anywhere else.
    @Entry public var scrollToTopRequests: Int = 0
}
