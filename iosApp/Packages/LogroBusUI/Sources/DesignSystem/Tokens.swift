import SwiftUI

/// Every brand value of the iOS app (NFR-MNT-04, design_guidelines.md §5 and §6.1), the same as Android's `Tokens`.
/// Until OQ-18's designs arrive (LB-413), the palette is a neutral placeholder: greys plus one accessible accent.
/// It is defined here and nowhere else.
public enum Tokens {
    /// One scheme's colours, as opaque ARGB values, so tests can measure them and views can draw them.
    public struct Palette: Sendable {
        public let accent: Int64
        public let onAccent: Int64
        public let surface: Int64
        public let onSurface: Int64
        public let surfaceContainer: Int64
        public let secondaryText: Int64
        public let warning: Int64
    }

    public static let light = Palette(
        accent: 0xFF6750A4, onAccent: 0xFFFFFFFF, surface: 0xFFFEF7FF, onSurface: 0xFF1D1B20,
        surfaceContainer: 0xFFF3EDF7, secondaryText: 0xFF49454F, warning: 0xFFFFC107
    )
    public static let dark = Palette(
        accent: 0xFFD0BCFF, onAccent: 0xFF381E72, surface: 0xFF141218, onSurface: 0xFFE6E0E9,
        surfaceContainer: 0xFF211F26, secondaryText: 0xFFCAC4D0, warning: 0xFFFFC107
    )
}
