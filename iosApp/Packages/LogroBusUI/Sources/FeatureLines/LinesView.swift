import DesignSystem
import Shared
import SwiftUI

/// The root of the Lines tab (LB-101, LB-103): the favourite lines (FR-LINE-12), then day lines and night (Búho) lines,
/// each sorted by code (FR-LINE-01). A card or a row opens its line through `onOpenLine`, which the app shell pushes;
/// the shell adds the More action too.
public struct LinesView: View {
    @State private var viewModel: LinesViewModel
    @Environment(\.scrollToTopRequests) private var scrollToTopRequests
    @State private var scrollPosition = ScrollPosition(edge: .top)
    private let onOpenLine: (Int32) -> Void

    public init(viewModel: LinesViewModel = LinesViewModel(), onOpenLine: @escaping (Int32) -> Void = { _ in }) {
        _viewModel = State(initialValue: viewModel)
        self.onOpenLine = onOpenLine
    }

    public var body: some View {
        content
            .navigationTitle(Text("lines.title", bundle: .module))
            // Cancelled when the screen leaves, which stops the use case's collection; restarted by Retry.
            .task(id: viewModel.attempt) { await viewModel.observe() }
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: viewModel.state) {
        case .loading:
            // Spec §7.3: the final layout with placeholders, never a lone spinner.
            // VoiceOver hears that the lines are loading rather than the shapes (design_guidelines.md §8).
            lineList(Self.placeholders, favourites: nil)
                .redacted(reason: .placeholder)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text("lines.loading", bundle: .module))
        case .content(let content):
            lineList(content.groups, favourites: content.favourites)
        case .empty:
            message(Text("lines.empty", bundle: .module))
        case .error(let error):
            message(error.reason.message)
        }
    }

    /// The favourite lines, if any are known yet (FR-LINE-12), then each group under its heading (FR-LINE-01).
    /// Re-selecting the tab at this root scrolls it to the top, its first heading included (FR-NAV-03).
    private func lineList(_ groups: [LineGroupUi], favourites: [FavouriteLineCardUi]?) -> some View {
        List {
            if let favourites {
                Section {
                    FavouriteLines(cards: favourites, onOpenLine: onOpenLine)
                } header: {
                    Text("lines.favourites.title", bundle: .module)
                }
            }
            ForEach(groups, id: \.kind) { group in
                Section {
                    ForEach(group.lines, id: \.id) { line in
                        LineRow(
                            line: line,
                            onOpen: { onOpenLine(line.id) },
                            onToggle: { isFavourite in
                                viewModel.onEvent(LinesEventSetFavourite(lineId: line.id, isFavourite: isFavourite))
                            }
                        )
                    }
                } header: {
                    Text(group.kind.title, bundle: .module)
                }
            }
        }
        .scrollPosition($scrollPosition)
        .onChange(of: scrollToTopRequests) {
            withAnimation { scrollPosition.scrollTo(edge: .top) }
        }
    }

    /// FR-ERR-05 and spec §7.3: one sentence, and Retry.
    private func message(_ text: Text) -> some View {
        ContentUnavailableView {
            text
        } actions: {
            RetryButton {
                viewModel.onEvent(LinesEventRetry.shared)
            }
        }
    }

    /// Sample rows for the redacted first load: shaped like lines, never read.
    private static let placeholders = [
        LineGroupUi(
            kind: .day,
            lines: (1...8).map { id in
                LineRowUi(
                    id: Int32(id), code: "\(id)", displayName: "Lardero - Hospital San Pedro",
                    color: 0xFF9E_9E9E, onColor: 0xFFFF_FFFF, isOperational: true, isFavourite: false
                )
            }
        )
    ]
}

/// FR-LINE-12 and FR-LINE-14: the favourite lines' cards in the user's order, or one hint while there are none, so the
/// section never disappears. At most 20 cards (FR-FAV-05), so a plain stack: each card takes the tallest one's height.
private struct FavouriteLines: View {
    let cards: [FavouriteLineCardUi]
    let onOpenLine: (Int32) -> Void

