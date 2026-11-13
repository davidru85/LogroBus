import UIKit
import XCTest

/// LB-406 (NFR-A11Y): every screen the app has passes Xcode's accessibility audit, at the default text size and at the
/// largest: touch targets of at least 44 pt, no clipped text, Dynamic Type, contrast, and a description and trait for
/// every element. One launch per text size walks the screens, and the issues of all of them are reported together,
/// each with a screenshot of its screen. The default size's launch then relaunches the app in its Dark theme and walks
/// the screens again.
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
        // LB-208: the stop's detail and its Save star, from the line the walk already has open, so neither the tab nor
        // the list is walked again.
        try MyStopsAudit.auditStar(audit, types, in: app, into: &issues)
        detail.buttons["Remove line 3 from favourites"].tap()
        XCTAssertTrue(detail.buttons["Add line 3 to favourites"].waitForExistence(timeout: 5))
        detail.buttons["Lines"].tap()

        try auditMoreAndSettings(audit, types, largestText: largestText, in: app, into: &issues)
        tap(app.tabBars.buttons["Near me"])
        XCTAssertTrue(app.buttons[Self.turnOnLocation].waitForExistence(timeout: 5))
        try audit.run("Near me", types, into: &issues)
        // LB-208: "My stops" and its edit sheet, here where the walk already is, in its own file, as this one is at
        // SwiftLint's length limit.
        try MyStopsAudit.auditSection(audit, types, in: app, into: &issues)
        try auditDirections(audit, types, in: app, into: &issues)
    }

    /// Near Me's row without location (LB-201), which the audit waits for: the screen is blank until access is read.
    private static let turnOnLocation = "Turn on location to see nearby stops"

    /// The Lines search, More, Settings, its alert, About data, Near me and Directions again, in dark mode: the dark
    /// accent and secondary text on the system's dark backgrounds.
    ///
    /// The app relaunches in its Dark theme, which a launch argument sets over the saved one, and which gives its
    /// windows the dark interface style that the device's dark mode would. The device's dark mode cannot be relied on:
    /// under `xcodebuild` on CI the app stayed light although `XCUIDevice` read dark (PR #50), as others report of
    /// iOS 26.2 and later.
    @MainActor
    private func auditInDarkMode(in app: XCUIApplication, into issues: inout [String]) throws {
        app.launchArguments += ["-settings.theme", "DARK"]
        app.launch()
        dismissSystemBanners()
        let audit = Audit(test: self, app: app, largestText: false, dark: true)
        XCTAssertTrue(app.navigationBars["Lines"].waitForExistence(timeout: 10))
        try search("varea", in: app)
        try audit.run("Lines, searching, dark", .all, into: &issues)
        closeSearch(in: app)
        try auditMoreAndSettings(audit, .all, largestText: false, in: app, into: &issues, suffix: ", dark")
        app.tabBars.buttons["Near me"].tap()
        XCTAssertTrue(app.buttons[Self.turnOnLocation].waitForExistence(timeout: 5))
        try audit.run("Near me, dark", .all, into: &issues)
        try auditDirections(audit, .all, in: app, into: &issues, suffix: ", dark")
    }

    /// How to Get There's root (LB-301), which the audit waits for: its fields, with the origin's hint in the
    /// secondary text colour when location is off. Then the departure's picker (LB-303), a sheet with its title, its
    /// buttons and the system's date and time picker.
    @MainActor
    private func auditDirections(
        _ audit: Audit,
        _ types: XCUIAccessibilityAuditType,
        in app: XCUIApplication,
        into issues: inout [String],
        suffix: String = ""
    ) throws {
        app.tabBars.buttons["Directions"].tap()
        XCTAssertTrue(app.navigationBars["How to get there"].waitForExistence(timeout: 5))
        let origin = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "Origin")).firstMatch
        XCTAssertTrue(origin.waitForExistence(timeout: 5))
        try audit.run("Directions" + suffix, types, into: &issues)

        app.buttons["Leave now"].tap()
        app.buttons["Depart at…"].tap()
        let sheet = app.navigationBars["Departure time"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 5))
        try audit.run("Departure time" + suffix, types, into: &issues)
        sheet.buttons["Cancel"].tap()
        XCTAssertTrue(sheet.waitForNonExistence(timeout: 5))
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
        // Re-selecting the tab pops its stack to its root (FR-NAV-03). A tap on the bar is a synthesised event and can
        // be dropped while the pushed screen is still settling (PR #62: the bar never came back within 5 s, with no
        // other failure), so the tap is repeated while the root is not on screen. The assertion itself stays.
        let linesRoot = app.navigationBars["Lines"]
        var attempts = 0
        while !linesRoot.exists, attempts < 3 {
            attempts += 1
            app.tabBars.buttons["Lines"].tap()
            _ = linesRoot.waitForExistence(timeout: 2)
        }
        XCTAssertTrue(linesRoot.waitForExistence(timeout: 5))
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
        // Location is scripted as never granted in the preview graph (`-logrobus.preview-data` gives
        // `LocationPermission.NotGranted`), so Near Me shows its "Turn on location" row (LB-201) without touching the
        // simulator's permission database. `resetAuthorizationStatus(for: .location)` was removed: on CI's simulator it
        // timed out ("Error resetting authorization status"), failing the audit before it ran a single check, and the
        // app never reads the real permission under the preview graph anyway.
        app.launchArguments += ["-AppleLanguages", "(\"en\")", "-AppleLocale", "en_GB"]
        if let textSize { app.launchArguments += ["-UIPreferredContentSizeCategoryName", textSize] }
        // The scripted graph: the audit looks at labels and touch targets, and its screens must not depend on the live
        // source or show an error state it is not auditing (LB-008 part 2).
        app.launchArguments += ["-logrobus.preview-data"]
        app.launch()
        dismissSystemBanners()
        return app
    }
}

