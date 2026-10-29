import SwiftUI

/// `.glassProminent` on iOS 26 and later; a prominent bordered button before (ADR-0003). For the one primary action
/// of a screen, such as "Buscar rutas", tinted with the accent; at least 44 × 44 pt (NFR-A11Y-04). An enabled label is
/// onAccent, not the system's white, which would fall below 4.5:1 on the light dark-mode accent; a disabled one keeps
/// the system's disabled look.
public struct AdaptiveGlassProminentButtonStyle: PrimitiveButtonStyle {
    @Environment(\.glassRendering) private var rendering
    @Environment(\.isEnabled) private var isEnabled

    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        let button = Button(role: configuration.role, action: configuration.trigger) {
            configuration.label.frame(minWidth: Tokens.minTouchTarget, minHeight: Tokens.minTouchTarget)
        }
        .tint(Tokens.Colors.accent)
        .foregroundStyle(isEnabled ? AnyShapeStyle(Tokens.Colors.onAccent) : AnyShapeStyle(.secondary))
        if #available(iOS 26, *), rendering.usesLiquidGlass {
            button.buttonStyle(.glassProminent)
        } else {
            button.buttonStyle(.borderedProminent)
        }
    }
}
