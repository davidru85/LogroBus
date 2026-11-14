import XCTest

/// LB-209 (FR-NEAR-05, FR-STOP-01, FR-STOP-02, DD-16): Near Me's stop search, end to end over the scripted catalogue.
final class StopSearchUITests: XCTestCase {

    @MainActor
    func test_lb209_aNameFindsItsStops_andTheEmptyAnswerSaysSo() {
        let app = launch()
        app.tabBars.buttons["Near me"].tap()

        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10), "Near Me offers a search field")
        field.tap()
        field.typeText("Campillo")

        // The stop the query names, with the line that reaches it (DD-16).
        let row = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Campillo")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "a name finds its stop")

        // A query that finds nothing says so rather than showing an empty list.
        field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 8))
        field.typeText("tranvia")
        XCTAssertTrue(
            app.staticTexts["No stops match that search"].waitForExistence(timeout: 10),
            "an empty answer says so"
        )
    }

    @MainActor
    func test_lb209_aStopNumberFindsThatStop() {
        let app = launch()
        app.tabBars.buttons["Near me"].tap()

        let field = app.searchFields.firstMatch
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        // Line 3's outbound first stop is 300 in the samples; its name is the line's own first terminus.
        field.typeText("300")

        let row = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Campillo")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "a number finds that stop")
    }

    /// The app with the scripted graph, in English, so the labels the assertions read are the English ones.
    @MainActor
    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(en)", "-AppleLocale", "en_GB", "-logrobus.preview-data"]
        app.launch()
        return app
    }
}
