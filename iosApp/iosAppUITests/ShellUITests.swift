import XCTest

/// LB-012: the three tabs (FR-NAV-01, FR-NAV-02), each with its own stack (FR-NAV-03), and More from every tab root
/// (FR-NAV-04). "Two screens deep in Lines" is Lines, More, About data. LB-101: the Lines list at the Lines root.
/// LB-403: Settings, end to end. LB-405: the language a device in another language gets.
final class ShellUITests: XCTestCase {

    @MainActor
    func test_lb012_theTabsAreInOrder_withLinesSelected() {
        assertTabs(["Lines", "Near me", "Directions"], in: launch())
    }

    @MainActor
    func test_lb012_theTabsAreInOrder_withLinesSelected_inSpanish() {
        assertTabs(["Líneas", "Cerca de mí", "Cómo llegar"], in: launch(languages: ["es"], region: "es_ES"))
    }

    @MainActor
    func test_lb405_aDeviceInAnotherLanguage_getsTheAppInSpanish() {
        // NFR-I18N-01: iOS falls back to the app's development language, Spanish, as Android does from values/. The
        // package targets' words too: the screen's title from FeatureLines, a row's line from DesignSystem.
        let app = launch(languages: ["fr"], region: "fr_FR")
        assertTabs(["Líneas", "Cerca de mí", "Cómo llegar"], in: app)
        XCTAssertTrue(app.navigationBars["Líneas"].waitForExistence(timeout: 5))
        let row = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Línea 4, ")).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 10), "a row's line in Spanish, from DesignSystem")
        app.navigationBars["Líneas"].buttons["Más"].tap()
        XCTAssertTrue(app.buttons["Ajustes"].waitForExistence(timeout: 5))
    }

