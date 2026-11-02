import Observation
import Shared

/// The Lines list (LB-101). It orchestrates only: the shared use case and mapper build the state (ADR-0007).
@MainActor
@Observable
public final class LinesViewModel {
    public private(set) var state: any LinesUiState = LinesUiStateLoading.shared
    /// Bumped by Retry. The view keys its `.task` on it, so a new value starts a new `observe()`, and with it a new
    /// sync (ADR-0008).
    public private(set) var attempt = 0
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(useCases: any UseCases = SharedDependencies.shared) {
        self.useCases = useCases
    }

    public func onEvent(_ event: any LinesEvent) {
        switch onEnum(of: event) {
        case .retry:
            state = LinesUiStateLoading.shared
            attempt += 1
        case .setFavourite:
            // No row has a toggle yet.
            break
        }
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves.
    public func observe() async {
        for await result in useCases.observeLinesWithFavourites().invoke() {
            state = LinesUiMapper.shared.state(result: result)
        }
    }
}
