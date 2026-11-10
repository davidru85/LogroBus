import DesignSystem
import Shared
import SwiftUI

/// A line's detail (SC-04): the line and its favourite toggle (LB-103, FR-LINE-09), the direction toggle and the
/// selected direction's ordered stop list (LB-104, FR-LINE-04). The map of both shapes waits for LB-013. The title
/// names the line, and the toolbar holds the toggle, labelled as the rows' are.
public struct LineDetailView: View {
    @State private var viewModel: LineDetailViewModel

    public init(viewModel: LineDetailViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    /// The detail of line `lineId`, with the use cases from the graph.
    public init(lineId: Int32) {
        self.init(viewModel: LineDetailViewModel(lineId: lineId))
    }

    public var body: some View {
        content
            .navigationTitle(title)
            .toolbar {
                if let line {
                    ToolbarItem(placement: .topBarTrailing) {
                        FavouriteToggle(isFavourite: line.isFavourite, label: toggleLabel(line)) { isFavourite in
                            viewModel.onEvent(LineDetailEventSetFavourite(isFavourite: isFavourite))
                        }
                    }
                }
            }
            .favouriteLimitAlert(viewModel.favouriteLimit) {
                viewModel.onEvent(LineDetailEventFavouriteLimitShown.shared)
            }
            // Cancelled when the screen leaves, which stops the use case's collection; restarted by Retry.
            .task(id: viewModel.attempt) { await viewModel.observe() }
    }

    private var line: LineDetailUi? {
        (viewModel.state as? LineDetailUiStateContent)?.line
    }

    /// "Line 10"; "Line" until the line's code is known, so the screen is never untitled.
    private var title: Text {
        line.map { Text("line.title \($0.code)", bundle: .module) } ?? Text("line.title.generic", bundle: .module)
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: viewModel.state) {
        case .loading:
            // Spec §7.3: the final layout with a placeholder, never a lone spinner (design_guidelines.md §8).
            header(Self.placeholder)
                .redacted(reason: .placeholder)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text("line.loading", bundle: .module))
        case .content(let content):
            LineDetailBody(content: content) { event in viewModel.onEvent(event) }
        case .error(let error):
            ContentUnavailableView {
                error.reason.message
            } actions: {
                RetryButton {
                    viewModel.onEvent(LineDetailEventRetry.shared)
                }
            }
        }
    }

    /// The line's badge and its whole display name, which wraps. VoiceOver reads them as one (NFR-A11Y-02).
    private func header(_ line: LineDetailUi) -> some View {
        List {
            HStack(spacing: Tokens.Spacing.medium) {
                LineBadge(code: line.code, color: line.color, onColor: line.onColor, size: .large)
                // Names are shown as delivered, never translated (AGENTS.md §7).
                Text(verbatim: line.displayName)
                    .font(.title3.weight(.semibold))
            }
            .accessibilityElement(children: .combine)
        }
    }

    private func toggleLabel(_ line: LineDetailUi) -> Text {
        line.isFavourite
            ? Text("lines.favourite.remove \(line.code)", bundle: .module)
            : Text("lines.favourite.add \(line.code)", bundle: .module)
    }

    /// The redacted first load's line: shaped like one, never read.
    private static let placeholder = LineDetailUi(
        id: 0, code: "10", displayName: "Lardero - Hospital San Pedro", color: 0xFF9E_9E9E, onColor: 0xFFFF_FFFF,
        isFavourite: false
    )
}

/// The line's header, the direction toggle and the ordered stop list (LB-104).
private struct LineDetailBody: View {
    let content: LineDetailUiStateContent
    let onEvent: (any LineDetailEvent) -> Void

    var body: some View {
        List {
            Section {
                HStack(spacing: Tokens.Spacing.medium) {
                    LineBadge(
                        code: content.line.code, color: content.line.color, onColor: content.line.onColor, size: .large
                    )
                    // Names are shown as delivered, never translated (AGENTS.md §7).
                    Text(verbatim: content.line.displayName)
                        .font(.title3.weight(.semibold))
                }
                .accessibilityElement(children: .combine)
            }

            if !content.directions.isEmpty {
                Section {
                    // FR-LINE-04 and DD-16: each option read as "Towards <headsign>". A segmented picker is a
                    // selection control, so VoiceOver announces the options and which one is selected.
                    Picker(selection: selection) {
                        ForEach(content.directions, id: \.direction) { option in
                            Text("lines.towards \(option.headsign)", bundle: .module)
                                .tag(option.direction)
                        }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                    .accessibilityLabel(Text("line.direction", bundle: .module))
                }
            }

            Section {
                ForEach(Array(content.stops.enumerated()), id: \.element.stopId) { index, stop in
                    // "Stop 3, Varea": the position is what a rider counts, and the key's one argument is it.
                    LabeledContent {
                        Text(verbatim: stop.name)
                    } label: {
                        Text("line.stop.position \(index + 1)", bundle: .module)
                    }
                }
            }
        }
    }

    /// The picker writes through an event, so the state comes back from the ViewModel and the list always matches it.
    private var selection: Binding<Direction> {
        Binding(
            get: { content.direction },
            set: { onEvent(LineDetailEventSelectDirection(direction: $0)) }
        )
    }
}

#Preview("Line") {
    NavigationStack {
        LineDetailView(viewModel: LineDetailViewModel(lineId: 10, useCases: SharedDependencies.shared.preview()))
    }
}

#Preview("Favourite line, dark") {
    NavigationStack {
        LineDetailView(
            viewModel: LineDetailViewModel(
                lineId: 10, useCases: SharedDependencies.shared.preview().favouriteLine(id: 10)
            )
        )
        .preferredColorScheme(.dark)
    }
}

#Preview("Line, large text") {
    NavigationStack {
        LineDetailView(viewModel: LineDetailViewModel(lineId: 10, useCases: SharedDependencies.shared.preview()))
            .dynamicTypeSize(.accessibility3)
    }
}

#Preview("Loading") {
    NavigationStack {
        LineDetailView(
            viewModel: LineDetailViewModel(lineId: 10, useCases: SharedDependencies.shared.preview().loading())
        )
    }
}

#Preview("Loading, dark") {
    NavigationStack {
        LineDetailView(
            viewModel: LineDetailViewModel(lineId: 10, useCases: SharedDependencies.shared.preview().loading())
        )
        .preferredColorScheme(.dark)
    }
}

#Preview("No network") {
    NavigationStack {
        LineDetailView(
            viewModel: LineDetailViewModel(lineId: 10, useCases: SharedDependencies.shared.preview().offline())
        )
    }
}

#Preview("No network, dark") {
    NavigationStack {
        LineDetailView(
            viewModel: LineDetailViewModel(lineId: 10, useCases: SharedDependencies.shared.preview().offline())
        )
        .preferredColorScheme(.dark)
    }
}
