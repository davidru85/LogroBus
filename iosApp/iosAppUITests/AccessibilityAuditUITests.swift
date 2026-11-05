import UIKit
import XCTest

/// LB-406 (NFR-A11Y): every screen the app has passes Xcode's accessibility audit, at the default text size and at the
/// largest: touch targets of at least 44 pt, no clipped text, Dynamic Type, contrast, and a description and trait for
/// every element. One launch per text size walks the screens, and the issues of all of them are reported together,
/// each with a screenshot of its screen. The default size's launch then turns the device to dark mode, relaunches the
/// app, and walks the screens again.
///
/// Contrast is audited only where nothing scrolls beneath the floating bars, whose scroll edge effect fades what is
/// under them: the audit reads that faded text, and does not always name its element, so it cannot be told apart.
/// The Lines list is therefore audited for contrast while a search keeps it short. At the largest text, every text is
/// large text, whose threshold, 3:1, the default size's audit at 4.5:1 already covers. Text colours are also checked
/// against every background by `TokensTests`, and line badges by NFR-A11Y-06's rule.
final class AccessibilityAuditUITests: XCTestCase {

    @MainActor
    func test_lb406_everyScreenPassesTheAccessibilityAudit() throws {
        let app = launch()
        var issues: [String] = []
        try auditEveryScreen(in: app, largestText: false, into: &issues)
        try auditInDarkMode(in: app, into: &issues)
        XCTAssertEqual(issues, [], "\n" + issues.joined(separator: "\n"))
    }

    @MainActor
    func test_lb406_everyScreenPassesTheAccessibilityAudit_atTheLargestTextSize() throws {
        let app = launch(textSize: "UICTContentSizeCategoryAccessibilityXXXL")
        var issues: [String] = []
        try auditEveryScreen(in: app, largestText: true, into: &issues)
        XCTAssertEqual(issues, [], "\n" + issues.joined(separator: "\n"))
    }

    /// Lines with the favourites' hint, a search and a favourite, the favourites' edit sheet, a favourite's detail,
    /// More, Settings and its Clear cache alert, About data, Near me with its row and Directions. Line 3 is a favourite
    /// only while the test needs one; one that an interrupted run left is removed first, as `ShellUITests` does.
    @MainActor
    private func auditEveryScreen(in app: XCUIApplication, largestText: Bool, into issues: inout [String]) throws {
        let audit = Audit(test: self, app: app, largestText: largestText)
        let types: XCUIAccessibilityAuditType = largestText ? .all.subtracting(.contrast) : .all
        XCTAssertTrue(app.navigationBars["Lines"].waitForExistence(timeout: 10))
        let addLine3 = app.buttons["Add line 3 to favourites"]
        let removeLine3 = app.buttons["Remove line 3 from favourites"]
        scrollUp(toward: addLine3, removeLine3, in: app)
        if removeLine3.exists { removeLine3.tap() }
        XCTAssertTrue(addLine3.waitForExistence(timeout: 5))
        scrollDown(toward: app.staticTexts["Your favourite lines"], in: app)
        try audit.run("Lines", types.subtracting(.contrast), into: &issues)

        try search("varea", in: app)
        try audit.run("Lines, searching", types, into: &issues)
        closeSearch(in: app)

        scrollUp(toward: addLine3, in: app)
        addLine3.tap()
        XCTAssertTrue(removeLine3.waitForExistence(timeout: 5))
        let edit = app.buttons["Edit favourite lines"]
        scrollDown(toward: edit, in: app)
        try audit.run("Lines with a favourite", types.subtracting(.contrast), into: &issues)

        edit.tap()
        let sheet = app.navigationBars["Edit favourite lines"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 5))
        try audit.run("Edit favourite lines", types, into: &issues)
        sheet.buttons["Done"].tap()
        XCTAssertTrue(sheet.waitForNonExistence(timeout: 5))

