import SwiftUI
import UIKit

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
        /// Text that warns, such as "Actualizado hace N s" on stale data (spec §7.3).
        public let warning: Int64
    }

    public static let light = Palette(
        accent: 0xFF2F5D8A, onAccent: 0xFFFFFFFF, surface: 0xFFFCFCFC, onSurface: 0xFF1B1B1B,
        surfaceContainer: 0xFFF0F0F0, secondaryText: 0xFF464646, warning: 0xFF8A5300
    )
    public static let dark = Palette(
        accent: 0xFFA7C8EC, onAccent: 0xFF0E2F4D, surface: 0xFF121212, onSurface: 0xFFE3E3E3,
        surfaceContainer: 0xFF1F1F1F, secondaryText: 0xFFC6C6C6, warning: 0xFFF2B861
    )

    /// The colours that follow light and dark mode.
    public enum Colors {
        public static let accent = Color(light: light.accent, dark: dark.accent)
        public static let secondaryText = Color(light: light.secondaryText, dark: dark.secondaryText)
        public static let warning = Color(light: light.warning, dark: dark.warning)
    }

    /// The spacing scale and the screen margin (design_guidelines.md §6.1).
    public enum Spacing {
        public static let xSmall: CGFloat = 4
        public static let small: CGFloat = 8
        public static let medium: CGFloat = 12
        public static let large: CGFloat = 16
        public static let xLarge: CGFloat = 24
        public static let xxLarge: CGFloat = 32
        public static let screenMargin: CGFloat = 16
    }

    /// Cards use 12 pt corners (spec §7.5).
    public static let cardCornerRadius: CGFloat = 12
    /// A line badge's corners: an identity element that never morphs.
    public static let lineBadgeCornerRadius: CGFloat = 6
    /// The smallest touch target (NFR-A11Y-04).
    public static let minTouchTarget: CGFloat = 44
}

public extension Color {
    /// An opaque colour from an ARGB value, as the shared code sends line colours.
    init(argb: Int64) {
        self.init(uiColor: UIColor(argb: argb))
    }

    /// A colour that follows light and dark mode.
    init(light: Int64, dark: Int64) {
        self.init(uiColor: UIColor { traits in
            UIColor(argb: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

extension UIColor {
    convenience init(argb: Int64) {
        self.init(
            red: CGFloat((argb >> 16) & 0xFF) / 255,
            green: CGFloat((argb >> 8) & 0xFF) / 255,
            blue: CGFloat(argb & 0xFF) / 255,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255
        )
    }
}
