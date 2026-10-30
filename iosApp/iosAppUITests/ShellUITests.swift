import XCTest

/// LB-012: the three tabs (FR-NAV-01, FR-NAV-02), each with its own stack (FR-NAV-03), and More from every tab root
/// (FR-NAV-04). "Two screens deep in Lines" is Lines, More, About data.
final class ShellUITests: XCTestCase {

    @MainActor
    func test_lb012_theTabsAreInOrder_withLinesSelected() {
        assertTabs(["Lines", "Near me", "Directions"], in: launch())
    }

    @MainActor
    func test_lb012_theTabsAreInOrder_withLinesSelected_inSpanish() {
        assertTabs(["Líneas", "Cerca de mí", "Cómo llegar"], in: launch(language: "es", region: "es_ES"))
    }

    @MainActor
    func test_lb012_eachTabRootOpensMore_withSettingsAndAboutData() {
        let app = launch()

        // design_guidelines.md §2.1: the How to Get There tab says "Directions"; its screen says "How to get there".
        for (tab, title) in [("Lines", "Lines"), ("Near me", "Near me"), ("Directions", "How to get there")] {
            app.tabBars.buttons[tab].tap()
            app.navigationBars[title].buttons["More"].tap()

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
    private func assertTabs(_ labels: [String], in app: XCUIApplication) {
        let tabs = app.tabBars.buttons
        XCTAssertTrue(tabs[labels[0]].waitForExistence(timeout: 10))
        XCTAssertEqual((0..<tabs.count).map { tabs.element(boundBy: $0).label }, labels)
        XCTAssertTrue(tabs[labels[0]].isSelected)
    }

    @MainActor
    private func launch(language: String = "en", region: String = "en_GB") -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(\(language))", "-AppleLocale", region]
        app.launch()
        return app
    }
}
