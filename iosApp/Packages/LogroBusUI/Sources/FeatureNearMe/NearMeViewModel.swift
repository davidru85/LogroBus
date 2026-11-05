import Observation
import Shared

/// Near Me (LB-201). It orchestrates only: the shared use case and mapper build the state (ADR-0007).
@MainActor
@Observable
public final class NearMeViewModel {
    public private(set) var state = NearMeUiState(nearYou: NearYouUiUnknown.shared)
    /// Bumped when location access must be read again. The view keys its `.task` on it.
    public private(set) var checks = 0
    private let useCases: any UseCases

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests, and
    /// the system's location authorization, or a fake one.
    public init(
        useCases: any UseCases = SharedDependencies.shared,
        authorization _: @escaping @MainActor () -> any LocationAuthorizing = { CoreLocationAuthorization() }
    ) {
        self.useCases = useCases
    }

    public func onEvent(_: any NearMeEvent) {}

    /// The "Turn on location" row (FR-LOC-01).
    public func turnOnLocation() {}

    /// Runs while the screen shows and the app is active.
    public func observe() async {}

    /// Follows the system's authorization while the screen shows.
    public func followAuthorization() async {}
}
