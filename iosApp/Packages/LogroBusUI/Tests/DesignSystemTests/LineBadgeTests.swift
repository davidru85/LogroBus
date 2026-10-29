@testable import DesignSystem
import Foundation
import Testing

/// design_guidelines.md §6.1 and §6.2, NFR-A11Y-02: how a line is shown and spoken.
struct LineBadgeTests {
    @Test
    func lb011_aLineBadgeSaysItsLine_neverItsColour() {
        let spanish = LineBadge.spokenLabel(
            code: "3", bundle: designSystemBundle("es"), locale: Locale(identifier: "es")
        )
        let english = LineBadge.spokenLabel(
            code: "B1", bundle: designSystemBundle("en"), locale: Locale(identifier: "en")
        )
        #expect(spanish == "Línea 3")
        #expect(english == "Line B1")
    }

    @Test(arguments: LineBadgeSize.allCases)
    func lb011_everyLineBadgeSizeIsAtLeast32By24(size: LineBadgeSize) {
        #expect(size.minWidth >= 32)
        #expect(size.minHeight >= 24)
    }
}
