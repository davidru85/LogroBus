import DesignSystem
import Shared
import SwiftUI

/// FR-LINE-12 and FR-LINE-14: the favourite lines' cards in the user's order, or one hint while there are none, so the
/// section never disappears. A plain stack, so each card takes the tallest one's height: LB-110 caps the cards at 20
/// (FR-FAV-05), and even a few dozen would be cheap.
struct FavouriteLines: View {
    let cards: [FavouriteLineCardUi]
    let onOpenLine: (Int32) -> Void

    var body: some View {
        if cards.isEmpty {
            Text("lines.favourites.hint", bundle: .module)
                .font(.subheadline)
                .foregroundStyle(Tokens.Colors.secondaryText)
                // VoiceOver reads words, never "white star".
                .accessibilityLabel(Text("lines.favourites.hint_spoken", bundle: .module))
        } else {
            ScrollView(.horizontal) {
                HStack(spacing: Tokens.Spacing.small) {
                    ForEach(cards, id: \.id) { card in
                        FavouriteCard(card: card) { onOpenLine(card.id) }
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
                .scrollTargetLayout()
            }
            // Content margins, not padding: view-aligned snapping keeps them.
            .contentMargins(.horizontal, Tokens.Spacing.screenMargin, for: .scrollContent)
            .scrollTargetBehavior(.viewAligned)
            .scrollIndicators(.hidden)
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
        }
    }
}

/// FR-LINE-13: the badge, the display name in at most two lines, and both headsigns, "→ Hospital San Pedro · →
/// Lardero". VoiceOver reads the card as one, the headsigns as "Towards …", and opens the line. Content, so never
/// glass.
private struct FavouriteCard: View {
    let card: FavouriteLineCardUi
    let onOpen: () -> Void
    // Wide enough for a two-line name and a short headsign. The inset row, which clips its content, leaves about 334
    // pt on a 390 pt iPhone: two cards and the edge of a third, which shows the cards scroll. Cards widen with the
    // text, up to most of the row, so at large sizes a name keeps more than a word per line.
    @ScaledMetric(relativeTo: .subheadline) private var width: CGFloat = 150

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: Tokens.Spacing.small) {
                LineBadge(code: card.code, color: card.color, onColor: card.onColor)
                Text(verbatim: card.displayName)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                if !card.headsigns.isEmpty {
                    // Symbols, the same in every language; VoiceOver reads the headsigns as words. No-break spaces keep
                    // each arrow with its headsign, and the dot with the headsign before it, when the text wraps.
                    Text(verbatim: card.headsigns.map { "→\u{00A0}\($0)" }.joined(separator: "\u{00A0}· "))
                        .font(.caption)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                        .accessibilityLabel(towards)
                }
            }
            .padding(Tokens.Spacing.medium)
            .containerRelativeFrame(.horizontal, alignment: .topLeading) { length, _ in
                min(width, length * Self.maxShare)
            }
            .frame(maxHeight: .infinity, alignment: .topLeading)
            .background(Tokens.Colors.cardBackground, in: RoundedRectangle(cornerRadius: Tokens.cardCornerRadius))
        }
        .buttonStyle(.plain)
    }

    /// "Towards Hospital San Pedro, Towards Lardero".
    private var towards: Text {
        let parts = card.headsigns.map { Text("lines.towards \($0)", bundle: .module) }
        guard let first = parts.first else { return Text(verbatim: "") }
        return parts.dropFirst().reduce(first) { spoken, next in Text("\(spoken), \(next)", bundle: .module) }
    }

    // At large sizes, a card takes at most this share of the row, so the next card's edge still shows.
    private static let maxShare: CGFloat = 0.8
}
