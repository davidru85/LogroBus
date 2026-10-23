import XCTest

/// LB-001: the app launches without crashing, on the start tab of FR-NAV-02, in both shipped languages.
final class LaunchTests: XCTestCase {

    @MainActor
    func test_lb001_appLaunchesOnTheLinesTab() {
        assertLaunch(language: "en", region: "en_GB", showing: "Lines")
    }

    @MainActor
    func test_lb001_appLaunchesOnTheLinesTab_inSpanish() {
        assertLaunch(language: "es", region: "es_ES", showing: "Líneas")
    }

    @MainActor
    private func assertLaunch(language: String, region: String, showing label: String) {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(\(language))", "-AppleLocale", region]
        app.launch()
        XCTAssertTrue(app.staticTexts[label].waitForExistence(timeout: 10), "\"\(label)\" is shown")
    }
}
