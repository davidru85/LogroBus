import SwiftUI

/// The favourite toggle of a line or stop (FR-LINE-15): a star, filled in the accent colour once it is a favourite, at
/// least 44 pt to touch. `label` says what a tap does, such as "Add line 3 to favourites"; `action` gets the new value.
/// Content, so never glass (design_guidelines.md §5).
public struct FavouriteToggle: View {
    private let isFavourite: Bool
    private let label: Text
    private let action: (Bool) -> Void

    public init(isFavourite: Bool, label: Text, action: @escaping (Bool) -> Void) {
        self.isFavourite = isFavourite
        self.label = label
        self.action = action
    }

    public var body: some View {
        Button {
            action(!isFavourite)
        } label: {
            Image(systemName: isFavourite ? "star.fill" : "star")
                .foregroundStyle(isFavourite ? Tokens.Colors.accent : Tokens.Colors.secondaryText)
                .frame(minWidth: Tokens.minTouchTarget, minHeight: Tokens.minTouchTarget)
                .contentShape(Rectangle())
        }
        // Its own button inside a list row, which may be a button too.
        .buttonStyle(.borderless)
        .accessibilityLabel(label)
    }
}
