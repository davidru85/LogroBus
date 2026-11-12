import Observation
import Shared

/// Near Me (LB-201). It orchestrates only: the shared use case and mapper build the state (ADR-0007). Location runs
/// while `observe()` does (FR-LOC-03).
@MainActor
@Observable
public final class NearMeViewModel {
    public private(set) var state = NearMeUiState(
        nearYou: NearYouUiUnknown.shared,
        // LB-208: "My stops" comes with the next slice, so the section starts empty here.
        myStops: MyStopsUi(rows: [], limitReached: false)
    )
    /// Bumped when location access must be read again. The view keys its `.task` on it, so a new value starts a new
    /// `observe()`.
    public private(set) var checks = 0
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap. The authorization's location manager is made on first use for the same reason.
    // The screen's graph, which its "My stops" section reads too, so both work over the same use cases and a preview or
    // a test replaces them once.
    let useCases: any UseCases
    @ObservationIgnored private let makeAuthorization: @MainActor () -> any LocationAuthorizing
    @ObservationIgnored private var madeAuthorization: (any LocationAuthorizing)?

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests, and
    /// the system's location authorization, or a fake one.
    public init(
        useCases: any UseCases = SharedDependencies.shared,
        authorization: @escaping @MainActor () -> any LocationAuthorizing = { CoreLocationAuthorization() }
    ) {
        self.useCases = useCases
        makeAuthorization = authorization
    }

    public func onEvent(_ event: any NearMeEvent) {
        switch onEnum(of: event) {
        case .permissionMayHaveChanged:
            // Only without location, as on Android: until access is known, the read under way gets the latest
            // permission, and while location is on, its flow ends by itself if location goes.
            if state.nearYou is NearYouUiTurnOnLocation { checks += 1 }
        }
    }

    /// The "Turn on location" row (FR-LOC-01): the system's prompt until the user has answered it. iOS asks once, so
    /// after a denial only Settings can turn location on.
    public func turnOnLocation() {
        switch authorization.status {
        case .notDetermined: authorization.requestWhenInUse()
        case .denied: authorization.openSettings()
        case .granted: onEvent(NearMeEventPermissionMayHaveChanged.shared)
        }
    }

    /// Runs while the screen shows and the app is active: the view calls it from `.task {}`, which cancels it when
    /// either ends, and the location updates stop with it.
    public func observe() async {
        // LB-203: one collection feeds the permission row and the list, because the provider starts the platform's
        // updates with each collection (FR-LOC-03).
        for await section in useCases.observeNearYou().invoke() {
            state = NearMeUiMapper.shared.state(access: section.access, nearby: section.nearby)
        }
    }

    /// Follows the system's authorization while the screen shows: an answer to the prompt, or a change in Settings.
    public func followAuthorization() async {
        for await _ in authorization.changes() {
            onEvent(NearMeEventPermissionMayHaveChanged.shared)
        }
    }

    private var authorization: any LocationAuthorizing {
        if let madeAuthorization { return madeAuthorization }
        let made = makeAuthorization()
        madeAuthorization = made
        return made
    }
}