        // The carousel's card, at the top: it alone says where the line heads.
        let card = NSPredicate(format: "label BEGINSWITH %@ AND label CONTAINS %@", "Line 3, ", "Towards")
        app.buttons.matching(card).firstMatch.tap()
        let detail = app.navigationBars["Line 3"]
        XCTAssertTrue(detail.waitForExistence(timeout: 5))
        try audit.run("Line 3", types, into: &issues)
        detail.buttons["Remove line 3 from favourites"].tap()
        XCTAssertTrue(detail.buttons["Add line 3 to favourites"].waitForExistence(timeout: 5))
        detail.buttons["Lines"].tap()

        try auditMoreAndSettings(audit, types, largestText: largestText, in: app, into: &issues)
        app.tabBars.buttons["Near me"].tap()
        XCTAssertTrue(app.buttons[Self.turnOnLocation].waitForExistence(timeout: 5))
        try audit.run("Near me", types, into: &issues)
        app.tabBars.buttons["Directions"].tap()
        XCTAssertTrue(app.navigationBars["How to get there"].waitForExistence(timeout: 5))
        try audit.run("Directions", types, into: &issues)
    }

    /// Near Me's row without location (LB-201), which the audit waits for: the screen is blank until access is read.
    private static let turnOnLocation = "Turn on location to see nearby stops"

    /// The Lines search, More, Settings, its alert, About data and Near me again, with the device in dark mode: the
    /// dark accent and secondary text on the system's dark backgrounds. The app's theme follows the device.
    ///
    /// The app relaunches once the device is dark, and the walk waits for its screen to be dark: on CI the app that
    /// was running stayed light (PR #50). The app's own Dark theme would not need the device, but the audit then
    /// misreads the contrast of the system's search field and alert, which show white on dark.
    @MainActor
    private func auditInDarkMode(in app: XCUIApplication, into issues: inout [String]) throws {
        XCUIDevice.shared.appearance = .dark
        addTeardownBlock { @MainActor in XCUIDevice.shared.appearance = .light }
        app.launch()
        let audit = Audit(test: self, app: app, largestText: false, dark: true)
        XCTAssertTrue(app.navigationBars["Lines"].waitForExistence(timeout: 10))
        let deadline = Date().addingTimeInterval(10)
        while !Audit.isMostlyDark(app.screenshot().image), Date() < deadline {
            Thread.sleep(forTimeInterval: 0.5)
        }
        try search("varea", in: app)
        try audit.run("Lines, searching, dark", .all, into: &issues)
        closeSearch(in: app)
        try auditMoreAndSettings(audit, .all, largestText: false, in: app, into: &issues, suffix: ", dark")
        app.tabBars.buttons["Near me"].tap()
        XCTAssertTrue(app.buttons[Self.turnOnLocation].waitForExistence(timeout: 5))
        try audit.run("Near me, dark", .all, into: &issues)
    }

    /// More, Settings, at its end at the largest text, its Clear cache alert, and About data, from the Lines root.
    @MainActor
    private func auditMoreAndSettings(
        _ audit: Audit,
        _ types: XCUIAccessibilityAuditType,
        largestText: Bool,
        in app: XCUIApplication,
        into issues: inout [String],
        suffix: String = ""
    ) throws {
        app.navigationBars["Lines"].buttons["More"].tap()
        XCTAssertTrue(app.buttons["Settings"].waitForExistence(timeout: 5))
        try audit.run("More" + suffix, types, into: &issues)
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        try audit.run("Settings" + suffix, types, into: &issues)
        let clearCache = app.buttons["Clear cache"]
        if largestText {
            // Clear cache is below the screen.
            scrollUp(toward: clearCache, in: app)
            try audit.run("Settings, at its end" + suffix, types, into: &issues)
        }
        clearCache.tap()
        let alert = app.alerts["Cache cleared"]
        XCTAssertTrue(alert.waitForExistence(timeout: 5))
        try audit.run("Settings, Clear cache's alert" + suffix, types, into: &issues)
        alert.buttons.firstMatch.tap()
        XCTAssertTrue(alert.waitForNonExistence(timeout: 5))
        app.navigationBars["Settings"].buttons["More"].tap()
        app.buttons["About data"].tap()
        XCTAssertTrue(app.navigationBars["About data"].waitForExistence(timeout: 5))
        try audit.run("About data" + suffix, types, into: &issues)
        // Re-selecting the tab pops its stack to its root (FR-NAV-03).
        app.tabBars.buttons["Lines"].tap()
        XCTAssertTrue(app.navigationBars["Lines"].waitForExistence(timeout: 5))
    }

    /// Searches the Lines list: "varea" leaves lines 2 and B1, under both headings, and nothing beneath the tab bar.
    @MainActor
    private func search(_ query: String, in app: XCUIApplication) throws {
        let field = app.searchFields.firstMatch
        field.tap()
        field.typeText(query + "\n")
        XCTAssertTrue(app.staticTexts["Night lines (Búho)"].waitForExistence(timeout: 5))
    }

    /// Ends the search with its close button: "Close" on iOS 26, "Cancel" before.
    @MainActor
    private func closeSearch(in app: XCUIApplication) {
        let close = app.buttons["Close"].firstMatch
        (close.exists ? close : app.buttons["Cancel"].firstMatch).tap()
    }

    /// Swipes the list up until one of `elements` can be tapped: at the largest text, line 3 is below the screen.
    @MainActor
    private func scrollUp(toward elements: XCUIElement..., in app: XCUIApplication) {
        var swipes = 0
        while swipes < 20, !elements.contains(where: { $0.exists && $0.isHittable }) {
            app.swipeUp(velocity: .slow)
            swipes += 1
        }
    }

    /// Swipes the list down until `element`, near its top, can be tapped, then on to the top, so that nothing is left
    /// beneath the search field.
    @MainActor
    private func scrollDown(toward element: XCUIElement, in app: XCUIApplication) {
        var swipes = 0
        while swipes < 20, !(element.exists && element.isHittable) {
            app.swipeDown(velocity: .slow)
            swipes += 1
        }
        app.swipeDown()
        app.swipeDown()
        XCTAssertTrue(element.isHittable, "\(element) is on screen")
    }

    @MainActor
    private func launch(textSize: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        // Location never asked, as on a fresh install, so Near Me shows its "Turn on location" row (LB-201).
        app.resetAuthorizationStatus(for: .location)
        app.launchArguments += ["-AppleLanguages", "(\"en\")", "-AppleLocale", "en_GB"]
        if let textSize { app.launchArguments += ["-UIPreferredContentSizeCategoryName", textSize] }
        app.launch()
        return app
    }
}

