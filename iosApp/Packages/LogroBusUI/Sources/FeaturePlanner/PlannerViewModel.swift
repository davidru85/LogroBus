import Observation
import Shared

/// How to Get There's root (LB-301). It orchestrates only: the shared use cases, `PlanPlaces`, `PlanDeparture` and the
/// mapper build the state (ADR-0007). Each time the root shows, "My location" follows the permission (FR-PLAN-11), and
/// the swap button exchanges the trip's ends (FR-PLAN-08). The departure is "Leave now" or a picked time (FR-PLAN-03),
/// on the app clock.
@MainActor
@Observable
public final class PlannerViewModel {
    /// The shared mapper's state of the trip, observed through it, on the app clock.
    public var state: PlannerUiState {
        PlannerUiMapper.shared.state(
            places: places, departure: departure, picking: picking, now: useCases.clock().now()
        )
    }
    // The trip as it stands, its ends none until the root first shows, and whether the date and time pickers are open.
    private var places = PlanPlaces(origin: nil, destination: nil)
    private var departure: any PlanDeparture = PlanDepartureNow.shared
    private var picking = false
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
        case .leaveNow:
            departure = PlanDepartureNow.shared
        case .chooseDepartureTime:
            picking = true
        case .cancelDepartureTime:
            picking = false
        case .departAt(let time):
            departure = PlanDepartureCompanion.shared.atMadrid(
                year: time.year, month: time.month, day: time.day, hour: time.hour, minute: time.minute,
                now: useCases.clock().now()
            )
            picking = false
        }
    }
}
