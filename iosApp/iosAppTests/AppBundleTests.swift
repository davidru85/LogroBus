import Foundation
import Shared
import Testing

/// LB-001: the built app, not just its spec, is iPhone only, targets iOS 18 and uses the store bundle id.
/// LB-005: it refuses cleartext HTTP. LB-010: it starts the shared dependency graph at launch.
struct AppBundleTests {

    @Test
    func lb001_appIsIphoneOnly() {
        // NFR-PLAT-03: UIDeviceFamily 1 is iPhone; 2 would add iPad.
        #expect(Bundle.main.object(forInfoDictionaryKey: "UIDeviceFamily") as? [Int] == [1])
    }

    @Test
    func lb001_appSupportsPortraitAndLandscape() {
        // NFR-A11Y-01 (WCAG 2.2 SC 1.3.4): content is not locked to one orientation, as on Android.
        let orientations = Bundle.main.object(forInfoDictionaryKey: "UISupportedInterfaceOrientations") as? [String]
        #expect(
            Set(orientations ?? []) == [
                "UIInterfaceOrientationPortrait",
                "UIInterfaceOrientationLandscapeLeft",
                "UIInterfaceOrientationLandscapeRight"
            ]
        )
    }

    @Test
    func lb001_appTargetsIos18() {
        // ADR-0003: iOS 18 minimum.
        #expect(Bundle.main.object(forInfoDictionaryKey: "MinimumOSVersion") as? String == "18.0")
    }

    @Test
    func lb005_appUsesDefaultTransportSecurity() {
        // NFR-SEC-01: App Transport Security with no exceptions refuses cleartext HTTP; any exception adds this key.
        // The bundle must be the app's, or a missing key would prove nothing.
        #expect(Bundle.main.bundleIdentifier == "com.ruizurraca.logrobus")
        #expect(Bundle.main.object(forInfoDictionaryKey: "NSAppTransportSecurity") == nil)
    }

    @Test
    func lb201_theAppSaysWhyItAsksForLocation_inSpanishAndEnglish() throws {
        // FR-LOC-01: iOS shows the purpose in its prompt, and App Store Connect requires it once the binary uses Core
        // Location. The app speaks Spanish and English (LB-405).
        let purposes = try ["es", "en"].map { language in
            let path = try #require(Bundle.main.path(forResource: language, ofType: "lproj"))
            let bundle = try #require(Bundle(path: path))
            return bundle.localizedString(forKey: "NSLocationWhenInUseUsageDescription", value: nil, table: "InfoPlist")
        }
        #expect(purposes[0].contains("ubicación"))
        #expect(purposes[1].contains("location"))
    }

    @Test
    func lb201_theAppAsksForLocationWhenInUseOnly_neverAlways() {
        // security_privacy.md §3: foreground location only, and no background mode that would keep it running.
        #expect(Bundle.main.object(forInfoDictionaryKey: "NSLocationWhenInUseUsageDescription") != nil)
        #expect(Bundle.main.object(forInfoDictionaryKey: "NSLocationAlwaysAndWhenInUseUsageDescription") == nil)
        #expect(Bundle.main.object(forInfoDictionaryKey: "NSLocationAlwaysUsageDescription") == nil)
        let backgroundModes = Bundle.main.object(forInfoDictionaryKey: "UIBackgroundModes") as? [String] ?? []
        #expect(!backgroundModes.contains("location"))
    }

    @Test
    func lb010_appStartsTheDependencyGraphAtLaunch() {
        // ADR-0014: the app's initialiser starts the graph once, before any screen asks SharedDependencies for it.
        #expect(SharedDependencies.shared.isReady)
    }

    @Test
    func lb001_appUsesTheStoreBundleId() {
        // OQ-14: shared with Android, and fixed after the first release.
        #expect(Bundle.main.bundleIdentifier == "com.ruizurraca.logrobus")
    }
}
