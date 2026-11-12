import DesignSystem
import Shared
import SwiftUI

/// A stop's detail (SC-05, LB-205): its header, the badges and headsigns of the lines that serve it, its arrivals
/// through
/// the design system's `EtaText`, and how old the list is (FR-STOP-03 to FR-STOP-05). The same screen serves both doors
/// (FR-NAV-07): pushed from line detail, or in Near Me's sheet (LB-203). Line filter and grouping are LB-207's.
public struct StopDetailView: View {
    @State private var viewModel: StopDetailViewModel
    /// The alias the save alert holds while it shows.
    @State private var draft = ""
    @State private var askingAlias = false

    /// The alias field's label: the key, with the share of characters it takes as its argument.
    private var aliasPrompt: String {
        String(
            format: String(localized: "stop.save.alias", bundle: .module),
            Int(SavedStopAlias.shared.MAX_LENGTH)
        )
    }

    public init(viewModel: StopDetailViewModel) {
        _viewModel = State(initialValue: viewModel)
    }

    /// The detail of stop `stopId`, with the use cases from the graph.
    public init(stopId: Int32) {
        self.init(viewModel: StopDetailViewModel(stopId: stopId))
    }

    public var body: some View {
        content
            .navigationTitle(title)
            // LB-208 (FR-STOP-08): the Save star, once the saved stops are known, so it never shows the wrong state.
            // Filled means saved; the label carries the alias the user gave it. Saving asks for the alias first, as
            // Android's Save action does, so a stop can be labelled "Casa" at the moment it is saved.
            .toolbar {
                if let saved = viewModel.saved {
                    ToolbarItem(placement: .primaryAction) {
                        Button {
                            if saved.isSaved {
                                Task { await viewModel.remove() }
                            } else {
                                draft = saved.alias ?? ""
                                askingAlias = true
                            }
                        } label: {
                            Label {
                                Text(verbatim: saved.alias ?? "")
                            } icon: {
                                Image(systemName: saved.isSaved ? "star.fill" : "star")
                            }
                        }
                        .accessibilityLabel(
                            Text(
                                saved.isSaved ? "stop.save.saved" : "stop.save.add",
                                bundle: .module
                            )
                        )
                        .accessibilityValue(Text(verbatim: saved.alias ?? ""))
                        .frame(minWidth: Tokens.minTouchTarget, minHeight: Tokens.minTouchTarget)
                    }
                }
            }
            .stopSaveLimitAlert(viewModel.limitReached ? Int(FAVOURITES_PER_TYPE) : nil) {
                viewModel.clearLimit()
            }
            // The alias, cut at thirty characters as it is typed, as Android's dialog is: nothing is cut behind the
            // user's back, and a blank field saves the stop under its own name.
            .alert(Text("stop.save.title", bundle: .module), isPresented: $askingAlias) {
                TextField(text: $draft) {
                    // The key carries the limit as a positional format, looked up verbatim: an interpolated key is a
                    // different key, and the localisation test reads this one.
                    Text(aliasPrompt)
                }
                Button {
                    let alias = draft.isEmpty ? nil : String(draft.prefix(Int(SavedStopAlias.shared.MAX_LENGTH)))
                    Task { await viewModel.save(alias: alias) }
                } label: {
                    Text("stop.save.confirm", bundle: .module)
                }
                Button(role: .cancel) {} label: {
                    Text("stop.save.cancel", bundle: .module)
                }
            }
            // Cancelled when the screen leaves, which stops API-06's polling; restarted by Retry.
            .task(id: viewModel.attempt) { await viewModel.observe() }
    }

    private var stop: Stop? {
        (viewModel.state as? StopDetailUiStateContent)?.stop
    }

    /// The stop's name, shown as delivered and never translated (AGENTS.md §7); "Stop" until it is known, so the
    /// is never untitled.
    private var title: Text {
        stop.map { Text(verbatim: $0.name) } ?? Text("stop.title.generic", bundle: .module)
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: viewModel.state) {
        case .loading:
            // Spec §7.3: the final layout with a placeholder, never a lone spinner (design_guidelines.md §8).
            List {
                Section {
                    Text("stop.title.generic", bundle: .module)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Tokens.Colors.onSurface)
                }
                Section {
                    Text("stop.served_lines", bundle: .module)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                }
                Section {
                    Text("stop.arrivals", bundle: .module)
                        .foregroundStyle(Tokens.Colors.secondaryText)
                }
            }
            .redacted(reason: .placeholder)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("stop.loading", bundle: .module))
        case .content(let content):
            StopDetailBody(content: content) { event in
                viewModel.onEvent(event)
            }
        case .error(let error):
            ContentUnavailableView {
                error.reason.message
            } actions: {
                RetryButton {
                    viewModel.onEvent(.retry)
                }
            }
        }
    }
}

/// The stop's header, the lines that serve it and the arrival list (FR-STOP-03 to FR-STOP-05).
private struct StopDetailBody: View {
    let content: StopDetailUiStateContent
    let onEvent: (StopDetailEvent) -> Void

