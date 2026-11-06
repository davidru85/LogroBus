import Observation
import Shared

/// How to Get There's root (LB-301). It orchestrates only: the shared use case, `PlanPlaces` and the mapper build the
/// state (ADR-0007). Each time the root shows, "My location" follows the permission (FR-PLAN-11), and the swap button
/// exchanges the trip's ends (FR-PLAN-08).
@MainActor
@Observable
public final class PlannerViewModel {
    /// The shared mapper's state of the trip's ends, observed through them.
    public var state: PlannerUiState { PlannerUiMapper.shared.state(places: places) }
    // The trip's ends as they stand: none until the root first shows.
    private var places = PlanPlaces(origin: nil, destination: nil)
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
            places = places.following(initialOrigin: useCases.readInitialOrigin().invoke())
        case .swap:
            places = places.swapped()
        case .leaveNow, .chooseDepartureTime, .cancelDepartureTime, .departAt:
            break
        }
    }
}
