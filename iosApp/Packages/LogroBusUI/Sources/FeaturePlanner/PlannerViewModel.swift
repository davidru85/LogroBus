import Observation
import Shared

/// How to Get There's root (LB-301). It orchestrates only: the shared use case and mapper build the state (ADR-0007).
/// The initial origin is read each time the root shows (FR-PLAN-11).
@MainActor
@Observable
public final class PlannerViewModel {
    public private(set) var state = PlannerUiMapper.shared.state(origin: nil)
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(useCases: any UseCases = SharedDependencies.shared) {
        self.useCases = useCases
    }

    public func onEvent(_ event: any PlannerEvent) {
        switch onEnum(of: event) {
        case .shown:
            // It reads the permission only: nothing asks for it here, and no location updates start.
            state = PlannerUiMapper.shared.state(origin: useCases.readInitialOrigin().invoke())
        case .swap:
            break
        }
    }
}
