@testable import FeaturePlanner
import Shared
import Testing

/// FR-PLAN-11 on iOS: How to Get There starts from "My location" with the location permission and with both fields
/// empty without it, and the origin follows the permission each time the root shows.
@MainActor
struct PlannerViewModelTests {
    @Test
    func lb301_withLocation_theOriginIsMyLocation_andTheDestinationIsEmpty() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())

        viewModel.onEvent(PlannerEventShown.shared)

        #expect(viewModel.state.origin is PlaceFieldUiMyLocation)
        #expect(viewModel.state.destination is PlaceFieldUiEmpty)
    }

    @Test
    func lb301_withoutLocation_bothFieldsAreEmpty() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview())

        viewModel.onEvent(PlannerEventShown.shared)

        #expect(viewModel.state.origin is PlaceFieldUiEmpty)
        #expect(viewModel.state.destination is PlaceFieldUiEmpty)
    }

    @Test
    func lb301_whenTheRootShowsAgain_theOriginFollowsThePermission() {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = PlannerViewModel(useCases: useCases)
        viewModel.onEvent(PlannerEventShown.shared)
        #expect(viewModel.state.origin is PlaceFieldUiEmpty)

        // Granted in Near Me, or in Settings while the app was in the background.
        useCases.locationOn()
        viewModel.onEvent(PlannerEventShown.shared)
        #expect(viewModel.state.origin is PlaceFieldUiMyLocation)

        useCases.locationOff()
        viewModel.onEvent(PlannerEventShown.shared)
        #expect(viewModel.state.origin is PlaceFieldUiEmpty)
    }
}
