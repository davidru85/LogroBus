import Observation
import Shared

/// About data (LB-404). It orchestrates only: the shared use case and mapper build the state (ADR-0007).
@MainActor
@Observable
public final class AboutDataViewModel {
    public private(set) var state: any AboutDataUiState = AboutDataUiStateLoading.shared
    private let observeLastSyncs: any ObserveLastSyncs

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(useCases: any UseCases = SharedDependencies.shared) {
        observeLastSyncs = useCases.observeLastSyncs()
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves.
    public func observe() async {
        for await syncs in observeLastSyncs.invoke() {
            state = AboutDataUiMapper.shared.content(syncs: syncs)
        }
    }
}
