import SwiftUI

/// The sizes of a ``LineBadge``; each is at least 32 × 24 pt (design_guidelines.md §6.1).
public enum LineBadgeSize: CaseIterable, Sendable {
    case small, regular, large

    var minWidth: CGFloat {
        switch self {
        case .small: 32
        case .regular: 40
        case .large: 48
        }
    }

    var minHeight: CGFloat {
        switch self {
        case .small: 24
        case .regular: 28
        case .large: 32
        }
    }

    var padding: CGFloat {
        switch self {
        case .small: 4
        case .regular: 6
        case .large: 8
        }
    }

    var font: Font {
        switch self {
        case .small: .caption
        case .regular: .subheadline
        case .large: .body
        }
    }
}

/// The only way a line is shown (design_guidelines.md §6.2): its code in `onColor` on its `color`, both opaque ARGB
/// values from the data (DD-08, NFR-A11Y-06). Lines 4 and B1 share a colour, as do 9 and B2, so the code always shows.
public struct LineBadge: View {
    private let code: String
    private let color: Int64
    private let onColor: Int64
    private let size: LineBadgeSize
    @Environment(\.locale) private var locale

    public init(code: String, color: Int64, onColor: Int64, size: LineBadgeSize = .regular) {
        self.code = code
        self.color = color
        self.onColor = onColor
        self.size = size
    }

    public var body: some View {
        // It grows with Dynamic Type rather than truncate; the code never wraps.
        Text(code)
            .font(size.font)
            .fontWeight(.bold)
            .foregroundStyle(Color(argb: onColor))
            .lineLimit(1)
            .padding(.horizontal, size.padding)
            .padding(.vertical, 2)
            .frame(minWidth: size.minWidth, minHeight: size.minHeight)
            // After the minimum frame: the colour is always the bigger of the minimum and the code, whatever the
            // row offers, so the code never spills out of it.
            .fixedSize()
            .background(Color(argb: color), in: RoundedRectangle(cornerRadius: Tokens.lineBadgeCornerRadius))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Self.spokenLabel(code: code, bundle: .designSystem(for: locale), locale: locale))
    }

    /// What VoiceOver says: the line, never its colour.
    static func spokenLabel(code: String, bundle: Bundle = .module, locale: Locale = .current) -> String {
        String(localized: "line.badge \(code)", bundle: bundle, locale: locale)
    }
}
