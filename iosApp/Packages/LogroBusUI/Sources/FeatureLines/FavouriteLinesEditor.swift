import DesignSystem
import Shared
import SwiftUI

/// LB-110 (FR-FAV-02): the favourite lines' edit sheet: its title and Done over the list of favourite lines.
struct FavouriteLinesEditor: View {
    let rows: [any EditableFavouriteLineUi]
    let onReorder: ([Int32]) -> Void
    let onRemove: (Int32) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            FavouriteLinesList(rows: rows, onReorder: onReorder, onRemove: onRemove)
                .navigationTitle(Text("lines.favourites.edit_title", bundle: .module))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button { dismiss() } label: { Text("lines.favourites.done", bundle: .module) }
                    }
                }
        }
    }
}

/// The edit sheet's list: a system list in edit mode, where each row has its delete control, the line's badge and
/// name, or "no longer served", and its reorder control. VoiceOver reads a row as one, and moves it with "Move up" and
/// "Move down" as well as the system's own actions (NFR-A11Y). A removed line goes through `onRemove`, its toggle's
/// event, so the list's star follows; a new order goes through `onReorder` once a move ends.
struct FavouriteLinesList: View {
    let rows: [any EditableFavouriteLineUi]
    let onReorder: ([Int32]) -> Void
    let onRemove: (Int32) -> Void
    // The order the user sees. A move or a delete changes it here first, as the list's animation needs, and the saved
    // favourites then arrive the same. A favourite removed or added elsewhere, or a new saved order, replaces it.
    @State private var shown: [any EditableFavouriteLineUi]

    init(
        rows: [any EditableFavouriteLineUi],
        onReorder: @escaping ([Int32]) -> Void,
        onRemove: @escaping (Int32) -> Void
    ) {
        self.rows = rows
        self.onReorder = onReorder
        self.onRemove = onRemove
        _shown = State(initialValue: rows)
    }

    var body: some View {
        List {
            ForEach(Array(shown.enumerated()), id: \.element.id) { index, row in
                EditRow(row: row)
                    .accessibilityActions {
                        if index > 0 {
                            Button { move(index, to: index - 1) } label: {
                                Text("lines.favourites.move_up", bundle: .module)
                            }
                        }
                        if index < shown.count - 1 {
                            Button { move(index, to: index + 1) } label: {
                                Text("lines.favourites.move_down", bundle: .module)
                            }
                        }
                    }
            }
            .onMove { source, destination in
                shown.move(fromOffsets: source, toOffset: destination)
                onReorder(shown.map(\.id))
            }
            .onDelete { offsets in
                let removed = offsets.map { shown[$0].id }
                shown.remove(atOffsets: offsets)
                removed.forEach(onRemove)
            }
        }
        .environment(\.editMode, .constant(.active))
        .onChange(of: rows.map(\.id)) { shown = rows }
    }

    private func move(_ index: Int, to target: Int) {
        // `move(fromOffsets:toOffset:)` takes the place before which the row lands.
        shown.move(fromOffsets: IndexSet(integer: index), toOffset: target > index ? target + 1 : target)
        onReorder(shown.map(\.id))
    }
}

/// The line's badge and name, or "no longer served" for a favourite whose line has left the catalogue. At
/// accessibility sizes the badge goes above the name, as in the list's rows, so the name breaks between words.
private struct EditRow: View {
    let row: any EditableFavouriteLineUi
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        switch onEnum(of: row) {
        case .served(let line):
            let layout = dynamicTypeSize.isAccessibilitySize
                ? AnyLayout(VStackLayout(alignment: .leading, spacing: Tokens.Spacing.xSmall))
                : AnyLayout(HStackLayout(spacing: Tokens.Spacing.medium))
            layout {
                LineBadge(code: line.code, color: line.color, onColor: line.onColor)
                // Names are shown as delivered, never translated (AGENTS.md §7).
                Text(verbatim: line.displayName)
            }
            .accessibilityElement(children: .combine)
        case .noLongerServed:
            Text("lines.favourites.no_longer_served", bundle: .module)
                .foregroundStyle(Tokens.Colors.secondaryText)
        }
    }
}

/// The edit sheet over the view model's favourite lines, read here so that it follows a favourite removed or added.
struct FavouriteLinesSheet: View {
    let viewModel: LinesViewModel

    var body: some View {
        FavouriteLinesEditor(
            rows: (viewModel.state as? LinesUiStateContent)?.editable ?? [],
            onReorder: { lineIds in
                viewModel.onEvent(LinesEventReorderFavourites(lineIds: lineIds.map { KotlinInt(value: $0) }))
            },
            onRemove: { lineId in
                viewModel.onEvent(LinesEventSetFavourite(lineId: lineId, isFavourite: false))
            }
        )
    }
}

/// The edit sheet over lines 10, 4 and B1, and line 99, which has left the catalogue, so it is no longer served.
@MainActor
private func editing() -> some View {
    let viewModel = LinesViewModel(
        useCases: SharedDependencies.shared.preview()
            .favouriteLine(id: 10).favouriteLine(id: 4).favouriteLine(id: 99).favouriteLine(id: 101)
    )
    return FavouriteLinesSheet(viewModel: viewModel).task { await viewModel.observe() }
}

#Preview("Edit favourite lines") {
    editing()
}

#Preview("Edit favourite lines, dark, large text") {
    editing()
        .preferredColorScheme(.dark)
        .dynamicTypeSize(.accessibility3)
}
