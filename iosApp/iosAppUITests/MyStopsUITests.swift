import XCTest

/// LB-208 part 4 (FR-STOP-08, FR-HOME-02, FR-FAV-02): the Save star on a stop's detail, and "My stops" in Near Me with
/// the sheet that reorders, renames and removes. End to end through the app shell, over the scripted graph.
///
/// The saved stops outlive a launch, as the favourites do, so each test clears them through the sheet first and reads a
/// state it made rather than whatever the last run left.
final class MyStopsUITests: XCTestCase {
    /// FR-FAV-05: the most saved stops there can be, which bounds the clearing loop.
    private let mostSavedStops = 20

    @MainActor
    func test_lb208_theStarSavesTheStop_andNearMeListsItUnderItsName() {
        let app = launch()
        clearMyStops(app)

        openTheStop(app)
        let star = app.buttons["Save stop"]
        XCTAssertTrue(
            star.waitForExistence(timeout: 10),
            "the stop's detail offers Save once the saved stops are known"
        )
        star.tap()

        XCTAssertTrue(
            app.buttons["Saved stop"].waitForExistence(timeout: 5),
            "the star fills, and its label says the stop is saved"
        )

        app.tabBars.buttons["Near me"].tap()
        XCTAssertTrue(app.staticTexts["My stops"].waitForExistence(timeout: 5))
        let listed = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Campillo")).firstMatch
        XCTAssertTrue(
            listed.waitForExistence(timeout: 5),
            "the saved stop is listed under its own name until it has an alias"
        )
    }

    @MainActor
    func test_lb208_theEditSheetRemovesASavedStop_andNearMeSaysHowToFillIt() {
        let app = launch()
        clearMyStops(app)

        openTheStop(app)
        let star = app.buttons["Save stop"]
        XCTAssertTrue(star.waitForExistence(timeout: 10))
        star.tap()
        XCTAssertTrue(app.buttons["Saved stop"].waitForExistence(timeout: 5))

        app.tabBars.buttons["Near me"].tap()
        let edit = app.buttons["Edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 5), "Edit is offered while something is saved")
        edit.tap()

        XCTAssertTrue(app.navigationBars["Edit my stops"].waitForExistence(timeout: 5))
        // The row is one accessibility element: its label is the stop and its value its place, as Android's merges the
        // same two.
        let row = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "Campillo")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        // FR-FAV-02: the row's place, which VoiceOver reads.
        XCTAssertEqual(row.value as? String, "1 of 1")

        // The row's own Remove button, as Android's has.
        let remove = app.buttons["Remove"]
        XCTAssertTrue(remove.waitForExistence(timeout: 5), "the row offers Remove")
        remove.tap()

        app.buttons["Done"].tap()

        let hint = app.staticTexts["Save a stop to see its next buses here"]
        XCTAssertTrue(hint.waitForExistence(timeout: 5), "with nothing saved, the section says how to fill it")
        XCTAssertFalse(app.buttons["Edit"].exists, "and offers no Edit")
    }

    /// Removes every saved stop through the sheet, so a test starts from an empty "My stops" whatever a run left there.
    @MainActor
    private func clearMyStops(_ app: XCUIApplication) {
        app.tabBars.buttons["Near me"].tap()
        let edit = app.buttons["Edit"]
        // Nothing saved: the section offers no Edit, so there is nothing to clear, and the test goes on to Lines.
        guard edit.waitForExistence(timeout: 5) else {
            goToLines(app)
            return
        }
        edit.tap()
        XCTAssertTrue(app.navigationBars["Edit my stops"].waitForExistence(timeout: 5))
        // At most FAVOURITES_PER_TYPE rows. Bounded, so a swipe that removes nothing fails rather than looping.
        for _ in 0..<mostSavedStops {
            let removes = app.buttons.matching(identifier: "Remove")
            guard !removes.allElementsBoundByIndex.isEmpty else { break }
            removes.element(boundBy: 0).tap()
            _ = removalsSettle(app)
        }
        XCTAssertEqual(app.buttons.matching(identifier: "Remove").count, 0, "every saved stop was removed")
        app.buttons["Done"].tap()
        // The sheet animates out; tapping a tab while it is still there lands on the sheet, not the tab bar.
        XCTAssertTrue(app.buttons["Done"].waitForNonExistence(timeout: 5), "the sheet closes")
        goToLines(app)
    }

    /// One frame, so the sheet has redrawn before the next round counts its rows.
    @MainActor
    private func removalsSettle(_ app: XCUIApplication) -> Bool {
        app.buttons["Done"].waitForExistence(timeout: 1)
    }

    /// The Lines tab, its first row waited for, so the screen the test came from is the one it acts on.
    @MainActor
    private func goToLines(_ app: XCUIApplication) {
        let tab = app.tabBars.buttons["Lines"]
        XCTAssertTrue(tab.waitForExistence(timeout: 10))
        tab.tap()
        XCTAssertTrue(
            app.descendants(matching: .any).matching(
                NSPredicate(format: "label BEGINSWITH %@ AND NOT label CONTAINS %@", "Line 3, ", "Towards")
            ).firstMatch.waitForExistence(timeout: 10),
            "the Lines tab shows the scripted lines"
        )
    }

    /// Line 3's first outbound stop, whose detail the shell pushes.
    @MainActor
    private func openTheStop(_ app: XCUIApplication) {
        let line = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@ AND NOT label CONTAINS %@", "Line 3, ", "Towards")
        ).firstMatch
        XCTAssertTrue(line.waitForExistence(timeout: 10))
        line.tap()
        let stop = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "Stop 1, Campillo")
        ).firstMatch
        XCTAssertTrue(stop.waitForExistence(timeout: 10))
        stop.tap()
        XCTAssertTrue(app.navigationBars["Campillo"].waitForExistence(timeout: 5))
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
