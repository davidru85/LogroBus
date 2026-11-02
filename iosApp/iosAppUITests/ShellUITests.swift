import XCTest

/// LB-012: the three tabs (FR-NAV-01, FR-NAV-02), each with its own stack (FR-NAV-03), and More from every tab root
/// (FR-NAV-04). "Two screens deep in Lines" is Lines, More, About data. LB-101: the Lines list at the Lines root.
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
    func test_lb101_reselectingLinesAtItsRootScrollsTheListToItsTop() {
        // FR-NAV-03 end to end through the shell, moved from LB-012. Debug builds list the sample lines, and the
        // largest text makes them far longer than the screen.
        let app = launch(textSize: "UICTContentSizeCategoryAccessibilityXL")
        let firstLine = line("1", in: app)
        XCTAssertTrue(firstLine.waitForExistence(timeout: 10))

        for _ in 0..<4 { app.swipeUp() }
        XCTAssertFalse(firstLine.exists && firstLine.isHittable, "the list scrolled away from line 1")

        app.tabBars.buttons["Lines"].tap()

        // The scroll animates: wait until the heading above line 1 can be tapped again.
        let dayLines = app.staticTexts["Day lines"]
        let hittable = XCTNSPredicateExpectation(predicate: NSPredicate(format: "isHittable == true"), object: dayLines)
        XCTAssertEqual(XCTWaiter().wait(for: [hittable], timeout: 5), .completed, "back at the top, with its heading")
        XCTAssertTrue(firstLine.isHittable, "re-selecting Lines scrolled back to line 1")
    }

    @MainActor
    func test_lb101_linesThatShareAColourAreSpokenByTheirCode_thenTheirName() {
        // DD-08 and NFR-A11Y-02: lines 4 and B1 share red; VoiceOver reads each row as its line, never a colour.
        let app = launch()

        XCTAssertTrue(line("4", in: app).waitForExistence(timeout: 10))
        XCTAssertEqual(line("4", in: app).label, "Line 4, Casa Blanca - Yagüe")
        for _ in 0..<3 where !line("B1", in: app).exists { app.swipeUp() }
        XCTAssertTrue(line("B1", in: app).waitForExistence(timeout: 5))
        XCTAssertEqual(line("B1", in: app).label, "Line B1, Varea - La Cava")
    }

    @MainActor
    func test_lb103_aLineAddedFromItsRowGoesInTheCarousel_itsToggleSaysSo_andItsCardOpensIt() {
        // FR-LINE-12 to 15, through the debug build's graph and the device's database. The simulator keeps the app's
        // data between launches, so the test starts and ends with line 3 out of the favourites.
        let app = launch()
        XCTAssertTrue(line("3", in: app).waitForExistence(timeout: 10))
        removeFavourite("3", in: app)

        app.buttons["Add line 3 to favourites"].tap()

        XCTAssertTrue(app.buttons["Remove line 3 from favourites"].waitForExistence(timeout: 5))
        let card = favouriteCard("3", in: app)
        XCTAssertTrue(card.waitForExistence(timeout: 5))
        card.tap()
        XCTAssertTrue(app.navigationBars["Line 3"].waitForExistence(timeout: 5), "the card opened line 3")
        back(in: app)
        removeFavourite("3", in: app)
        XCTAssertTrue(app.staticTexts["Tap ☆ on a line to pin it here"].waitForExistence(timeout: 5))
    }

    @MainActor
    func test_lb103_aLineAddedFromItsDetailIsInTheCarousel_backOnLines() {
        // FR-LINE-09: the row opens the line's detail, whose toolbar has the toggle.
        let app = launch()
        XCTAssertTrue(line("3", in: app).waitForExistence(timeout: 10))
        removeFavourite("3", in: app)

        line("3", in: app).tap()
        let detail = app.navigationBars["Line 3"]
        XCTAssertTrue(detail.buttons["Add line 3 to favourites"].waitForExistence(timeout: 5))
        detail.buttons["Add line 3 to favourites"].tap()
        XCTAssertTrue(detail.buttons["Remove line 3 from favourites"].waitForExistence(timeout: 5))
        back(in: app)

        XCTAssertTrue(favouriteCard("3", in: app).waitForExistence(timeout: 5), "line 3 is in the carousel")
        removeFavourite("3", in: app)
    }

    /// Takes line `code` out of the favourites, if it is one, from its row's toggle.
    @MainActor
    private func removeFavourite(_ code: String, in app: XCUIApplication) {
        let remove = app.buttons["Remove line \(code) from favourites"]
        if remove.exists { remove.tap() }
        XCTAssertTrue(app.buttons["Add line \(code) to favourites"].waitForExistence(timeout: 5))
    }

    /// The carousel's card of line `code`: like its row, it starts with the badge, and it alone says where it heads.
    @MainActor
    private func favouriteCard(_ code: String, in app: XCUIApplication) -> XCUIElement {
        let card = NSPredicate(format: "label BEGINSWITH %@ AND label CONTAINS %@", "Line \(code), ", "Towards")
        return app.buttons.matching(card).firstMatch
    }

    /// The navigation bar's back button, to the previous screen of the tab.
    @MainActor
    private func back(in app: XCUIApplication) {
        app.navigationBars.buttons.element(boundBy: 0).tap()
    }

    /// The row of the line with `code`, read as one element that starts with its badge's spoken label.
    @MainActor
    private func line(_ code: String, in app: XCUIApplication) -> XCUIElement {
        let row = NSPredicate(format: "label BEGINSWITH %@", "Line \(code), ")
        return app.descendants(matching: .any).matching(row).firstMatch
    }

    @MainActor
    private func assertTabs(_ labels: [String], in app: XCUIApplication) {
        let tabs = app.tabBars.buttons
        XCTAssertTrue(tabs[labels[0]].waitForExistence(timeout: 10))
        XCTAssertEqual((0..<tabs.count).map { tabs.element(boundBy: $0).label }, labels)
        XCTAssertTrue(tabs[labels[0]].isSelected)
    }

    @MainActor
    private func launch(
        language: String = "en",
        region: String = "en_GB",
        textSize: String? = nil
    ) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(\(language))", "-AppleLocale", region]
        if let textSize { app.launchArguments += ["-UIPreferredContentSizeCategoryName", textSize] }
        app.launch()
        return app
    }
}