/// One launch's audits: each runs Xcode's audit on the screen on display and reports what is not a known false
/// positive.
@MainActor
private struct Audit {
    let test: XCTestCase
    let app: XCUIApplication
    let largestText: Bool
    /// Whether the screens are meant to be in dark mode, which each audit then checks in its screenshot.
    var dark = false

    /// The accessibility identifiers of texts whose labels say their symbols in words.
    private static let spokenInWords = ["lines.favourites.hint", "lines.favourites.card.headsigns"]

    /// Audits the screen on display for `types`, and adds each issue that is not a known false positive to `issues`,
    /// named after the screen, its kind and its element. The audit sometimes reads a screen that is still settling,
    /// and then cannot name an issue's element: it runs again, up to three times, while an issue has none.
    ///
    /// A screen meant to be dark that is not is an issue too: on CI the simulator kept light mode, and the dark audits
    /// audited light mode again without saying so (PR #50).
    func run(_ screen: String, _ types: XCUIAccessibilityAuditType, into issues: inout [String]) throws {
        let screenshot = app.screenshot()
        let shot = XCTAttachment(screenshot: screenshot)
        shot.name = screen
        shot.lifetime = .keepAlways
        test.add(shot)
        if dark && !Self.isMostlyDark(screenshot.image) {
            let device = XCUIDevice.shared.appearance == .dark ? "dark" : "not dark"
            issues.append("\(screen): the screen is not in dark mode, and the device's appearance is \(device)")
        }
        var found: [String] = []
        for attempt in 1...3 {
            found = []
            var unnamed = false
            let barButtons = app.navigationBars.buttons.allElementsBoundByIndex.map(\.frame)
            try app.performAccessibilityAudit(for: types) { issue in
                if isFalsePositive(issue, on: screen, barButtons: barButtons) { return true }
                unnamed = unnamed || issue.element == nil
                let element = issue.element.map { "\"\($0.label)\" at \($0.frame)" } ?? "no element"
                found.append("\(screen): \(issue.compactDescription), \(element): \(issue.detailedDescription)")
                return true
            }
            if !unnamed || attempt == 3 { break }
            Thread.sleep(forTimeInterval: 1)
        }
        issues += found
    }

