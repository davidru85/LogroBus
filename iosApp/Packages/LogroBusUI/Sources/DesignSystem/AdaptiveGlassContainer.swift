import SwiftUI

/// Neighbouring glass controls, so their shapes blend (design_guidelines.md §5): a `GlassEffectContainer` on iOS 26
/// and later, and the plain content before, where materials do not blend. Feature code uses it instead of an
/// availability check of its own (ADR-0003).
public struct AdaptiveGlassContainer<Content: View>: View {
    private let spacing: CGFloat?
    private let content: Content
    @Environment(\.glassRendering) private var rendering

    public init(spacing: CGFloat? = nil, @ViewBuilder content: () -> Content) {
        self.spacing = spacing
        self.content = content()
    }

    public var body: some View {
        if #available(iOS 26, *), rendering.usesLiquidGlass {
            GlassEffectContainer(spacing: spacing) { content }
        } else {
            content
        }
    }
}
