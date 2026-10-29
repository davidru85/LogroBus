import SwiftUI

/// How the glass helpers draw (ADR-0003): Liquid Glass where the system has it, iOS 26 and later, or the material
/// fallback of iOS 18–25. Snapshot tests force the fallback, to cover it on the iOS 26 simulator.
public enum GlassRendering: Sendable {
    case automatic
    case fallback

    /// Whether to draw Liquid Glass, given whether the running system has it.
    func usesLiquidGlass(systemHasGlass: Bool) -> Bool {
        self == .automatic && systemHasGlass
    }
}

public extension EnvironmentValues {
    @Entry var glassRendering: GlassRendering = .automatic
}

public extension View {
    /// Glass for floating controls only (design_guidelines.md §5): never on list rows, cards or line badges. Group
    /// neighbouring controls in a `GlassEffectContainer` on iOS 26.
    func adaptiveGlass<S: Shape>(in shape: S) -> some View {
        modifier(AdaptiveGlass(shape: shape))
    }
}

private struct AdaptiveGlass<S: Shape>: ViewModifier {
    let shape: S
    @Environment(\.glassRendering) private var rendering

    func body(content: Content) -> some View {
        if #available(iOS 26, *), rendering.usesLiquidGlass(systemHasGlass: true) {
            content.glassEffect(.regular.interactive(), in: shape)
        } else {
            content.background(.regularMaterial, in: shape)
        }
    }
}

/// `.glass` on iOS 26 and later; a bordered button before (ADR-0003). For floating map controls.
public struct AdaptiveGlassButtonStyle: PrimitiveButtonStyle {
    @Environment(\.glassRendering) private var rendering

    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26, *), rendering.usesLiquidGlass(systemHasGlass: true) {
            Button(configuration).buttonStyle(.glass)
        } else {
            Button(configuration).buttonStyle(.bordered)
        }
    }
}

/// `.glassProminent` on iOS 26 and later; a prominent bordered button before (ADR-0003). For the one primary
/// action of a screen, such as "Buscar rutas", tinted with the accent.
public struct AdaptiveGlassProminentButtonStyle: PrimitiveButtonStyle {
    @Environment(\.glassRendering) private var rendering

    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 26, *), rendering.usesLiquidGlass(systemHasGlass: true) {
            Button(configuration).buttonStyle(.glassProminent).tint(Tokens.Colors.accent)
        } else {
            Button(configuration).buttonStyle(.borderedProminent).tint(Tokens.Colors.accent)
        }
    }
}
