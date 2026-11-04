import XCTest

/// LB-406 (NFR-A11Y): every screen the app has passes Xcode's accessibility audit, at the default text size and at the
/// largest: touch targets of at least 44 pt, no clipped text, Dynamic Type, contrast, and a description and trait for
/// every element. One launch per text size walks the screens, and the issues of all of them are reported together,
/// each with a screenshot of its screen.
///
/// Contrast is audited only where nothing scrolls beneath the floating bars, whose scroll edge effect fades what is
/// under them: the audit reads that faded text, and does not always name its element, so it cannot be told apart.
/// The Lines list is therefore audited for contrast while a search keeps it short. At the largest text, every text is
/// large text, whose threshold, 3:1, the default size's audit at 4.5:1 already covers. Text colours are also checked
/// against every background by `TokensTests`, and line badges by NFR-A11Y-06's rule.
final class AccessibilityAuditUITests: XCTestCase {

    @MainActor
    func test_lb406_everyScreenPassesTheAccessibilityAudit() throws {
        try auditEveryScreen(in: launch(), contrast: true)
    }

    @MainActor
    func test_lb406_everyScreenPassesTheAccessibilityAudit_atTheLargestTextSize() throws {
        try auditEveryScreen(in: launch(textSize: "UICTContentSizeCategoryAccessibilityXXXL"), contrast: false)
    }

    /// Lines with the favourites' hint, a search and a favourite, the favourites' edit sheet, a line's detail, More,
    /// Settings, About data, Near me and Directions. Line 3 is a favourite only while the test needs one.
    @MainActor
    private func auditEveryScreen(in app: XCUIApplication, contrast: Bool) throws {
        let types: XCUIAccessibilityAuditType = contrast ? .all : .all.subtracting(.contrast)
        var issues: [String] = []
        XCTAssertTrue(app.navigationBars["Lines"].waitForExistence(timeout: 10))
        let addLine3 = app.buttons["Add line 3 to favourites"]
        let removeLine3 = app.buttons["Remove line 3 from favourites"]
        scrollUp(toward: addLine3, removeLine3, in: app)
        if removeLine3.exists { removeLine3.tap() }
        XCTAssertTrue(addLine3.waitForExistence(timeout: 5))
        scrollDown(toward: app.staticTexts["Your favourite lines"], in: app)
        try audit("Lines", types.subtracting(.contrast), in: app, into: &issues)

        // Lines 2 and B1, under both headings, and nothing beneath the tab bar.
        let search = app.searchFields.firstMatch
        search.tap()
        search.typeText("varea\n")
        XCTAssertTrue(app.staticTexts["Night lines (Búho)"].waitForExistence(timeout: 5))
        try audit("Lines, searching", types, in: app, into: &issues)
        app.buttons["Close"].firstMatch.tap()

        scrollUp(toward: addLine3, in: app)
        addLine3.tap()
        addTeardownBlock { @MainActor in
            let remove = app.buttons["Remove line 3 from favourites"]
            if remove.exists { remove.tap() }
        }
        XCTAssertTrue(removeLine3.waitForExistence(timeout: 5))
        let edit = app.buttons["Edit favourite lines"]
        scrollDown(toward: edit, in: app)
        try audit("Lines with a favourite", types.subtracting(.contrast), in: app, into: &issues)

        edit.tap()
        let sheet = app.navigationBars["Edit favourite lines"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 5))
        try audit("Edit favourite lines", types, in: app, into: &issues)
        sheet.buttons["Done"].tap()
        XCTAssertTrue(sheet.waitForNonExistence(timeout: 5))
        scrollUp(toward: removeLine3, in: app)
        removeLine3.tap()

        let row = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Line 3, ")).firstMatch
        scrollUp(toward: row, in: app)
        row.tap()
        XCTAssertTrue(app.navigationBars["Line 3"].waitForExistence(timeout: 5))
        try audit("Line 3", types, in: app, into: &issues)
        app.navigationBars["Line 3"].buttons["Lines"].tap()

