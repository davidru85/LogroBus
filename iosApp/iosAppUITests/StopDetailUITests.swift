import XCTest

/// LB-205 part 2: the pushed stop detail (FR-NAV-07), end to end through the app shell. A stop's row on line detail
/// opens the stop's own screen, and back returns to the line. Its own file because `ShellUITests` is at SwiftLint's
/// 400-line limit, which the shell's own tests keep.
final class StopDetailUITests: XCTestCase {

    @MainActor
    func test_lb205_aStopRowOpensItsStopDetail_andBackReturnsToTheLine() {
        // Line 3's stops are its name's two ends, so its first outbound stop is "Campillo".
        let app = launch()
        let line = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@ AND NOT label CONTAINS %@", "Line 3, ", "Towards")
        ).firstMatch
        XCTAssertTrue(line.waitForExistence(timeout: 10))
        line.tap()
        XCTAssertTrue(app.navigationBars["Line 3"].waitForExistence(timeout: 5))

        let stop = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "Stop 1, Campillo")
        ).firstMatch
        XCTAssertTrue(stop.waitForExistence(timeout: 10), "the stop's row is on the line's detail")
        stop.tap()

        XCTAssertTrue(app.navigationBars["Campillo"].waitForExistence(timeout: 5), "the stop's detail was pushed")
        XCTAssertFalse(app.navigationBars["Line 3"].exists, "the line's detail is under it, not shown")

        app.navigationBars.buttons.element(boundBy: 0).tap()

        XCTAssertTrue(app.navigationBars["Line 3"].waitForExistence(timeout: 5), "back returns to the line")
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
