@testable import DesignSystem
import Testing

/// ADR-0003 and design_guidelines.md §5: Liquid Glass on iOS 26 and later, a material on iOS 18–25.
struct AdaptiveGlassTests {
    // The real iOS 18–25 path is the views' `#available(iOS 26, *)` check, covered by the forced-fallback snapshots
    // and the nightly iOS 18 run (LB-412).
    @Test
    func lb011_automaticRenderingDrawsLiquidGlass() {
        #expect(GlassRendering.automatic.usesLiquidGlass)
    }

    @Test
    func lb011_snapshotsCanForceTheFallbackOnIos26() {
        #expect(!GlassRendering.fallback.usesLiquidGlass)
    }
}
