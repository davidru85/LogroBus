@testable import DesignSystem
import Foundation
import Shared
import Testing

/// Spec §5.7, design_guidelines.md §6.3 and NFR-A11Y-02: every ETA case, drawn and spoken, in Spanish and English.
/// The cases and words are Android's (EtaTextTest), so both platforms show and say the same.
struct EtaTextTests {
    @Test
    func lb011_eachEtaCaseShowsItsValueAndSecondaryLine() {
        let english = designSystemBundle("en")
        #expect(EtaCase.all.map { EtaStrings.lines($0.eta, bundle: english, locale: Locale(identifier: "en")) }
            == EtaCase.all.map(\.english))
    }

    @Test
    func lb011_eachEtaCaseShowsItsValueAndSecondaryLine_inSpanish() {
        let spanish = designSystemBundle("es")
        #expect(EtaCase.all.map { EtaStrings.lines($0.eta, bundle: spanish, locale: Locale(identifier: "es")) }
            == EtaCase.all.map(\.spanish))
    }

    @Test
    func lb011_eachEtaCaseIsSpoken() {
        let english = designSystemBundle("en")
        #expect(EtaCase.all.map { EtaStrings.spoken($0.eta, bundle: english, locale: Locale(identifier: "en")) }
            == EtaCase.all.map(\.englishSpoken))
    }

    @Test
    func lb011_eachEtaCaseIsSpoken_inSpanish() {
        let spanish = designSystemBundle("es")
        #expect(EtaCase.all.map { EtaStrings.spoken($0.eta, bundle: spanish, locale: Locale(identifier: "es")) }
            == EtaCase.all.map(\.spanishSpoken))
    }

    @Test
    func lb011_nowPulses_andHoldsStillWithReduceMotion() {
        #expect(EtaStrings.nowOpacity(dimmed: true, reduceMotion: false) == EtaStrings.pulseMinOpacity)
        #expect(EtaStrings.nowOpacity(dimmed: true, reduceMotion: true) == 1)
        #expect(EtaStrings.nowOpacity(dimmed: false, reduceMotion: false) == 1)
    }

    @Test
    func lb011_thePulseKeepsNowReadable() {
        // NFR-A11Y-06: at its faintest, "Now" still reaches 4.5:1 on the surfaces a row sits on.
        for palette in [Tokens.light, Tokens.dark] {
            for background in [palette.surface, palette.surfaceContainer] {
                let text = blend(palette.onSurface, over: background, opacity: EtaStrings.pulseMinOpacity)
                #expect(contrastRatio(first: text, second: background) >= 4.5)
            }
        }
    }

    private func blend(_ colour: Int64, over background: Int64, opacity: Double) -> Int64 {
        let channel = { (shift: Int64) -> Int64 in
            let top = Double((colour >> shift) & 0xFF)
            let bottom = Double((background >> shift) & 0xFF)
            return Int64((top * opacity + bottom * (1 - opacity)).rounded())
        }
        return 0xFF00_0000 | channel(16) << 16 | channel(8) << 8 | channel(0)
    }
}