    var body: some View {
        List {
            Section {
                header
            }

            Section {
                if servedLines.isEmpty {
                    // FR-STOP-03: a stop no line reaches keeps none, which the screen says rather than hides.
                    Text("stop.no_lines", bundle: .module)
                        .foregroundStyle(Tokens.Colors.onSurface)
                } else {
                    ForEach(servedLines, id: \.lineId) { line in
                        ServedLineRow(line: line)
                    }
                }
            } header: {
                // design_guidelines.md §3.1: a section heading is the tokens' secondary text, never the system's grey,
                // which the accessibility audit reads as nearly short of 4.5:1.
                Text("stop.served_lines", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            }

            Section {
                ForEach(Array(content.arrivals.enumerated()), id: \.offset) { _, arrival in
                    ArrivalRow(arrival: arrival)
                }
            } header: {
                Text("stop.arrivals", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            } footer: {
                FreshnessFooter(updated: content.updated) { onEvent(.retry) }
            }
        }
    }

    private var servedLines: [ServedLine] {
        content.stop.servedLines
    }

    /// FR-STOP-03: the stop's name, which wraps, over the badges of the lines that serve it.
    private var header: some View {
        VStack(alignment: .leading, spacing: Tokens.Spacing.small) {
            // Names are shown as delivered, never translated (AGENTS.md §7).
            Text(verbatim: content.stop.name)
                .font(.title3.weight(.semibold))
                .foregroundStyle(Tokens.Colors.onSurface)
            if !servedLines.isEmpty {
                HStack(spacing: Tokens.Spacing.xSmall) {
                    ForEach(servedLines, id: \.lineId) { line in
                        LineBadge(code: line.code, color: line.color, onColor: line.onColor)
                    }
                }
            }
        }
        // Not one combined element (NFR-A11Y-02): the audit reads a merged node's contrast as nearly short when it
        // spans primary and secondary text. Each badge carries its own "Line N" label.
        .accessibilityElement(children: .contain)
    }
}

/// A line that calls at the stop, with the headsign of the pattern that reaches it (FR-STOP-03, DD-16).
private struct ServedLineRow: View {
    let line: ServedLine

    var body: some View {
        HStack(spacing: Tokens.Spacing.medium) {
            LineBadge(code: line.code, color: line.color, onColor: line.onColor, size: .small)
            if let headsign = line.headsign {
                Text("stop.towards \(headsign)", bundle: .module)
                    .foregroundStyle(Tokens.Colors.onSurface)
            }
        }
    }
}

/// One arrival (FR-STOP-04, spec §5.7): its line's badge, the headsign of its own journey (DD-16), and the ETA through
/// the design system's `EtaText`, which strikes a cancelled row through under "Cancelled" and marks DD-09's "~".
private struct ArrivalRow: View {
    let arrival: ServedArrivalUi
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        // design_guidelines.md §3.1: beside each other while the text fits, one above the other at accessibility
        // where a fixed row would squeeze the headsign to a word a line. The ETA keeps the trailing edge in both.
        Group {
            if typeSize.isAccessibilitySize {
                VStack(alignment: .trailing, spacing: Tokens.Spacing.xSmall) {
                    leading.frame(maxWidth: .infinity, alignment: .leading)
                    eta
                }
            } else {
                HStack(spacing: Tokens.Spacing.medium) {
                    leading
                    eta
                }
            }
        }
    }

    private var leading: some View {
        HStack(spacing: Tokens.Spacing.small) {
            LineBadge(code: arrival.lineCode, color: arrival.color, onColor: arrival.onColor)
            if let headsign = arrival.headsign {
                // NFR-A11Y-01: the row's words take the primary text colour, never the system's secondary one.
                Text(verbatim: headsign)
                    .foregroundStyle(Tokens.Colors.onSurface)
            }
        }
    }

    @ViewBuilder private var eta: some View {
        if let display = arrival.eta {
            EtaText(
                eta: EtaUi(
                    display: display,
                    lowConfidence: arrival.isLowConfidence,
                    scheduled: false,
                    delay: nil,
                    cancelled: arrival.isCancelled
                )
            )
        } else {
            // NFR-A11Y-01: the dash is the row's content, so it takes the primary text colour: the audit reads a lone
            // glyph in the secondary one as nearly short of 4.5:1.
            Text("stop.eta.none", bundle: .module)
                .font(Tokens.Typography.etaValue)
                .foregroundStyle(Tokens.Colors.onSurface)
                .accessibilityLabel(Text("stop.eta.none_spoken", bundle: .module))
        }
    }
}

/// FR-STOP-05 (design_guidelines.md §7): "Updated N s ago", in the warning colour from 50 s, and past 120 s the
/// ETAs are clock times and the footer offers "Tap to refresh" (spec §5.7).
private struct FreshnessFooter: View {
    let updated: StopUpdatedUi?
    let onRefresh: () -> Void

    var body: some View {
        if let updated {
            Text("freshness.updated_ago \(Int(updated.ageSeconds))", bundle: .module)
                // NFR-A11Y-01: the warning colour reaches 4.5:1 on every surface (design_guidelines.md §6.1), and the
                // secondary one reads as muted once the list is fresh.
                .foregroundStyle(updated.isStale ? Tokens.Colors.warning : Tokens.Colors.secondaryText)
            if updated.needsRefresh {
                Button(action: onRefresh) {
                    Text("freshness.tap_to_refresh", bundle: .module)
                }
                .buttonStyle(.borderless)
                .foregroundStyle(Tokens.Colors.warning)
                // NFR-A11Y-04: a full touch target rather than a line of small text.
                .frame(minHeight: Tokens.minTouchTarget)
            }
        }
    }
}

#Preview("Stop") {
    NavigationStack {
        StopDetailView(stopId: 38)
    }
}