/// One launch's audits: each runs Xcode's audit on the screen on display and reports what is not a known false
/// positive.
@MainActor
struct Audit {
    let test: XCTestCase
    let app: XCUIApplication
    let largestText: Bool
    /// Whether the screens are meant to be in dark mode, which each audit then checks in its screenshot.
    var dark = false

    /// The accessibility identifiers of texts whose labels say their symbols in words.
    private static let spokenInWords = ["lines.favourites.hint", "lines.favourites.card.headsigns"]

    /// Audits the screen on display for `types`, and adds each issue that is not a known false positive to `issues`,
    /// named after the screen, its kind and its element. The audit sometimes reads a screen that is still settling,
    /// and then cannot name an issue's element: it runs again, up to three times, while an issue has none. It also runs
    /// again when XCTest gives up on it, as it did on busy CI runners (`gaveUp(_:)`), after a pause.
    ///
    /// A screen meant to be dark that is not is an issue too: on CI the app kept light mode when the device was asked
    /// for dark, and the dark audits audited light mode again without saying so (PR #50).
    func run(_ screen: String, _ types: XCUIAccessibilityAuditType, into issues: inout [String]) throws {
        let screenshot = app.screenshot()
        let shot = XCTAttachment(screenshot: screenshot)
        shot.name = screen
        shot.lifetime = .keepAlways
        test.add(shot)
        if dark && !Self.isMostlyDark(screenshot.image) {
            issues.append("\(screen): the screen is not in dark mode")
        }
        var found: [String] = []
        for attempt in 1...3 {
            found = []
            var unnamed = false
            let barButtons = app.navigationBars.buttons.allElementsBoundByIndex.map(\.frame)
            let systemPicker = screen.hasPrefix("Departure time") ? app.datePickers.firstMatch.frame : nil
            do {
                try app.performAccessibilityAudit(for: types) { issue in
                    if isFalsePositive(issue, on: screen, barButtons: barButtons) { return true }
                    if let systemPicker, isTheSystemPickersOwn(issue, picker: systemPicker) { return true }
                    unnamed = unnamed || issue.element == nil
                    let element = issue.element.map { "\"\($0.label)\" at \($0.frame)" } ?? "no element"
                    let kind = issue.compactDescription
                    found.append("\(screen): \(kind), \(element): \(issue.detailedDescription)")
                    return true
                }
            } catch let error as NSError where Self.gaveUp(error) && attempt < 3 {
                Thread.sleep(forTimeInterval: Self.pauseAfterGivingUp * Double(attempt))
                continue
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
    /// - **Contrast of the system's search field, of the Clear cache alert's title and of the departure sheet's title
    ///   (LB-303), in the app's Dark theme:** all show white on dark, yet the audit reports them as failing. With the
    ///   device's dark mode, locally, it did not.
    /// Whether `issue` is the system's date picker's own, on How to Get There's departure sheet (LB-303), where the
    /// sheet shows `picker`, its frame. Exempted by name on the owner's decision (2026-11-07), since the app cannot
    /// change them; the sheet's title and buttons are still audited:
    /// - **Dynamic Type:** the calendar's month and day numbers keep their size at the largest text.
    /// - **Contrast:** the picker's time label, the system's own colours.
    /// - **Unattributed text and Dynamic Type,** with no element: the picker is all the sheet draws besides its bar.
    private func isTheSystemPickersOwn(_ issue: XCUIAccessibilityAuditIssue, picker: CGRect) -> Bool {
        let pickersKinds: [XCUIAccessibilityAuditType] = [.dynamicType, .contrast, .elementDetection]
        guard pickersKinds.contains(issue.auditType) else { return false }
        guard let element = issue.element else { return issue.auditType != .contrast }
        return picker.insetBy(dx: -1, dy: -1).contains(element.frame)
    }

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
        case .contrast:
            let searchField = screen == "Lines, searching, dark" && element.elementType == .searchField
            let alertTitle = screen == "Settings, Clear cache's alert, dark" && element.label == "Cache cleared"
            let sheetTitle = screen == "Departure time, dark" && element.label == "Departure time"
            return dark && (searchField || alertTitle || sheetTitle)
        default:
            return false
        }
    }

    /// How long the audit waits before it runs again after XCTest gave up on it, times the attempt: 5 s, then 10 s.
    /// Run again at once, in PR #51, it gave up three times on Lines, each attempt slower than the one before (6, 15
    /// and 34 s), as if each new audit waited behind the one abandoned.
    private static let pauseAfterGivingUp: TimeInterval = 5

    /// Whether XCTest gave up on an audit: "Audit failed to complete in time", after about 16 s on one screen. It did
    /// on CI, on `main` and in PRs #50 and #51, each time on a launch's first screen, Lines, which other runs audit in
    /// a few seconds.
    private static func gaveUp(_ error: NSError) -> Bool {
        error.domain == "com.apple.xcode.xctest.accessibilityAudit" && error.code == -56
    }

    /// Whether `image` is mostly dark: the mean luminance of a 32 × 32 reduction of it is under one half. A screen in
    /// dark mode, on its black background, is; one in light mode, on the system's light grey, is not.
    private static func isMostlyDark(_ image: UIImage) -> Bool {
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
