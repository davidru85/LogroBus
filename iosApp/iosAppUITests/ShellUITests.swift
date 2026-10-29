import XCTest

/// LB-012: the three tabs (FR-NAV-01, FR-NAV-02), each with its own stack (FR-NAV-03), and More from every tab root
/// (FR-NAV-04). "Two screens deep in Lines" is Lines, More, About data.
final class ShellUITests: XCTestCase {

    @MainActor
    func test_lb012_theTabsAreInOrder_withLinesSelected() {
        let tabs = launch().tabBars.buttons

        XCTAssertTrue(tabs["Lines"].waitForExistence(timeout: 10))
        XCTAssertEqual((0..<tabs.count).map { tabs.element(boundBy: $0).label }, ["Lines", "Near me", "Directions"])
        XCTAssertTrue(tabs["Lines"].isSelected)
    }

    @MainActor
    func test_lb012_eachTabRootOpensMore_withSettingsAndAboutData() {
        let app = launch()

        for tab in ["Lines", "Near me", "Directions"] {
            app.tabBars.buttons[tab].tap()
            app.navigationBars[tab].buttons["More"].tap()

            XCTAssertTrue(app.buttons["Settings"].waitForExistence(timeout: 5), "More lists Settings, from \(tab)")
            XCTAssertTrue(app.buttons["About data"].exists, "More lists About data, from \(tab)")
        }
    }

    @MainActor
    func test_lb012_aTabKeepsItsScreen_andReselectingItPopsToItsRoot() {
        let app = launch()
        app.navigationBars["Lines"].buttons["More"].tap()
        app.buttons["About data"].tap()
        XCTAssertTrue(app.navigationBars["About data"].waitForExistence(timeout: 5))

        app.tabBars.buttons["Near me"].tap()
        XCTAssertTrue(app.navigationBars["Near me"].waitForExistence(timeout: 5))
        app.tabBars.buttons["Lines"].tap()
        XCTAssertTrue(app.navigationBars["About data"].waitForExistence(timeout: 5), "Lines kept About data")

        app.tabBars.buttons["Lines"].tap()
        XCTAssertTrue(app.navigationBars["Lines"].waitForExistence(timeout: 5), "re-selecting Lines popped it")
    }

    @MainActor
    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_GB"]
        app.launch()
        return app
    }
}
