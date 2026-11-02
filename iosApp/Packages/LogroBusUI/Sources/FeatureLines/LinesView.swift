import DesignSystem
import Shared
import SwiftUI

/// The root of the Lines tab (LB-101): day lines, then night (Búho) lines, each sorted by code (FR-LINE-01). The app
/// shell adds the More action.
public struct LinesView: View {
    @State private var viewModel: LinesViewModel
    @Environment(\.scrollToTopRequests) private var scrollToTopRequests
    @State private var scrollPosition = ScrollPosition(edge: .top)

    public init(viewModel: LinesViewModel = LinesViewModel()) {
        _viewModel = State(initialValue: viewModel)
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
            lineList(Self.placeholders)
                .redacted(reason: .placeholder)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Text("lines.loading", bundle: .module))
        case .content(let content):
            lineList(content.groups)
        case .empty:
            message(Text("lines.empty", bundle: .module))
        case .error(let error):
            message(error.reason.message)
        }
    }

    /// FR-LINE-01: each group under its heading. Re-selecting the tab at this root scrolls it to the top, its first
    /// heading included (FR-NAV-03).
    private func lineList(_ groups: [LineGroupUi]) -> some View {
        List {
            ForEach(groups, id: \.kind) { group in
                Section {
                    ForEach(group.lines, id: \.id) { LineRow(line: $0) }
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
                    color: 0xFF9E_9E9E, onColor: 0xFFFF_FFFF, isOperational: true
                )
            }
        )
    ]
}

/// FR-LINE-02: the badge, the display name, which wraps rather than clips, and the status chip of a line that is not
/// operational. VoiceOver reads the row as one: the line, its name, then its status (NFR-A11Y-02).
private struct LineRow: View {
    let line: LineRowUi

    var body: some View {
        HStack(spacing: Tokens.Spacing.medium) {
            LineBadge(code: line.code, color: line.color, onColor: line.onColor)
            VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall) {
                // Names are shown as delivered, never translated (AGENTS.md §7).
                Text(verbatim: line.displayName)
                if !line.isOperational {
                    StatusChip()
                }
            }
        }
        .accessibilityElement(children: .combine)
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
