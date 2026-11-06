import Observation
import Shared

/// How to Get There's root (LB-301). Stubbed until the green phase.
@MainActor
@Observable
public final class PlannerViewModel {
    public private(set) var state = PlannerUiState(origin: PlaceFieldUiEmpty.shared, destination: PlaceFieldUiEmpty.shared)
    private let useCases: any UseCases

    public init(useCases: any UseCases = SharedDependencies.shared) {
        self.useCases = useCases
    }

    public func onEvent(_: any PlannerEvent) {}
}
