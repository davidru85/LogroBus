@testable import DesignSystem
import Testing

/// ADR-0003 and design_guidelines.md §5: Liquid Glass on iOS 26 and later, a material on iOS 18–25.
struct AdaptiveGlassTests {
    @Test
    func lb011_glassIsLiquidGlassWhereTheSystemHasIt() {
        #expect(GlassRendering.automatic.usesLiquidGlass(systemHasGlass: true))
    }

    @Test
    func lb011_glassFallsBackToAMaterialBeforeIos26() {
        #expect(!GlassRendering.automatic.usesLiquidGlass(systemHasGlass: false))
    }

    @Test
    func lb011_snapshotsCanForceTheFallbackOnIos26() {
        #expect(!GlassRendering.fallback.usesLiquidGlass(systemHasGlass: true))
    }
}
