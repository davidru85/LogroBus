import XCTest

/// LB-208 (FR-STOP-08, FR-HOME-02, FR-FAV-02): a stop's detail with its Save star, "My stops" with that stop saved, and
/// the edit sheet's rows, which carry the place, the remove and rename buttons and the drag handle.
///
/// It is two small steps, each taken where the walk already is, because the audit's job is close to its time limit: the
/// star from line 3's detail, which the walk opens anyway, and the section and its sheet from Near Me, where the walk
/// ends. Its own file, as `AccessibilityAuditUITests` is at SwiftLint's 400-line limit.
enum MyStopsAudit {
    /// From line 3's detail: its first stop, saved through the star, and the star read in its saved state.
    ///
    /// Returns after the stop's detail is left, so the walk continues where it was.
    @MainActor
    static func auditStar(
        _ audit: Audit,
        _ types: XCUIAccessibilityAuditType,
        in app: XCUIApplication,
        into issues: inout [String]
    ) throws {
        let stop = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "Stop 1, Campillo")
        ).firstMatch
        XCTAssertTrue(stop.waitForExistence(timeout: 10), "line 3 lists its first stop")
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

        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["Line 3"].waitForExistence(timeout: 5), "back on the line")
    }

    /// From Near Me, where the walk ends: "My stops" with the stop saved above, and the edit sheet's rows.
    ///
    /// It leaves the stop unsaved, so the next run starts from the same state.
    @MainActor
    static func auditSection(
        _ audit: Audit,
        _ types: XCUIAccessibilityAuditType,
        in app: XCUIApplication,
        into issues: inout [String]
    ) throws {
        XCTAssertTrue(app.staticTexts["My stops"].waitForExistence(timeout: 10), "Near Me lists My stops")
        try audit.run("Near me with My stops", types, into: &issues)

        let edit = app.buttons["Edit"]
        XCTAssertTrue(edit.waitForExistence(timeout: 5), "the section offers Edit while a stop is saved")
        edit.tap()
        let sheet = app.navigationBars["Edit my stops"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 5))
        try audit.run("Edit my stops", types, into: &issues)

        app.buttons.matching(NSPredicate(format: "label == %@", "Remove")).firstMatch.tap()
        sheet.buttons["Done"].tap()
        XCTAssertTrue(sheet.waitForNonExistence(timeout: 5))
    }
}
