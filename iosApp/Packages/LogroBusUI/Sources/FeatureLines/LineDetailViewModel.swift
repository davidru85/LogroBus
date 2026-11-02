import Observation
// Kotlin types declare no Sendable conformance, so Swift 6 refuses to send a use case to the async function SKIE makes
// of a `suspend` one. Kotlin/Native's memory model lets any thread call them (ADR-0004), so Shared's types are
// imported as pre-concurrency here.
@preconcurrency import Shared

/// The detail of one line (LB-103; LB-104 adds its map, directions and stops). It orchestrates only: the shared use
/// cases and mapper build the state (ADR-0007).
@MainActor
@Observable
public final class LineDetailViewModel {
    public private(set) var state: any LineDetailUiState = LineDetailUiStateLoading.shared
    /// Bumped by Retry. The view keys its `.task` on it, so a new value starts a new `observe()`.
    public private(set) var attempt = 0
    private let lineId: Int32
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(lineId: Int32, useCases: any UseCases = SharedDependencies.shared) {
        self.lineId = lineId
        self.useCases = useCases
    }

    public func onEvent(_ event: any LineDetailEvent) {
        switch onEnum(of: event) {
        case .retry:
            state = LineDetailUiStateLoading.shared
            attempt += 1
        case .setFavourite(let favourite):
            // FR-LINE-09: the toggle changes through the use case's flow.
            Task { [lineId] in
                _ = try? await useCases.setFavouriteLine().invoke(lineId: lineId, isFavourite: favourite.isFavourite)
            }
        }
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves.
    public func observe() async {
        for await result in useCases.observeLinesWithFavourites().invoke() {
            state = LineDetailUiMapper.shared.state(result: result, lineId: lineId)
        }
    }
}
