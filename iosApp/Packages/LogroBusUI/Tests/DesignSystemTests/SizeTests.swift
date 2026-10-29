@testable import DesignSystem
import SwiftUI
import Testing
import UIKit

/// NFR-A11Y-04 and design_guidelines.md §6.1, §6.2 and §8: touch targets of at least 44 pt, and a line badge that
/// never draws its code outside its colour, measured as SwiftUI lays them out.
@MainActor
struct SizeTests {
    @Test(arguments: [GlassRendering.automatic, .fallback])
    func lb011_theGlassButtonsAreAtLeast44Points(rendering: GlassRendering) {
        let icon = Button {} label: { Image(systemName: "square.3.layers.3d").accessibilityLabel("Layers") }
            .buttonStyle(AdaptiveGlassButtonStyle())
            .environment(\.glassRendering, rendering)
        let primary = Button {} label: { Text(verbatim: "Go") }
            .buttonStyle(AdaptiveGlassProminentButtonStyle())
            .environment(\.glassRendering, rendering)

        for size in [fittingSize(icon), fittingSize(primary)] {
            #expect(size.width >= Tokens.minTouchTarget && size.height >= Tokens.minTouchTarget, "\(size)")
        }
    }

    @Test(arguments: LineBadgeSize.allCases)
    func lb011_aLineBadgeIsNeverSmallerThanItsCode(size: LineBadgeSize) {
        // At 200 % text, in a row that offers little height, the colour still holds the whole code.
        let badge = LineBadge(code: "B1", color: 0xFFFD_0200, onColor: 0xFF00_0000, size: size)
            .environment(\.dynamicTypeSize, .accessibility3)
        let code = Text(verbatim: "B1").font(size.font).fontWeight(.bold)
            .environment(\.dynamicTypeSize, .accessibility3)

        let badgeSize = fittingSize(badge, in: CGSize(width: 1000, height: 10))
        let codeSize = fittingSize(code)
        #expect(badgeSize.width >= codeSize.width && badgeSize.height >= codeSize.height, "\(badgeSize), \(codeSize)")
    }

    private func fittingSize(_ view: some View, in proposal: CGSize = CGSize(width: 1000, height: 1000)) -> CGSize {
        UIHostingController(rootView: view).sizeThatFits(in: proposal)
    }
}