    @MainActor
    func test_lb405_theDevicesFirstLanguageThatTheAppSpeaks_isTheAppsLanguage() {
        let app = launch(languages: ["fr", "en"], region: "fr_FR")
        assertTabs(["Lines", "Near me", "Directions"], in: app)
        XCTAssertTrue(app.navigationBars["Lines"].waitForExistence(timeout: 5))
        XCTAssertTrue(line("4", in: app).waitForExistence(timeout: 10), "a row's line in English, from DesignSystem")
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
    func test_lb201_nearMeShowsTheRowThatTurnsLocationOn_andAsksForNothing() {
        // Location never asked, as on a fresh install.
        XCUIApplication().resetAuthorizationStatus(for: .location)
        let app = launch()

        app.tabBars.buttons["Near me"].tap()

        XCTAssertTrue(app.buttons["Turn on location to see nearby stops"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Near you"].exists)
        // FR-LOC-01: opening Near Me shows no prompt; only the row asks.
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        XCTAssertFalse(springboard.alerts.firstMatch.waitForExistence(timeout: 2), "no location prompt on opening")
    }

    @MainActor
    func test_lb301_withoutLocation_howToGetTheresFieldsAreBothEmpty() {
        // Location never asked, as on a fresh install: no origin (FR-PLAN-11).
        XCUIApplication().resetAuthorizationStatus(for: .location)
        let app = launch()

        app.tabBars.buttons["Directions"].tap()

        // Each field is one element, its name then what it shows.
        XCTAssertTrue(field("Origin", showing: "Where from?", in: app).waitForExistence(timeout: 5))
        XCTAssertTrue(field("Destination", showing: "Where to?", in: app).exists)
        // Nothing asks for location here either.
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        XCTAssertFalse(springboard.alerts.firstMatch.waitForExistence(timeout: 2), "no location prompt on opening")
    }

    @MainActor
    func test_lb304_withLocation_oneTapOnSwap_makesMyLocationTheDestination() {
        // FR-PLAN-08, end to end: location turned on in Near Me, as a user would, then one swap in How to Get There.
        XCUIApplication().resetAuthorizationStatus(for: .location)
        // Location is left as it was found: never asked.
        addTeardownBlock { @MainActor in XCUIApplication().resetAuthorizationStatus(for: .location) }
        let app = launch()
        app.tabBars.buttons["Near me"].tap()
        let turnOn = app.buttons["Turn on location to see nearby stops"]
        XCTAssertTrue(turnOn.waitForExistence(timeout: 5))
        turnOn.tap()
        // The system's prompt speaks the simulator's language, not the app's: English on CI, maybe Spanish locally.
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let whileUsing = NSPredicate(format: "label IN %@", ["Allow While Using App", "Permitir al usarse la app"])
        let allow = springboard.alerts.buttons.matching(whileUsing).firstMatch
        let labels = { springboard.alerts.buttons.allElementsBoundByIndex.map(\.label) }
        XCTAssertTrue(allow.waitForExistence(timeout: 10), "the system's location prompt, with: \(labels())")
        allow.tap()
        app.tabBars.buttons["Directions"].tap()
        XCTAssertTrue(field("Origin", showing: "My location", in: app).waitForExistence(timeout: 5))

        // The button, spoken by what it does.
        app.buttons["Swap origin and destination"].tap()

        XCTAssertTrue(field("Origin", showing: "Where from?", in: app).waitForExistence(timeout: 5))
        XCTAssertTrue(field("Destination", showing: "My location", in: app).exists)
    }

    @MainActor
    func test_lb303_aTimePickedFromTheDepartureMenu_isTheDeparture_untilLeaveNow() {
        // FR-PLAN-03, end to end: the departure's menu, the system's date and time picker, and back to leaving now.
        let app = launch()
        app.tabBars.buttons["Directions"].tap()
        let leaveNow = app.buttons["Leave now"]
        XCTAssertTrue(leaveNow.waitForExistence(timeout: 5))

        leaveNow.tap()
        app.buttons["Depart at…"].tap()
        let sheet = app.navigationBars["Departure time"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 5))
        sheet.buttons["Done"].tap()

        // The picker opened at now: the trip departs at now's minute, today unless midnight came meanwhile.
        let departing = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Depart ")).firstMatch
        XCTAssertTrue(departing.waitForExistence(timeout: 5))
        departing.tap()
        app.buttons["Leave now"].tap()
        XCTAssertTrue(app.buttons["Leave now"].waitForExistence(timeout: 5))
        XCTAssertTrue(departing.waitForNonExistence(timeout: 5))
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
        // Other favourites may be left on the simulator from a manual run: only line 3's card is checked.
        XCTAssertTrue(favouriteCard("3", in: app).waitForNonExistence(timeout: 5), "line 3 left the carousel")
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

    @MainActor
    func test_lb102_aSearchFiltersTheLines_withoutTheFavourites_andClearSearchBringsThemBack() {
        // FR-LINE-03 and FR-LINE-16, through the debug build's sample lines: "varea" finds lines 2 and B1.
        let app = launch()
        XCTAssertTrue(line("3", in: app).waitForExistence(timeout: 10))
        let favourites = app.staticTexts["Your favourite lines"]
        XCTAssertTrue(favourites.exists)

        let search = app.searchFields.firstMatch
        search.tap()
        search.typeText("varea")

        XCTAssertTrue(line("B1", in: app).waitForExistence(timeout: 5))
        XCTAssertTrue(line("2", in: app).exists)
        XCTAssertTrue(line("3", in: app).waitForNonExistence(timeout: 5), "line 3 does not match")
        XCTAssertFalse(favourites.exists, "the favourites hide while a search is active")
        XCTAssertTrue(app.staticTexts["Day lines"].exists, "the results keep the day group")
        XCTAssertTrue(app.staticTexts["Night lines (Búho)"].exists, "and the night group")

        // A search that finds nothing offers one action, which empties the field.
        search.typeText("x")
        let clear = app.buttons["Clear search"]
        XCTAssertTrue(clear.waitForExistence(timeout: 5))
        clear.tap()

        XCTAssertTrue(favourites.waitForExistence(timeout: 5), "the favourites are back")
        XCTAssertTrue(line("3", in: app).exists)
        // An empty search field reports its prompt as its value.
        XCTAssertEqual(search.value as? String, "Search lines", "Clear search emptied the field")
    }

    @MainActor
    func test_lb205_aStopRowOpensItsStopDetail_andBackReturnsToTheLine() {
        // FR-NAV-07's pushed door from line detail, through the debug build's scripted graph: line 3's stops are its
        // name's two ends, so its first outbound stop is "Campillo".
        let app = launch()
        XCTAssertTrue(line("3", in: app).waitForExistence(timeout: 10))
        line("3", in: app).tap()
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

    @MainActor
    func test_lb110_editListsTheFavourites_andALineRemovedThereLeavesTheCarousel_andItsStarGoesOff() {
        // FR-FAV-02, through the debug build's graph and the device's database: line 3 is added from its row, then
        // removed in the edit sheet. The test starts and ends with line 3 out of the favourites.
        let app = launch()
        XCTAssertTrue(line("3", in: app).waitForExistence(timeout: 10))
        removeFavourite("3", in: app)
        app.buttons["Add line 3 to favourites"].tap()
        XCTAssertTrue(favouriteCard("3", in: app).waitForExistence(timeout: 5))

        app.buttons["Edit favourite lines"].tap()
        let sheet = app.navigationBars["Edit favourite lines"]
        XCTAssertTrue(sheet.waitForExistence(timeout: 5))
        // The sheet's row of line 3: the list's own row, behind the sheet, has no delete control.
        let row = app.cells
            .containing(.image, identifier: "minus.circle.fill")
            .containing(NSPredicate(format: "label == %@", "Line 3"))
            .firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 5), "line 3 is in the sheet")
        // Edit mode's delete control, then the Delete button it reveals.
        row.images["minus.circle.fill"].tap()
        app.buttons["Delete"].firstMatch.tap()
        XCTAssertTrue(row.waitForNonExistence(timeout: 5), "line 3 left the sheet")
        sheet.buttons["Done"].tap()

        XCTAssertTrue(sheet.waitForNonExistence(timeout: 5))
        XCTAssertTrue(favouriteCard("3", in: app).waitForNonExistence(timeout: 5), "line 3 left the carousel")
        XCTAssertTrue(app.buttons["Add line 3 to favourites"].exists, "line 3's star is off")
    }

    @MainActor
    func test_lb403_settingsOffersItsChoices_aThemeAppliesAtOnce_andClearCacheSaysSo() {
        // FR-SET-01 through the debug build's graph and the device's settings. The test leaves the theme as it found
        // it, the system's.
        let app = launch()
        app.navigationBars["Lines"].buttons["More"].tap()
        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))

        // ADR-0020: the language is the system's per-app setting, which the row opens; launched in English.
        let language = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Language")).firstMatch
        XCTAssertTrue(language.waitForExistence(timeout: 5))
        XCTAssertTrue(language.label.contains("English"), "the language row says English: \(language.label)")
        // A menu picker reads as its name and the choice in use, whatever the device saved.
        let etaFormat = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Arrival times, ")).firstMatch
        XCTAssertTrue(etaFormat.waitForExistence(timeout: 5), "the ETA format's picker")

        addTeardownBlock { @MainActor in self.choose("System", in: "Theme", app: app) }
        choose("Dark", in: "Theme", app: app)

        app.buttons["Clear cache"].tap()
        let cleared = app.alerts["Cache cleared"]
        XCTAssertTrue(cleared.waitForExistence(timeout: 5))
        cleared.buttons.firstMatch.tap()
        XCTAssertTrue(cleared.waitForNonExistence(timeout: 5))
    }

