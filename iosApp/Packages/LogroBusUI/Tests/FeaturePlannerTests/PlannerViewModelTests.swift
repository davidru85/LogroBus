@testable import FeaturePlanner
import Observation
import Shared
import Testing

/// FR-PLAN-11 on iOS: How to Get There starts from "My location" with the location permission and with both fields
/// empty without it, and the origin follows the permission each time the root shows. FR-PLAN-08: one swap exchanges
/// the fields, and the root showing again keeps it. FR-PLAN-03: the departure time, on the app clock, which stands at
/// 10:30 on Wednesday 28 Oct 2026 in Madrid (CET) in the preview.
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

    @Test
    func lb304_oneSwap_exchangesTheFields_andASecondRestoresThem() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
        viewModel.onEvent(PlannerEventShown.shared)

        viewModel.onEvent(PlannerEventSwap.shared)
        #expect(viewModel.state.origin is PlaceFieldUiEmpty)
        #expect(viewModel.state.destination is PlaceFieldUiMyLocation)

        viewModel.onEvent(PlannerEventSwap.shared)
        #expect(viewModel.state.origin is PlaceFieldUiMyLocation)
        #expect(viewModel.state.destination is PlaceFieldUiEmpty)
    }

    @Test
    func lb304_afterASwap_theRootShowingAgain_keepsIt() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview().locationOn())
        viewModel.onEvent(PlannerEventShown.shared)
        viewModel.onEvent(PlannerEventSwap.shared)

        // Back from another tab, or from the background, with the permission still granted.
        viewModel.onEvent(PlannerEventShown.shared)

        #expect(viewModel.state.origin is PlaceFieldUiEmpty)
        #expect(viewModel.state.destination is PlaceFieldUiMyLocation)
    }

    private let eightNineteenToday = DepartureUiAt(
        time: MadridDateTime(year: 2026, month: 10, day: 28, hour: 8, minute: 19), relativeDay: .today
    )

    @Test
    func lb303_theRootStarts_leavingNow_withThePickersClosed() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview())

        viewModel.onEvent(PlannerEventShown.shared)

        #expect(viewModel.state.departure is DepartureUiNow)
        #expect(viewModel.state.departurePicker == nil)
    }

    @Test
    func lb303_choosingATime_opensThePickersAtNow_onTheAppClock() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview())

        viewModel.onEvent(PlannerEventChooseDepartureTime.shared)

        let now = MadridDateTime(year: 2026, month: 10, day: 28, hour: 10, minute: 30)
        #expect(viewModel.state.departurePicker == DeparturePickerUi(start: now, now: now))
    }

    @Test
    func lb303_aPickedTime_isTheDeparture_andThePickersClose() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview())
        viewModel.onEvent(PlannerEventChooseDepartureTime.shared)

        viewModel.onEvent(PlannerEventDepartAt(year: 2026, month: 10, day: 29, hour: 8, minute: 19))

        let tomorrow = DepartureUiAt(
            time: MadridDateTime(year: 2026, month: 10, day: 29, hour: 8, minute: 19), relativeDay: .tomorrow
        )
        #expect(viewModel.state.departure as? DepartureUiAt == tomorrow)
        #expect(viewModel.state.departurePicker == nil)
    }

    @Test
    func lb303_cancelledPickers_closeAndKeepTheDeparture() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview())
        viewModel.onEvent(PlannerEventDepartAt(year: 2026, month: 10, day: 28, hour: 8, minute: 19))
        #expect(viewModel.state.departure as? DepartureUiAt == eightNineteenToday)

        viewModel.onEvent(PlannerEventChooseDepartureTime.shared)
        viewModel.onEvent(PlannerEventCancelDepartureTime.shared)

        #expect(viewModel.state.departure as? DepartureUiAt == eightNineteenToday)
        #expect(viewModel.state.departurePicker == nil)
    }

    @Test
    func lb303_leaveNow_afterAPickedTime_leavesNowAgain() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview())
        viewModel.onEvent(PlannerEventDepartAt(year: 2026, month: 10, day: 28, hour: 8, minute: 19))
        #expect(viewModel.state.departure as? DepartureUiAt == eightNineteenToday)

        viewModel.onEvent(PlannerEventLeaveNow.shared)

        #expect(viewModel.state.departure is DepartureUiNow)
    }

    @Test
    func lb303_aPickedTime_staysWhenTheRootShowsAgain() {
        let viewModel = PlannerViewModel(useCases: SharedDependencies.shared.preview())
        viewModel.onEvent(PlannerEventDepartAt(year: 2026, month: 10, day: 28, hour: 8, minute: 19))

        viewModel.onEvent(PlannerEventShown.shared)

        #expect(viewModel.state.departure as? DepartureUiAt == eightNineteenToday)
    }

    @Test
    func lb303_theRootShowingAgain_countsTheDepartureDayAgain_andTellsTheView() {
        // Kept on view past midnight: "tomorrow" is today when the root shows again, and the view redraws.
        let useCases = SharedDependencies.shared.preview()
        let viewModel = PlannerViewModel(useCases: useCases)
        viewModel.onEvent(PlannerEventShown.shared)
        viewModel.onEvent(PlannerEventDepartAt(year: 2026, month: 10, day: 29, hour: 8, minute: 19))
        #expect((viewModel.state.departure as? DepartureUiAt)?.relativeDay == .tomorrow)
        nonisolated(unsafe) var told = false
        withObservationTracking { _ = viewModel.state } onChange: { told = true }

        useCases.timePasses(minutes: 24 * 60)
        viewModel.onEvent(PlannerEventShown.shared)

        #expect(told)
        #expect((viewModel.state.departure as? DepartureUiAt)?.relativeDay == .today)
    }
}
