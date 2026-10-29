@testable import DesignSystem
import Foundation
import Testing

/// The components' words follow SwiftUI's environment locale, not the simulator's language, so previews and
/// snapshots render the same everywhere.
struct LocalizationTests {
    @Test
    func lb011_theComponentsSpeakTheEnvironmentsLanguage() {
        let english = Bundle.designSystem(for: Locale(identifier: "en_GB"))
        let spanish = Bundle.designSystem(for: Locale(identifier: "es_ES"))

        #expect(String(localized: "eta.now", bundle: english) == "Now")
        #expect(String(localized: "eta.now", bundle: spanish) == "Ahora")
    }

    @Test
    func lb011_aLanguageWithoutStringsFallsBackToTheModule() {
        #expect(Bundle.designSystem(for: Locale(identifier: "fr_FR")) == .module)
    }
}
