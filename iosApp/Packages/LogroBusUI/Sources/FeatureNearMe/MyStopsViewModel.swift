import Observation
// Kotlin types declare no Sendable conformance, so Swift 6 refuses to send a use case to the async function SKIE makes
// of a `suspend` one. Kotlin/Native's memory model lets any thread call them (ADR-0004), so Shared's types are
// imported as pre-concurrency here, as the stop detail's ViewModel does.
@preconcurrency import Shared

/// LB-208 part 4 (FR-HOME-02, FR-FAV-02): "My stops" in Near Me, and its edit sheet.
///
/// It orchestrates only: the shared `ObserveMyStops` joins the saved stops with the catalogue's names, and
/// `SavedStopsUiMapper` decides what each row says, so this screen and Android's never disagree.
@MainActor
@Observable
public final class MyStopsViewModel {
    /// The rows, in the user's order, each under its alias or the catalogue's name.
    public private(set) var rows: [SavedStopRowUi] = []

    // Kept rather than resolved here, as the stop detail's ViewModel does: SwiftUI may build a view's ViewModel on every
    // parent update and keep only the first, so the init stays cheap — and, more than cheap, it must not reach the graph
    // at all, because a preview or a test builds the section without a saved-stops binding.
    private let useCases: any UseCases
    private let injected: (any SavedStopRepository)?

    public init(useCases: any UseCases = SharedDependencies.shared, saved: (any SavedStopRepository)? = nil) {
        self.useCases = useCases
        injected = saved
    }

    /// The saved stops, resolved on first write rather than in the initialiser.
    private var saved: any SavedStopRepository { injected ?? useCases.savedStops() }

    /// Runs while the section shows: the rows follow the saved stops, so a save made elsewhere appears at once.
    public func observe() async {
        for await result in useCases.observeMyStops().invoke() {
            switch onEnum(of: result) {
            case .failure:
                rows = []
            case .success(let success):
                guard let stops = success.value else { continue }
                rows = SavedStopsUiMapper.shared.myStops(
                    saved: stops.stops,
                    names: stops.names,
                    limitReached: false
                ).rows
            }
        }
    }

    /// FR-FAV-02: the user's new order, top to bottom.
    public func reorder(_ stopIds: [KotlinInt]) async {
        _ = try? await saved.reorder(stopIds: stopIds)
    }

    /// FR-STOP-08: a new alias for a saved stop.
    public func rename(stopId: Int32, alias: String?) async {
        _ = try? await saved.rename(stopId: stopId, alias: alias)
    }

    /// FR-STOP-08: removes a saved stop, including one the catalogue no longer holds.
    public func delete(stopId: Int32) async {
        _ = try? await saved.remove(stopId: stopId)
    }
}
