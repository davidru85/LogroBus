import Observation
import Shared

/// About data (LB-404). It orchestrates only: the shared use case and mapper build the state (ADR-0007).
@MainActor
@Observable
public final class AboutDataViewModel {
    public private(set) var state: any AboutDataUiState = AboutDataUiStateLoading.shared
    /// Bumped by Retry. The view keys its `.task` on it, so a new value starts a new `observe()`.
    public private(set) var attempt = 0
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(useCases: any UseCases = SharedDependencies.shared) {
        self.useCases = useCases
    }

    public func onEvent(_ event: any AboutDataEvent) {
        switch onEnum(of: event) {
        case .retry:
            state = AboutDataUiStateLoading.shared
            attempt += 1
        }
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves.
    public func observe() async {
        for await result in useCases.observeLastSyncs().invoke() {
            state = AboutDataUiMapper.shared.state(result: result)
        }
    }
}
