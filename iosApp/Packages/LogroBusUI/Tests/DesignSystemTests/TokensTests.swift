@testable import DesignSystem
import Shared
import Testing

/// NFR-MNT-04, NFR-A11Y-06 and OQ-18: the placeholder palette is neutral, and all its text is readable.
struct TokensTests {
    private static let schemes = [("light", Tokens.light), ("dark", Tokens.dark)]

    @Test(arguments: schemes)
    func lb011_everyTextColourOfThePlaceholderSchemesReaches4point5To1(name: String, palette: Tokens.Palette) {
        let pairs = [
            ("accent", palette.accent, palette.onAccent),
            ("surface", palette.surface, palette.onSurface),
            ("container", palette.surfaceContainer, palette.onSurface),
            ("secondary", palette.surface, palette.secondaryText),
            ("secondary on container", palette.surfaceContainer, palette.secondaryText),
            ("warning", palette.surface, palette.warning),
            ("warning on container", palette.surfaceContainer, palette.warning),
            ("error container", palette.errorContainer, palette.onErrorContainer)
        ]
        for (role, background, text) in pairs {
            #expect(contrastRatio(first: background, second: text) >= 4.5, "\(name) \(role)")
        }
    }

    @Test(arguments: schemes)
    func lb011_thePlaceholderSurfacesAreGrey(name: String, palette: Tokens.Palette) {
        for (role, colour) in [("surface", palette.surface), ("container", palette.surfaceContainer)] {
            let channels = [(colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF]
            let spread = (channels.max() ?? 0) - (channels.min() ?? 0)
            #expect(spread <= 5, "\(name) \(role) is tinted")
        }
    }

    @Test
    func lb011_theSchemesMatchAndroidsTokens() {
        // One palette on both platforms (design_guidelines.md §6.1): the values of Android's Tokens.kt.
        #expect(Tokens.light.accent == 0xFF2F5D8A)
        #expect(Tokens.dark.accent == 0xFFA7C8EC)
        #expect(Tokens.light.surface == 0xFFFCFCFC)
        #expect(Tokens.dark.surface == 0xFF121212)
        #expect(Tokens.light.warning == 0xFF8A5300)
        #expect(Tokens.dark.warning == 0xFFF2B861)
        // Material's error container on Android, which the status chip of a line out of service uses (LB-101).
        #expect(Tokens.light.errorContainer == 0xFFF9DEDC && Tokens.light.onErrorContainer == 0xFF410E0B)
        #expect(Tokens.dark.errorContainer == 0xFF8C1D18 && Tokens.dark.onErrorContainer == 0xFFF9DEDC)
    }
}