    /// Chooses `option` in the menu of the picker named `picker`, which reads as its name and the choice in use, and
    /// waits until the picker shows it.
    @MainActor
    private func choose(_ option: String, in picker: String, app: XCUIApplication) {
        let button = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "\(picker), ")).firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: 5))
        button.tap()
        app.buttons[option].firstMatch.tap()
        XCTAssertTrue(app.buttons["\(picker), \(option)"].waitForExistence(timeout: 5), "\(picker) shows \(option)")
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

    /// The navigation bar's back button, labelled with the previous screen's title, Lines; the star is beside it.
    @MainActor
    private func back(in app: XCUIApplication) {
        app.navigationBars.buttons["Lines"].tap()
    }

    /// The row of the line with `code`, read as one element that starts with its badge's spoken label. A favourite
    /// card starts the same way, and it alone says where the line heads, so it is left out.
    @MainActor
    private func line(_ code: String, in app: XCUIApplication) -> XCUIElement {
        let row = NSPredicate(format: "label BEGINSWITH %@ AND NOT label CONTAINS %@", "Line \(code), ", "Towards")
        return app.descendants(matching: .any).matching(row).firstMatch
    }

    @MainActor
    private func assertTabs(_ labels: [String], in app: XCUIApplication) {
        let tabs = app.tabBars.buttons
        XCTAssertTrue(tabs[labels[0]].waitForExistence(timeout: 10))
        XCTAssertEqual((0..<tabs.count).map { tabs.element(boundBy: $0).label }, labels)
        XCTAssertTrue(tabs[labels[0]].isSelected)
    }

    /// How to Get There's field named `name`, showing `value`.
    @MainActor
    private func field(_ name: String, showing value: String, in app: XCUIApplication) -> XCUIElement {
        let field = NSPredicate(format: "label == %@ AND value == %@", name, value)
        return app.descendants(matching: .any).matching(field).firstMatch
    }

    @MainActor
    private func launch(
        languages: [String] = ["en"],
        region: String = "en_GB",
        textSize: String? = nil
    ) -> XCUIApplication {
        let app = XCUIApplication()
        // The device's languages in order, as an old-style property list array: ("fr", "es").
        let list = "(" + languages.map { "\"\($0)\"" }.joined(separator: ", ") + ")"
        app.launchArguments += ["-AppleLanguages", list, "-AppleLocale", region]
        if let textSize { app.launchArguments += ["-UIPreferredContentSizeCategoryName", textSize] }
        // LB-008 part 2: these assertions are about the list and its labels, not about which buses run today, so the
        // app starts the scripted graph instead of downloading from the live source.
        app.launchArguments += ["-logrobus.preview-data"]
        app.launch()
        return app
    }
}
