@testable import FeatureSettings
import Foundation
import Testing

/// LB-405 (NFR-I18N-01): SwiftUI looks the app's words up, and formats, in the language iOS gave the app, with the
/// device's region. With the device's own locale, a French device got Spanish, the development language, from the
/// app's bundles, but English from the lookups that take a locale, such as the tab labels' LocalizedStringResource.
struct AppLocaleTests {
    @Test
    func lb405_aDeviceInAnotherLanguage_getsTheAppsLanguageInItsRegion() {
        #expect(Locale.app(localization: "en", device: Locale(identifier: "fr_FR")).identifier == "en_FR")
        #expect(Locale.app(localization: "es", device: Locale(identifier: "fr_FR")).identifier == "es_FR")
    }

    @Test
    func lb405_aDeviceInTheAppsLanguage_keepsItsOwnLocale() {
        let device = Locale(identifier: "en_GB@calendar=gregorian")
        #expect(Locale.app(localization: "en", device: device) == device)
        #expect(Locale.app(localization: "es", device: Locale(identifier: "es_ES")).identifier == "es_ES")
        #expect(Locale.app(localization: nil, device: device) == device)
    }
}
