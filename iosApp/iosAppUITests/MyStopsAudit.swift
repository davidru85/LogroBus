import XCTest

/// LB-208 (FR-STOP-08, FR-HOME-02, FR-FAV-02): a stop's detail with its Save star, "My stops" with that stop saved, and
/// the edit sheet's rows, which carry the place, the remove and rename buttons and the drag handle. Its own file,
/// because `AccessibilityAuditUITests` is at SwiftLint's 400-line limit that the rest of its walk keeps.
enum MyStopsAudit {
    @MainActor
    static func audit(
        _ audit: Audit,
        _ types: XCUIAccessibilityAuditType,
        in app: XCUIApplication,
        into issues: inout [String]
    ) throws {
        // The saved stop, through the star on the stop's own detail. The screen itself is LB-205's and is audited
        // elsewhere; only the story's states are read here.
        //
        // The walk runs late, after every other screen, so on a busy runner the tab may not have drawn its lines yet:
        // the tab is tapped, and the row waited for, before anything is read from it.
        app.tabBars.buttons["Lines"].tap()
        let line = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@ AND NOT label CONTAINS %@", "Line 3, ", "Towards")
        ).firstMatch
        if !line.waitForExistence(timeout: 10) {
            app.tabBars.buttons["Lines"].tap()
        }
        // At an accessibility text size the rows are taller, so line 3 may start below the fold: scroll it into view,
        // as the rest of the audit does for its own rows, rather than reading a list it has not reached.
        for _ in 0..<6 where !line.exists {
            app.swipeUp()
        }
        XCTAssertTrue(line.waitForExistence(timeout: 15), "the Lines tab shows the scripted lines")
        line.tap()
        let stop = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "Stop 1, Campillo")
        ).firstMatch
        XCTAssertTrue(stop.waitForExistence(timeout: 10))
        stop.tap()
        XCTAssertTrue(app.navigationBars["Campillo"].waitForExistence(timeout: 5))

        // A previous run may have left the stop saved, so the star is whichever of its two labels shows. Saving asks
        // for the alias first, as Android's Save action does, so the alert is answered before the star is read again.
        let add = app.buttons["Save stop"]
        let remove = app.buttons["Remove this stop from saved stops"]
        if !remove.exists {
            XCTAssertTrue(add.waitForExistence(timeout: 10), "the detail offers the star")
            add.tap()
            let alias = app.textFields.firstMatch
            XCTAssertTrue(alias.waitForExistence(timeout: 5), "the save alert asks for an alias")
            alias.typeText("Casa")
            app.buttons["Save"].tap()
            XCTAssertTrue(remove.waitForExistence(timeout: 5))
        }

        app.tabBars.buttons["Near me"].tap()
        XCTAssertTrue(app.staticTexts["My stops"].waitForExistence(timeout: 5))
        try audit.run("Near me with My stops", types, into: &issues)

        let edit = app.buttons["Edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 5))
        edit.tap()
        let sheet = app.navigationBars["Edit my stops"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 5))
        try audit.run("Edit my stops", types, into: &issues)

        // The audit leaves the app as it found it, so the stop is not saved twice over by the next run.
        app.buttons.matching(NSPredicate(format: "label == %@", "Remove")).firstMatch.tap()
        sheet.buttons["Done"].tap()
        XCTAssertTrue(sheet.waitForNonExistence(timeout: 5))
    }
}