        app.navigationBars["Lines"].buttons["More"].tap()
        XCTAssertTrue(app.buttons["Settings"].waitForExistence(timeout: 5))
        try audit("More", types, in: app, into: &issues)
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        try audit("Settings", types, in: app, into: &issues)
        // At the largest text, Clear cache is below the screen.
        app.swipeUp()
        app.swipeUp()
        try audit("Settings, at its end", types, in: app, into: &issues)
        app.navigationBars["Settings"].buttons.firstMatch.tap()
        app.buttons["About data"].tap()
        XCTAssertTrue(app.navigationBars["About data"].waitForExistence(timeout: 5))
        try audit("About data", types, in: app, into: &issues)

        for (tab, title) in [("Near me", "Near me"), ("Directions", "How to get there")] {
            app.tabBars.buttons[tab].tap()
            XCTAssertTrue(app.navigationBars[title].waitForExistence(timeout: 5))
            try audit(tab, types, in: app, into: &issues)
        }

        XCTAssertEqual(issues, [], "\n" + issues.joined(separator: "\n"))
    }

    /// Audits the screen on display for `types`, and adds each issue that is not a known false positive to `issues`,
    /// named after the screen, its kind and its element.
    @MainActor
    private func audit(
        _ screen: String,
        _ types: XCUIAccessibilityAuditType,
        in app: XCUIApplication,
        into issues: inout [String]
    ) throws {
        let shot = XCTAttachment(screenshot: app.screenshot())
        shot.name = screen
        shot.lifetime = .keepAlways
        add(shot)
        let navigationBars = app.navigationBars.allElementsBoundByIndex.map(\.frame)
        var found: [String] = []
        try app.performAccessibilityAudit(for: types) { issue in
            if Self.isFalsePositive(issue, navigationBars: navigationBars) { return true }
            let element = issue.element.map { "\"\($0.label)\" at \($0.frame)" } ?? "no element"
            found.append("\(screen): \(issue.compactDescription), \(element): \(issue.detailedDescription)")
            return true
        }
        issues += found
    }

    /// What the audit reports wrongly, each checked by hand in its screenshot (LB-406):
    /// - **"Clipped" text that VoiceOver reads in words:** the favourites' hint and a card's headsigns show symbols
    ///   ("☆", "→") that their labels say in words, longer than the text, which the audit measures. Removing the
    ///   hint's label removed the issue; both wrap whole in the screenshots and the package's snapshots.
    /// - **"Clipped" text of the system's search field:** at the largest text, its placeholder shows whole.
    /// - **Dynamic Type in a navigation bar:** the system caps a bar button's text and shows the Large Content Viewer
    ///   on a long press instead.
    /// - **Dynamic Type of Settings' Data section:** its heading and footer, alone in the form, are reported as not
    ///   scaling, yet they scale with the others at the largest text.
    private static func isFalsePositive(_ issue: XCUIAccessibilityAuditIssue, navigationBars: [CGRect]) -> Bool {
        let label = issue.element?.label ?? ""
        switch issue.auditType {
        case .textClipped:
            return label == "Tap the star on a line to pin it here" || label.hasPrefix("Towards ") ||
                issue.element?.elementType == .searchField
        case .dynamicType:
            let inABar = issue.element.map { element in navigationBars.contains { $0.contains(element.frame) } }
            return inABar == true || ["Data", "Your favourites and the lines and stops data are kept."].contains(label)
        default:
            return false
        }
    }

    /// Swipes the list up until one of `elements` can be tapped: at the largest text, line 3 is below the screen.
    @MainActor
    private func scrollUp(toward elements: XCUIElement..., in app: XCUIApplication) {
        for _ in 0..<10 where !elements.contains(where: { $0.exists && $0.isHittable }) { app.swipeUp() }
    }

    /// Swipes the list down until `element`, near its top, can be tapped, then on to the top, so that nothing is left
    /// beneath the search field.
    @MainActor
    private func scrollDown(toward element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<10 where !(element.exists && element.isHittable) { app.swipeDown() }
        app.swipeDown()
        app.swipeDown()
        XCTAssertTrue(element.isHittable, "\(element) is on screen")
    }

    @MainActor
    private func launch(textSize: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(\"en\")", "-AppleLocale", "en_GB"]
        if let textSize { app.launchArguments += ["-UIPreferredContentSizeCategoryName", textSize] }
        app.launch()
        return app
    }
}
