import Foundation
import Testing

/// LB-001: the built app, not just its spec, is iPhone only, targets iOS 18 and uses the store bundle id.
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
    func lb001_appUsesTheStoreBundleId() {
        // OQ-14: shared with Android, and fixed after the first release.
        #expect(Bundle.main.bundleIdentifier == "com.ruizurraca.logrobus")
    }
}
