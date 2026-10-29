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
}

/// The only way a line is shown (design_guidelines.md §6.2): its code in `onColor` on its `color`, both opaque ARGB
/// values from the data (DD-08, NFR-A11Y-06). Lines 4 and B1 share a colour, as do 9 and B2, so the code always shows.
public struct LineBadge: View {
    private let code: String
    private let color: Int64
    private let onColor: Int64
    private let size: LineBadgeSize

    public init(code: String, color: Int64, onColor: Int64, size: LineBadgeSize = .regular) {
        self.code = code
        self.color = color
        self.onColor = onColor
        self.size = size
    }

    public var body: some View {
        EmptyView()
    }

    /// What VoiceOver says: the line, never its colour.
    static func spokenLabel(code: String, bundle: Bundle = .module, locale: Locale = .current) -> String {
        ""
    }
}
