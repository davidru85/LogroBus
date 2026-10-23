import XCTest

/// LB-001: the app launches without crashing, on the start tab of FR-NAV-02, in both shipped languages.
final class LaunchTests: XCTestCase {

    @MainActor
    func test_lb001_launchesOnLines() {
        assertLaunch(language: "en", region: "en_GB", showing: "Lines")
    }

    @MainActor
    func test_lb001_launchesOnLines_inSpanish() {
        assertLaunch(language: "es", region: "es_ES", showing: "Líneas")
    }

    @MainActor
    private func assertLaunch(language: String, region: String, showing label: String) {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(\(language))", "-AppleLocale", region]
        app.launch()
        // TODO(LB-012): with the tab shell, assert app.tabBars.buttons[label].isSelected instead.
        XCTAssertTrue(app.staticTexts[label].waitForExistence(timeout: 10), "\"\(label)\" is shown")
    }
}