    var body: some View {
        if cards.isEmpty {
            Text("lines.favourites.hint", bundle: .module)
                .font(.subheadline)
                .foregroundStyle(Tokens.Colors.secondaryText)
        } else {
            ScrollView(.horizontal) {
                HStack(spacing: Tokens.Spacing.small) {
                    ForEach(cards, id: \.id) { card in
                        FavouriteCard(card: card) { onOpenLine(card.id) }
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
                .scrollTargetLayout()
                .padding(.horizontal, Tokens.Spacing.screenMargin)
            }
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

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: Tokens.Spacing.small) {
                LineBadge(code: card.code, color: card.color, onColor: card.onColor)
                Text(verbatim: card.displayName)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                if !card.headsigns.isEmpty {
                    // Symbols, the same in every language; VoiceOver reads the headsigns as words.
                    Text(verbatim: card.headsigns.map { "→ \($0)" }.joined(separator: " · "))
                        .font(.caption)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                        .accessibilityLabel(towards)
                }
            }
            .padding(Tokens.Spacing.medium)
            .frame(width: Self.width, alignment: .topLeading)
            .frame(maxHeight: .infinity, alignment: .topLeading)
            .background(
                Color(uiColor: .secondarySystemGroupedBackground),
                in: RoundedRectangle(cornerRadius: Tokens.cardCornerRadius)
            )
        }
        .buttonStyle(.plain)
    }

    /// "Towards Hospital San Pedro, Towards Lardero".
    private var towards: Text {
        let parts = card.headsigns.map { Text("lines.towards \($0)", bundle: .module) }
        guard let first = parts.first else { return Text(verbatim: "") }
        return parts.dropFirst().reduce(first) { spoken, next in spoken + Text(verbatim: ", ") + next }
    }

    // Wide enough for a two-line name and a short headsign. The inset row, which clips its content, leaves about 334
    // pt on a 390 pt iPhone: two cards and the edge of a third, which shows the cards scroll.
    private static let width: CGFloat = 150
}

/// FR-LINE-02: the badge, the display name, which wraps rather than clips, and the status chip of a line that is not
/// operational. VoiceOver reads the row as one: the line, its name, then its status (NFR-A11Y-02); a double tap opens
/// the line. The favourite toggle is its own element, labelled by the line (FR-LINE-15).
private struct LineRow: View {
    let line: LineRowUi
    let onOpen: () -> Void
    let onToggle: (Bool) -> Void

    var body: some View {
        HStack(spacing: Tokens.Spacing.small) {
            Button(action: onOpen) {
                HStack(spacing: Tokens.Spacing.medium) {
                    LineBadge(code: line.code, color: line.color, onColor: line.onColor)
                    VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall) {
                        // Names are shown as delivered, never translated (AGENTS.md §7).
                        Text(verbatim: line.displayName)
                        if !line.isOperational {
                            StatusChip()
                        }
                    }
                    Spacer(minLength: 0)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            FavouriteToggle(isFavourite: line.isFavourite, label: toggleLabel, action: onToggle)
        }
    }

    private var toggleLabel: Text {
        line.isFavourite
            ? Text("lines.favourite.remove \(line.code)", bundle: .module)
            : Text("lines.favourite.add \(line.code)", bundle: .module)
    }
}

/// A label, not an action: a line that is not operational.
private struct StatusChip: View {
    var body: some View {
        Text("lines.not_operational", bundle: .module)
            .font(.caption.weight(.medium))
            .foregroundStyle(Tokens.Colors.onErrorContainer)
            .padding(.horizontal, Tokens.Spacing.small)
            .padding(.vertical, Tokens.Spacing.xSmall)
            .background(Tokens.Colors.errorContainer, in: RoundedRectangle(cornerRadius: Tokens.lineBadgeCornerRadius))
    }
}

extension LineKind {
    /// The group's heading (design_guidelines.md §9, keys `lines.group.*`).
    var title: LocalizedStringKey {
        switch self {
        case .day: "lines.group.day"
        case .night: "lines.group.night"
        }
    }
}

#Preview("Lines") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview()))
    }
}

#Preview("Lines, dark") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview()))
            .preferredColorScheme(.dark)
    }
}

#Preview("Lines, large text") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview()))
            .dynamicTypeSize(.accessibility3)
    }
}

#Preview("Favourite lines") {
    NavigationStack {
        LinesView(
            viewModel: LinesViewModel(
                useCases: SharedDependencies.shared.preview()
                    .favouriteLine(id: 10).favouriteLine(id: 4).favouriteLine(id: 101)
            )
        )
    }
}

#Preview("Favourite lines, dark, large text") {
    NavigationStack {
        LinesView(
            viewModel: LinesViewModel(
                useCases: SharedDependencies.shared.preview().favouriteLine(id: 10).favouriteLine(id: 4)
            )
        )
        .preferredColorScheme(.dark)
        .dynamicTypeSize(.accessibility3)
    }
}

#Preview("Loading") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview().loading()))
    }
}

#Preview("Loading, dark") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview().loading()))
            .preferredColorScheme(.dark)
    }
}

#Preview("No lines") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview().neverSynced()))
    }
}

#Preview("No lines, dark") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview().neverSynced()))
            .preferredColorScheme(.dark)
    }
}

#Preview("No network") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview().offline()))
    }
}

#Preview("No network, dark") {
    NavigationStack {
        LinesView(viewModel: LinesViewModel(useCases: SharedDependencies.shared.preview().offline()))
            .preferredColorScheme(.dark)
    }
}
