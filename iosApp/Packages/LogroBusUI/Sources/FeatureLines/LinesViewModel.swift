import Observation
// Kotlin types declare no Sendable conformance, so Swift 6 refuses to send a use case to the async function SKIE makes
// of a `suspend` one. Kotlin/Native's memory model lets any thread call them (ADR-0004), so Shared's types are
// imported as pre-concurrency here.
@preconcurrency import Shared

/// The Lines tab (LB-101, LB-103, LB-102). It orchestrates only: the shared use cases and mapper build the state
/// (ADR-0007).
@MainActor
@Observable
public final class LinesViewModel {
    public private(set) var state: any LinesUiState = LinesUiStateLoading.shared
    /// Bumped by Retry. The view keys its `.task` on it, so a new value starts a new `observe()`, and with it a new
    /// sync (ADR-0008).
    public private(set) var attempt = 0
    /// The search field's text (LB-102). It filters the lines already read, so a key never starts a load.
    public private(set) var query = ""
    public private(set) var favouriteLimit: Int?
    // The use case's last result, which a new query maps again; none while a load has not answered yet.
    private var lines: AppResult<LinesWithFavourites>?
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases
    // The last toggle's write. Each waits for the one before: SKIE may start two Kotlin calls in either order, so two
    // quick taps could otherwise land the wrong way round.
    private var favouriteWrite: Task<Void, Never>?

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(useCases: any UseCases = SharedDependencies.shared) {
        self.useCases = useCases
    }

    public func onEvent(_ event: any LinesEvent) {
        switch onEnum(of: event) {
        case .retry:
            lines = nil
            state = LinesUiStateLoading.shared
            attempt += 1
        case .search(let search):
            query = search.query
            show()
        case .setFavourite(let favourite):
            // The carousel and the toggle change through the use case's flow (FR-LINE-15). A save that fails, a
            // damaged database the repository has logged, leaves them as they were.
            let previous = favouriteWrite
            favouriteWrite = Task { [useCases] in
                await previous?.value
                let toggle = try? await useCases.setFavouriteLine().invoke(
                    lineId: favourite.lineId, isFavourite: favourite.isFavourite
                )
                if let limit = Self.limit(reachedBy: toggle) { self.favouriteLimit = limit }
            }
        case .reorderFavourites(let reorder):
            Task { [useCases] in _ = try? await useCases.reorderFavouriteLines().invoke(lineIds: reorder.lineIds) }
        case .favouriteLimitShown:
            favouriteLimit = nil
        }
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves.
    public func observe() async {
        for await result in useCases.observeLinesWithFavourites().invoke() {
            lines = result
            show()
        }
    }

    /// The limit a toggle reached, if it did (FR-FAV-05).
    nonisolated static func limit(reachedBy toggle: AppResult<FavouriteToggle>?) -> Int? {
        guard let saved = toggle as? AppResultSuccess<FavouriteToggle>,
              let reached = saved.value as? FavouriteToggleLimitReached else { return nil }
        return Int(reached.limit)
    }

    private func show() {
        guard let lines else { return }
        state = LinesUiMapper.shared.state(result: lines, query: query)
    }
}