    /// What the audit reports wrongly, each checked by hand in its screenshot (LB-406), each only where it was seen:
    /// - **"Clipped" text that VoiceOver reads in words,** on Lines: the favourites' hint and a card's headsigns show
    ///   symbols ("☆", "→") that their labels say in words, longer than the text, which the audit measures. Removing
    ///   the hint's label removed the issue; both wrap whole. Matched by their accessibility identifiers.
    /// - **"Clipped" text of the system's search field,** on Lines at the largest text: its placeholder shows whole.
    /// - **Dynamic Type of a navigation bar's button:** the system caps a bar button's text and shows the Large
    ///   Content Viewer on a long press instead.
    /// - **Dynamic Type of Settings' Data section:** its heading and footer, alone in the form, are reported as not
    ///   scaling, yet they scale with the others at the largest text.
    /// - **The system's alert,** which SwiftUI's `.alert` presents: iOS caps its text's size and draws parts that the
    ///   audit cannot name ("potentially inaccessible text"). Its title and button are read, and its button takes the
    ///   accent, which the contrast audit checks.
    /// - **The search field's "Clear text" button,** 20 pt, the system's: Close beside the field ends the search.
    private func isFalsePositive(
        _ issue: XCUIAccessibilityAuditIssue,
        on screen: String,
        barButtons: [CGRect]
    ) -> Bool {
        if screen.contains("alert") && [.elementDetection, .dynamicType].contains(issue.auditType) { return true }
        guard let element = issue.element else { return false }
        switch issue.auditType {
        case .textClipped:
            let spokenInWords = Self.spokenInWords.contains(element.identifier)
            let searchField = largestText && element.elementType == .searchField
            return screen.hasPrefix("Lines") && (spokenInWords || searchField)
        case .dynamicType:
            let barButton = element.elementType == .button && barButtons.contains(element.frame)
            let dataSection = screen.hasPrefix("Settings") &&
                ["Data", "Your favourites and the lines and stops data are kept."].contains(element.label)
            return barButton || dataSection
        case .hitRegion:
            return screen.hasPrefix("Lines") && element.elementType == .button && element.label == "Clear text"
        default:
            return false
        }
    }

    /// Whether `image` is mostly dark: the mean luminance of a 32 × 32 reduction of it is under one half. A screen in
    /// dark mode, on its black background, is; one in light mode, on the system's light grey, is not.
    static func isMostlyDark(_ image: UIImage) -> Bool {
        guard let source = image.cgImage else { return false }
        let side = 32
        var pixels = [UInt8](repeating: 0, count: side * side * 4)
        let drawn = pixels.withUnsafeMutableBytes { buffer in
            guard let context = CGContext(
                data: buffer.baseAddress,
                width: side,
                height: side,
                bitsPerComponent: 8,
                bytesPerRow: side * 4,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
            ) else { return false }
            context.interpolationQuality = .high
            context.draw(source, in: CGRect(x: 0, y: 0, width: side, height: side))
            return true
        }
        guard drawn else { return false }
        let luminance = stride(from: 0, to: pixels.count, by: 4).reduce(0.0) { total, index in
            total + 0.2126 * Double(pixels[index]) + 0.7152 * Double(pixels[index + 1]) +
                0.0722 * Double(pixels[index + 2])
        }
        return luminance / Double(side * side) / 255 < 0.5
    }
}
