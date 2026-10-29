import Observation
import Shared

/// The screens a tab can push onto its stack. The shell resolves each to its view, so a feature names another
/// feature's screen without depending on it (architecture.md §6).
enum Route: Hashable {
    case more
    case settings
    case aboutData
}

/// The selected tab and one navigation path per tab (FR-NAV-03). Every tab keeps its path while another is selected.
@MainActor
@Observable
final class TabRouter {
    /// FR-NAV-02: the app always opens on Lines (OQ-15).
    private(set) var selection: TopLevelDestination = SharedDependencies.shared.startDestination
    var paths: [TopLevelDestination: [Route]] = [:]
    /// How many times each tab has been re-selected at its root. A scrolling root scrolls to its top on every change.
    private(set) var scrollToTopRequests: [TopLevelDestination: Int] = [:]

    /// Selects [destination]. Re-selecting the selected tab pops it to its root, or, at its root, asks the root to
    /// scroll to its top.
    func select(_ destination: TopLevelDestination) {
        if destination != selection {
            selection = destination
        } else if !paths[destination, default: []].isEmpty {
            paths[destination] = []
        } else {
            scrollToTopRequests[destination, default: 0] += 1
        }
    }
}
