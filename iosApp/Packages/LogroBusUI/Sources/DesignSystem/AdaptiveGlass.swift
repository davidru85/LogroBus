import SwiftUI

/// How the glass helpers draw (ADR-0003): Liquid Glass where the system has it, iOS 26 and later, or the material
/// fallback of iOS 18–25. Snapshot tests force the fallback, to cover it on the iOS 26 simulator.
public enum GlassRendering: Sendable {
    case automatic
    case fallback

    /// Whether to draw Liquid Glass, given whether the running system has it.
    func usesLiquidGlass(systemHasGlass: Bool) -> Bool {
        false
    }
}

public extension EnvironmentValues {
    @Entry var glassRendering: GlassRendering = .automatic
}

public extension View {
    /// Glass for floating controls only (design_guidelines.md §5): never on list rows, cards or line badges.
    func adaptiveGlass<S: Shape>(in shape: S) -> some View {
        self
    }
}
