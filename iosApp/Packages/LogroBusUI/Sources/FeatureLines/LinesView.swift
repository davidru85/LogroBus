import DesignSystem
import Shared
import SwiftUI

/// The root of the Lines tab (LB-101, LB-103, LB-102, LB-110): the search field (FR-LINE-03), the favourite lines
/// (FR-LINE-12) with Edit, which opens their edit sheet (FR-FAV-02), then day lines and night (Búho) lines, each sorted
/// by code (FR-LINE-01). While a search is active, its results in the same groups, without the favourites
/// (FR-LINE-16). A card or a row opens its line through `onOpenLine`, which the app shell pushes; the shell adds the
/// More action too.
public struct LinesView: View {
    @State private var viewModel: LinesViewModel
    @Environment(\.scrollToTopRequests) private var scrollToTopRequests
    @State private var scrollPosition = ScrollPosition(edge: .top)
    @State private var editing = false
    private let onOpenLine: (Int32) -> Void

    public init(viewModel: LinesViewModel = LinesViewModel(), onOpenLine: @escaping (Int32) -> Void = { _ in }) {
        _viewModel = State(initialValue: viewModel)
        self.onOpenLine = onOpenLine
    }

    public var body: some View {
        content
            .navigationTitle(Text("lines.title", bundle: .module))
            // Under the title, always shown and in every state: the system's search belongs to the navigation stack,
            // unlike Android's, which heads the list and shows with lines only (design_guidelines.md §3.1). A query
            // typed during the first load filters the lines once they arrive.
            .searchable(
                text: query,
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: Text("lines.search", bundle: .module)
            )
            .sheet(isPresented: $editing) { FavouriteLinesSheet(viewModel: viewModel) }
            .favouriteLimitAlert(viewModel.favouriteLimit) {
                viewModel.onEvent(LinesEventFavouriteLimitShown.shared)
            }
            // Cancelled when the screen leaves, which stops the use case's collection; restarted by Retry.
            .task(id: viewModel.attempt) { await viewModel.observe() }
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: viewModel.state) {
        case .loading:
            // Spec §7.3: the final layout with placeholders, never a lone spinner.
            // VoiceOver hears that the lines are loading rather than the shapes (design_guidelines.md §8).
            // With the favourites section, so nothing moves when the lines arrive.
            lineList(Self.placeholders, favourites: [])
                .redacted(reason: .placeholder)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text("lines.loading", bundle: .module))
        case .content(let content):
            lineList(content.groups, favourites: content.favourites, editable: content.editable)
        case .empty:
            message(Text("lines.empty", bundle: .module))
        case .error(let error):
            message(error.reason.message)
        case .searchResults(let results):
            lineList(results.groups, favourites: nil)
        case .noMatches:
            noMatches
        }
    }

    /// The search field's text is the view model's, which each key sends as an event (ADR-0007).
    private var query: Binding<String> {
        Binding(
            get: { viewModel.query },
            set: { viewModel.onEvent(LinesEventSearch(query: $0)) }
        )
    }

    /// Spec §7.3: a search that found no line says so in one sentence, which VoiceOver announces without moving its
    /// focus (NFR-A11Y), and offers one action, which empties the field.
    private var noMatches: some View {
        ContentUnavailableView {
            Text("lines.no_matches", bundle: .module)
        } actions: {
            Button {
                viewModel.onEvent(LinesEventSearch(query: ""))
            } label: {
                Text("search.clear", bundle: .module)
            }
            .buttonStyle(.bordered)
            .controlSize(.large)
        }
        // The list's background, so the screen keeps its colour as each key moves between results and none.
        .background(Tokens.Colors.groupedBackground)
        .onAppear {
            // In the app's language, which the process follows; a preview forced to another locale may differ.
            AccessibilityNotification.Announcement(String(localized: "lines.no_matches", bundle: .module)).post()
        }
    }

    /// The favourite lines, if any are known yet (FR-LINE-12), then each group under its heading (FR-LINE-01).
    /// Re-selecting the tab at this root scrolls it to the top, its first heading included (FR-NAV-03).
    private func lineList(
        _ groups: [LineGroupUi],
        favourites: [FavouriteLineCardUi]?,
        editable: [any EditableFavouriteLineUi] = []
    ) -> some View {
        List {
            if let favourites {
                Section {
                    FavouriteLines(cards: favourites, onOpenLine: onOpenLine)
                } header: {
                    HStack {
                        // The title's width is the header's when there is no Edit, as before LB-110.
                        Text("lines.favourites.title", bundle: .module)
                            .foregroundStyle(Tokens.Colors.secondaryText)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        // LB-110: Edit, while there is a favourite to edit, even one with no card.
                        if !editable.isEmpty {
                            Button { editing = true } label: {
                                Text("lines.favourites.edit", bundle: .module)
                                    .frame(minWidth: Tokens.minTouchTarget, minHeight: Tokens.minTouchTarget)
                                    .contentShape(Rectangle())
                            }
                            // VoiceOver names it by what it edits; the name keeps the word it shows (WCAG 2.5.3).
                            .accessibilityLabel(Text("lines.favourites.edit_title", bundle: .module))
                            .textCase(nil)
                        }
                    }
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
                        .foregroundStyle(Tokens.Colors.secondaryText)
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

/// FR-LINE-02: the badge, the display name, which wraps rather than clips, and the status chip of a line that is not
/// operational. VoiceOver reads the row as one: the line, its name, then its status (NFR-A11Y-02); a double tap opens
/// the line. The favourite toggle is its own element, labelled by the line (FR-LINE-15).
private struct LineRow: View {
    let line: LineRowUi
    let onOpen: () -> Void
    let onToggle: (Bool) -> Void
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        HStack(spacing: Tokens.Spacing.small) {
            Button(action: onOpen) {
                // At accessibility sizes the badge goes above the name, which then has the row's width beside the
                // toggle and breaks between words rather than inside one ("Valdegastea").
                let layout = dynamicTypeSize.isAccessibilitySize
                    ? AnyLayout(VStackLayout(alignment: .leading, spacing: Tokens.Spacing.xSmall))
                    : AnyLayout(HStackLayout(spacing: Tokens.Spacing.medium))
                layout {
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

#Preview("Search") {
    NavigationStack {
        LinesView(viewModel: searching("ca", useCases: SharedDependencies.shared.preview().favouriteLine(id: 4)))
    }
}

#Preview("Search, dark, large text") {
    NavigationStack {
        LinesView(viewModel: searching("ca"))
            .preferredColorScheme(.dark)
            .dynamicTypeSize(.accessibility3)
    }
}

#Preview("No matches") {
    NavigationStack { LinesView(viewModel: searching("tranvía")) }
}

#Preview("No matches, dark") {
    NavigationStack { LinesView(viewModel: searching("tranvía")).preferredColorScheme(.dark) }
}

/// A view model whose search field holds `query` (LB-102).
@MainActor
private func searching(
    _ query: String,
    useCases: PreviewUseCases = SharedDependencies.shared.preview()
) -> LinesViewModel {
    let viewModel = LinesViewModel(useCases: useCases)
    viewModel.onEvent(LinesEventSearch(query: query))
    return viewModel
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
