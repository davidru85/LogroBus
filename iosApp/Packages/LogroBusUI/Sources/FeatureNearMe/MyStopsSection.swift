import DesignSystem
import Shared
import SwiftUI

/// LB-208 part 4 (FR-HOME-02, FR-FAV-02): "My stops", above "Near you" in Near Me.
///
/// The rows follow the saved stops, so a save made on a stop's detail appears at once. Edit opens the sheet that
/// reorders, renames and removes them; while nothing is saved, the section says how to fill it and offers no Edit.
public struct MyStopsSection: View {
    private let viewModel: MyStopsViewModel
    private let onEdit: () -> Void
    private let onOpenStop: (Int32) -> Void

    /// The section over the screen's own `viewModel`, which the screen keeps because the edit sheet it presents reads
    /// the same rows. A sheet cannot be presented from inside a `List`'s section — the section is not a presentation
    /// context, and the tap would do nothing — so Edit only reports itself and the screen presents it.
    public init(viewModel: MyStopsViewModel, onEdit: @escaping () -> Void, onOpenStop: @escaping (Int32) -> Void = { _ in }) {
        self.viewModel = viewModel
        self.onEdit = onEdit
        self.onOpenStop = onOpenStop
    }

    public var body: some View {
        Section {
            if viewModel.rows.isEmpty {
                Text("nearme.my_stops.empty", bundle: .module)
                    .foregroundStyle(Tokens.Colors.onSurface)
            } else {
                ForEach(viewModel.rows, id: \.stopId) { row in
                    Button {
                        onOpenStop(row.stopId)
                    } label: {
                        Text(verbatim: row.label)
                            .foregroundStyle(Tokens.Colors.onSurface)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(.isButton)
                }
            }
        } header: {
            HStack {
                Text("nearme.my_stops.title", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
                Spacer()
                if !viewModel.rows.isEmpty {
                    Button(action: onEdit) {
                        Text("nearme.my_stops.edit", bundle: .module)
                    }
                    .buttonStyle(.borderless)
                }
            }
        }
        // Cancelled when the section leaves, which stops the collection.
        .task { await viewModel.observe() }
    }
}

/// LB-208 (FR-FAV-02): the sheet that reorders, renames and removes the saved stops.
///
/// Moving is a VoiceOver **custom action** on the row, as it is a TalkBack action on Android, so a screen reader can
/// change the order; the row's place is its accessibility value, so it is read on the row and again after each move.
struct MyStopsEditSheet: View {
    let rows: [SavedStopRowUi]
    let onReorder: ([KotlinInt]) -> Void
    let onRename: (Int32, String?) -> Void
    let onDelete: (Int32) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var renaming: SavedStopRowUi?
    @State private var draft = ""

    /// "1 of 2", for a row's accessibility value.
    private func place(_ position: Int, of count: Int) -> String {
        let format = String(localized: "nearme.my_stops.position", bundle: .module)
        return String(format: format, position, count)
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(Array(rows.enumerated()), id: \.element.stopId) { index, row in
                    Text(verbatim: row.label)
                        // Formatted here rather than through an interpolated key: the key carries a positional format,
                        // and an interpolated lookup falls back to reading the key itself aloud.
                        .accessibilityValue(Text(verbatim: place(index + 1, of: rows.count)))
                        .accessibilityActions {
                            if index > 0 {
                                Button {
                                    onReorder(rows.moved(index, -1))
                                } label: {
                                    Text("nearme.my_stops.move_up", bundle: .module)
                                }
                            }
                            if index < rows.count - 1 {
                                Button {
                                    onReorder(rows.moved(index, 1))
                                } label: {
                                    Text("nearme.my_stops.move_down", bundle: .module)
                                }
                            }
                            Button {
                                draft = row.label
                                renaming = row
                            } label: {
                                Text("nearme.my_stops.rename", bundle: .module)
                            }
                            Button(role: .destructive) {
                                onDelete(row.stopId)
                            } label: {
                                Text("nearme.my_stops.remove", bundle: .module)
                            }
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                onDelete(row.stopId)
                            } label: {
                                Text("nearme.my_stops.remove", bundle: .module)
                            }
                            Button {
                                draft = row.label
                                renaming = row
                            } label: {
                                Text("nearme.my_stops.rename", bundle: .module)
                            }
                        }
                }
            }
            .navigationTitle(Text("nearme.my_stops.edit_title", bundle: .module))
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Text("nearme.my_stops.done", bundle: .module)
                    }
                }
            }
            .alert(
                Text("nearme.my_stops.rename", bundle: .module),
                isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })
            ) {
                TextField(text: $draft) {
                    Text("nearme.my_stops.rename", bundle: .module)
                }
                Button {
                    if let row = renaming {
                        onRename(row.stopId, draft.isEmpty ? nil : String(draft.prefix(30)))
                    }
                    renaming = nil
                } label: {
                    Text("nearme.my_stops.done", bundle: .module)
                }
                Button(role: .cancel) {
                    renaming = nil
                } label: {
                    Text("nearme.my_stops.remove", bundle: .module)
                }
            }
        }
    }
}

/// The rows with the item at `index` moved by `offset`, never past either end, as ids for the shared repository.
extension Array where Element == SavedStopRowUi {
    func moved(_ index: Int, _ offset: Int) -> [KotlinInt] {
        guard !isEmpty else { return [] }
        let target = Swift.min(Swift.max(index + offset, 0), count - 1)
        guard target != index else { return map { KotlinInt(int: $0.stopId) } }
        var reordered = self
        let moved = reordered.remove(at: index)
        reordered.insert(moved, at: target)
        return reordered.map { KotlinInt(int: $0.stopId) }
    }
}
