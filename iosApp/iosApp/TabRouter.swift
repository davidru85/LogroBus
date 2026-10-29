import Observation
import Shared

enum Route: Hashable {
    case more
    case settings
    case aboutData
}

@MainActor
@Observable
final class TabRouter {
    private(set) var selection: TopLevelDestination = .lines
    var paths: [TopLevelDestination: [Route]] = [:]
    private(set) var scrollToTopRequests: [TopLevelDestination: Int] = [:]

    func select(_ destination: TopLevelDestination) {}
}
