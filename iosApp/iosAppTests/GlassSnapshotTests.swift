import DesignSystem
import SnapshotTesting
import SwiftUI
import Testing

/// LB-011, ADR-0003: the glass helpers as Liquid Glass and as the iOS 18–25 fallback, which the environment forces
/// on the iOS 26 simulator, in light, dark, Increase Contrast and 200 % text, with the primary action enabled and
/// disabled. Glass and materials are composited by the system, so these run hosted by the app and draw from its key
/// window; a package test has no window. Reduce Transparency is checked by hand: no trait sets it.
@MainActor
struct GlassSnapshotTests {
    @Test(arguments: [GlassRendering.automatic, .fallback], GlassLook.allCases)
    func lb011_theGlassHelpers(rendering: GlassRendering, look: GlassLook) {
        let name = "\(rendering == .automatic ? "liquidGlass" : "fallback")-\(look)"
        assertSnapshot(
            of: AnyView(
                FloatingControls(primaryEnabled: look != .disabled)
                    .environment(\.glassRendering, rendering)
                    .environment(\.dynamicTypeSize, look == .largeText ? .accessibility3 : .large)
            ),
            as: .image(
                drawHierarchyInKeyWindow: true,
                // Glass is composited on the host's GPU, which may differ between this machine and CI.
                precision: 0.99,
                perceptualPrecision: 0.98,
                layout: .sizeThatFits,
                traits: look.traits
            ),
            named: name
        )
    }
}

enum GlassLook: CaseIterable, CustomStringConvertible, Sendable {
    case light, dark, highContrast, largeText, disabled

    var description: String {
        switch self {
        case .light: "light"
        case .dark: "dark"
        case .highContrast: "highContrast"
        case .largeText: "largeText"
        case .disabled: "disabled"
        }
    }

    @MainActor var traits: UITraitCollection {
        UITraitCollection(traitsFrom: [
            UITraitCollection(userInterfaceStyle: self == .dark ? .dark : .light),
            UITraitCollection(accessibilityContrast: self == .highContrast ? .high : .normal),
            UITraitCollection(displayScale: 3),
            // 8-bit sRGB, like the component snapshots. In wide-gamut 16-bit images, low-order noise between machines
            // failed CI's comparison although the images matched to the eye.
            UITraitCollection(displayGamut: .SRGB)
        ])
    }
}

/// Floating controls over a busy background, where glass shows: a recenter control, a layers button and the primary
/// "find routes" action, in one container. Solid stripes keep the reference images small.
private struct FloatingControls: View {
    let primaryEnabled: Bool

    var body: some View {
        AdaptiveGlassContainer(spacing: Tokens.Spacing.medium) {
            HStack(spacing: Tokens.Spacing.medium) {
                // A minimum, not a fixed frame, so the circle grows with the symbol at 200 %.
                Image(systemName: "location.fill")
                    .accessibilityLabel(Text(verbatim: "Recenter"))
                    .padding(Tokens.Spacing.small)
                    .frame(minWidth: Tokens.minTouchTarget, minHeight: Tokens.minTouchTarget)
                    .adaptiveGlass(in: Circle())
                Button {} label: {
                    Image(systemName: "square.3.layers.3d").accessibilityLabel(Text(verbatim: "Layers"))
                }
                .buttonStyle(AdaptiveGlassButtonStyle())
                Button {} label: { Text(verbatim: "Find routes") }
                    .buttonStyle(AdaptiveGlassProminentButtonStyle())
                    .disabled(!primaryEnabled)
            }
        }
        // Its ideal size: the snapshot proposes none, which would squeeze the labels into "…".
        .fixedSize()
        .padding(Tokens.Spacing.xLarge)
        .background {
            HStack(spacing: 0) {
                Color(argb: 0xFF26_9905)
                Color(argb: 0xFF0F_DCFC)
                Color(argb: 0xFFC1_00FB)
            }
        }
    }
}
