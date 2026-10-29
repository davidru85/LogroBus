import DesignSystem
import SnapshotTesting
import SwiftUI
import Testing

/// LB-011, ADR-0003: the glass helpers as Liquid Glass and as the iOS 18–25 fallback, which the environment forces
/// on the iOS 26 simulator. Glass and materials are composited by the system, so these run hosted by the app and draw
/// from its key window; a package test has no window.
@MainActor
struct GlassSnapshotTests {
    @Test(arguments: [GlassRendering.automatic, .fallback], [false, true])
    func lb011_theGlassHelpers(rendering: GlassRendering, dark: Bool) {
        let name = "\(rendering == .automatic ? "liquidGlass" : "fallback")-\(dark ? "dark" : "light")"
        assertSnapshot(
            of: AnyView(FloatingControls().environment(\.glassRendering, rendering)),
            as: .image(
                drawHierarchyInKeyWindow: true,
                precision: 0.99,
                perceptualPrecision: 0.98,
                layout: .sizeThatFits,
                traits: UITraitCollection(userInterfaceStyle: dark ? .dark : .light)
            ),
            named: name
        )
    }
}

/// Floating controls over a busy background, where glass shows: a recenter control, a layers button and the primary
/// "find routes" action.
private struct FloatingControls: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(argb: 0xFF26_9905), Color(argb: 0xFF0F_DCFC), Color(argb: 0xFFC1_00FB)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            HStack(spacing: Tokens.Spacing.medium) {
                Image(systemName: "location.fill")
                    .accessibilityLabel("Recenter")
                    .frame(width: Tokens.minTouchTarget, height: Tokens.minTouchTarget)
                    .adaptiveGlass(in: Circle())
                Button {} label: { Image(systemName: "square.3.layers.3d").accessibilityLabel("Layers") }
                    .buttonStyle(AdaptiveGlassButtonStyle())
                Button("Find routes") {}
                    .buttonStyle(AdaptiveGlassProminentButtonStyle())
            }
        }
        .frame(width: 320, height: 120)
    }
}
