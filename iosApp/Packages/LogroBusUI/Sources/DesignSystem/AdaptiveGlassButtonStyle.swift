import SwiftUI

/// `.glass` on iOS 26 and later; before, a capsule of `.regularMaterial` with a primary label, as `adaptiveGlass`
/// falls back (ADR-0003). For floating map controls; at least 44 × 44 pt (NFR-A11Y-04).
public struct AdaptiveGlassButtonStyle: PrimitiveButtonStyle {
    @Environment(\.glassRendering) private var rendering

    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        let button = Button(role: configuration.role, action: configuration.trigger) {
            configuration.label.frame(minWidth: Tokens.minTouchTarget, minHeight: Tokens.minTouchTarget)
        }
        if #available(iOS 26, *), rendering.usesLiquidGlass {
            button.buttonStyle(.glass)
        } else {
            button.buttonStyle(MaterialButtonStyle())
        }
    }
}

/// The iOS 18–25 fallback of the glass buttons: legible on any background, in both appearances.
private struct MaterialButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(.primary)
            .padding(.horizontal, Tokens.Spacing.small)
            .background(.regularMaterial, in: Capsule())
            .opacity(configuration.isPressed ? 0.6 : 1)
    }
}
