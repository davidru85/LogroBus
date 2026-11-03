import DesignSystem
import Shared
import SwiftUI

/// A line's detail (SC-04): the line and its favourite toggle (LB-103, FR-LINE-09), until LB-104 adds its map,
/// direction toggle and stops. The title names the line, and the toolbar holds the toggle, labelled as the rows' are.
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
            header(content.line)
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
