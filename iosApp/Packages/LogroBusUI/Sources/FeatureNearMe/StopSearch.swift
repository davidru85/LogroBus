import DesignSystem
import Observation
// Kotlin's types declare no Sendable conformance, so a `suspend` use case is sent from the main actor under the
// pre-concurrency rule the other ViewModels use (ADR-0004).
@preconcurrency import Shared
import SwiftUI

/// LB-209 (FR-NEAR-05, FR-STOP-01, FR-STOP-02): the stop search, over the catalogue the app already holds.
///
/// It asks once per change of the query, and drops an answer that a later query has already replaced, so the list never
/// shows the results of what the user has stopped typing. Each row carries the lines that reach its stop, which is how
/// two stops with one name are told apart (DD-16).
@MainActor
@Observable
public final class StopSearchViewModel {
    /// What the user typed.
    public var query = ""
    /// The stops the last answer found, in the order the source ranked them.
    public private(set) var results: [StopSearchResultUi] = []
    /// Whether a search is in flight, for the progress indicator that keeps the rows interactive.
    public private(set) var isSearching = false

    private let useCases: any UseCases

    /// The answer this ViewModel is waiting for. A slower answer to an older query must not replace a newer one.
    private var generation = 0

    public init(useCases: any UseCases = SharedDependencies.shared) {
        self.useCases = useCases
    }

    /// Asks for `query`, or clears the list when it is blank. Cancelled by the view when the screen leaves.
    public func search() async {
        let asked = query
        generation += 1
        let mine = generation
        guard !asked.trimmingCharacters(in: .whitespaces).isEmpty else {
            results = []
            isSearching = false
            return
        }
        isSearching = true
        let found = (try? await useCases.searchStops().invoke(query: asked)) ?? []
        // A later query has answered already, so this one is stale and must not land.
        guard mine == generation else { return }
        results = found
        isSearching = false
    }
}

/// The search field and its results, above the saved and nearby stops.
public struct StopSearchSection: View {
    /// The screen's own ViewModel: `.searchable` belongs to the screen, which binds this one's query, so both read the
    /// same state (a sheet, a list's section and the field cannot each own a copy).
    let viewModel: StopSearchViewModel
    private let onOpenStop: (Int32) -> Void

    public init(viewModel: StopSearchViewModel, onOpenStop: @escaping (Int32) -> Void) {
        self.viewModel = viewModel
        self.onOpenStop = onOpenStop
    }

    public var body: some View {
        Section {
            ForEach(viewModel.results, id: \.stop.id) { result in
                Button {
                    onOpenStop(result.stop.id)
                } label: {
                    VStack(alignment: .leading, spacing: Tokens.Spacing.xSmall) {
                        // Names are shown as delivered, never translated (AGENTS.md §7).
                        Text(verbatim: result.stop.name)
                            .foregroundStyle(Tokens.Colors.onSurface)
                        // DD-16: the headsign and the badge tell apart the two stops that share a name.
                        if let line = result.lines.first {
                            HStack(spacing: Tokens.Spacing.small) {
                                LineBadge(code: line.code, color: line.color, onColor: line.onColor)
                                if let headsign = line.headsign {
                                    Text("nearme.search.towards \(headsign)", bundle: .module)
                                        .foregroundStyle(Tokens.Colors.secondaryText)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(accessibilityLabel(of: result))
                .accessibilityAddTraits(.isButton)
            }
        } footer: {
            // FR-NEAR-05: a query that finds nothing says so rather than showing an empty list.
            if !viewModel.query.isEmpty, viewModel.results.isEmpty, !viewModel.isSearching {
                Text("nearme.search.no_matches", bundle: .module)
                    .foregroundStyle(Tokens.Colors.secondaryText)
            }
        }
    }

    /// One utterance: the stop, then its first line and where that line heads.
    private func accessibilityLabel(of result: StopSearchResultUi) -> Text {
        guard let line = result.lines.first, let headsign = line.headsign else {
            return Text(verbatim: result.stop.name)
        }
        let towards = String(localized: "nearme.search.towards \(headsign)", bundle: .module)
        return Text(verbatim: "\(result.stop.name), \(line.code), \(towards)")
    }
}
