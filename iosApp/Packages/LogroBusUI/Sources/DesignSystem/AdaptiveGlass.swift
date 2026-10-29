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
