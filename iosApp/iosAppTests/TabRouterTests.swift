@testable import LogroBus
import Shared
import Testing

/// LB-012: the selected tab and one navigation path per tab (FR-NAV-03). Re-selecting a tab pops it to its root, or,
/// at its root, asks the root to scroll to its top.
@MainActor
struct TabRouterTests {

    @Test
    func lb012_theAppStartsOnLines() {
        // FR-NAV-02, OQ-15.
        #expect(TabRouter().selection == .lines)
    }

    @Test
    func lb012_eachTabKeepsItsOwnPath() {
        let router = TabRouter()
        router.paths[.lines] = [.more, .aboutData]

        router.select(.nearMe)

        #expect(router.selection == .nearMe)
        #expect(router.paths[.nearMe, default: []].isEmpty)
        router.select(.lines)
        #expect(router.paths[.lines] == [.more, .aboutData])
    }

    @Test
    func lb012_reselectingATabPopsItToItsRoot_withoutScrolling() {
        let router = TabRouter()
        router.paths[.lines] = [.more, .aboutData]

        router.select(.lines)

        #expect(router.paths[.lines, default: []].isEmpty)
        #expect(router.scrollToTopRequests[.lines, default: 0] == 0)
    }

    @Test
    func lb012_reselectingATabAtItsRootAsksItToScrollToTheTop() {
        let router = TabRouter()

        router.select(.lines)
        router.select(.lines)

        #expect(router.scrollToTopRequests[.lines, default: 0] == 2)
    }

    @Test
    func lb012_selectingAnotherTabAsksNoTabToScroll() {
        let router = TabRouter()

        router.select(.planner)

        #expect(router.scrollToTopRequests.values.allSatisfy { $0 == 0 })
    }
}
