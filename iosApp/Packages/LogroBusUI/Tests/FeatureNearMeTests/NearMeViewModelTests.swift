@testable import FeatureNearMe
import Shared
import Testing

/// FR-LOC-01 to 03 and FR-NEAR-06 on iOS: "Near you" shows the row without location, which shows the system's prompt
/// once and Settings after a denial; nothing asks before the tap; location runs only while Near Me shows.
@MainActor
struct NearMeViewModelTests {
    @Test
    func lb201_withoutLocation_nearYouIsTheTurnOnLocationRow() async throws {
        let viewModel = NearMeViewModel(useCases: SharedDependencies.shared.preview()) { FakeLocationAuthorization() }
        #expect(viewModel.state.nearYou is NearYouUiUnknown)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }

        try await waitUntil { viewModel.state.nearYou is NearYouUiTurnOnLocation }
    }

    @Test
    func lb201_withLocation_thereIsNoRow() async throws {
        let useCases = SharedDependencies.shared.preview().locationOn()
        let viewModel = NearMeViewModel(useCases: useCases) { FakeLocationAuthorization(status: .granted) }

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }

        try await waitUntil { viewModel.state.nearYou is NearYouUiLocationOn }
    }

    @Test
    func lb201_openingNearMeAsksForNothing() async throws {
        let authorization = FakeLocationAuthorization()
        let viewModel = NearMeViewModel(useCases: SharedDependencies.shared.preview()) { authorization }

        let observation = Task { await viewModel.observe() }
        let following = Task { await viewModel.followAuthorization() }
        defer {
            observation.cancel()
            following.cancel()
        }
        try await waitUntil { viewModel.state.nearYou is NearYouUiTurnOnLocation }

        #expect(authorization.prompts == 0)
        #expect(authorization.settingsOpened == 0)
    }

    @Test
    func lb201_theRowShowsTheSystemsPrompt_untilTheUserHasAnswered() {
        let authorization = FakeLocationAuthorization(status: .notDetermined)
        let viewModel = NearMeViewModel(useCases: SharedDependencies.shared.preview()) { authorization }

        viewModel.turnOnLocation()

        #expect(authorization.prompts == 1)
        #expect(authorization.settingsOpened == 0)
    }

    @Test
    func lb201_afterADenial_theRowOpensSettings() {
        // iOS asks once: after a denial, only Settings can turn location on.
        let authorization = FakeLocationAuthorization(status: .denied)
        let viewModel = NearMeViewModel(useCases: SharedDependencies.shared.preview()) { authorization }

        viewModel.turnOnLocation()

        #expect(authorization.settingsOpened == 1)
        #expect(authorization.prompts == 0)
    }

    @Test
    func lb201_whenTheSystemHasAlreadyGrantedIt_theRowReadsAccessAgain() async throws {
        // Granted in Settings while the row showed, say: no prompt, no Settings, a new read.
        let authorization = FakeLocationAuthorization(status: .granted)
        let viewModel = NearMeViewModel(useCases: SharedDependencies.shared.preview()) { authorization }
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state.nearYou is NearYouUiTurnOnLocation }

        viewModel.turnOnLocation()

        #expect(viewModel.checks == 1)
        #expect(authorization.prompts == 0)
        #expect(authorization.settingsOpened == 0)
    }

    @Test
    func lb201_aGrantedPrompt_readsAccessAgain_andTheRowGoes() async throws {
        let useCases = SharedDependencies.shared.preview()
        let authorization = FakeLocationAuthorization(status: .notDetermined)
        let viewModel = NearMeViewModel(useCases: useCases) { authorization }
        var observation = Task { await viewModel.observe() }
        let following = Task { await viewModel.followAuthorization() }
        defer {
            observation.cancel()
            following.cancel()
        }
        try await waitUntil { viewModel.state.nearYou is NearYouUiTurnOnLocation }

        viewModel.turnOnLocation()
        // The user allows location in the prompt.
        useCases.locationOn()
        authorization.change(to: .granted)
        try await waitUntil { viewModel.checks == 1 }
        // What `.task(id:)` does when the checks change.
        observation.cancel()
        observation = Task { await viewModel.observe() }

        try await waitUntil { viewModel.state.nearYou is NearYouUiLocationOn }
    }

    @Test
    func lb201_aRevokedPermission_bringsTheRowBack() async throws {
        let useCases = SharedDependencies.shared.preview().locationOn()
        let viewModel = NearMeViewModel(useCases: useCases) { FakeLocationAuthorization(status: .granted) }
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state.nearYou is NearYouUiLocationOn }

        useCases.locationOff()

        try await waitUntil { viewModel.state.nearYou is NearYouUiTurnOnLocation }
    }

    @Test
    func lb201_leavingNearMeStopsTheLocationUpdates() async throws {
        let useCases = SharedDependencies.shared.preview().locationOn()
        let viewModel = NearMeViewModel(useCases: useCases) { FakeLocationAuthorization(status: .granted) }

        let observation = Task { await viewModel.observe() }
        try await waitUntil { useCases.activeCollections == 1 }
        // What `.task {}` does when another tab is selected or the app leaves the foreground (FR-LOC-03).
        observation.cancel()
        await observation.value

        try await waitUntil { useCases.activeCollections == 0 }
    }

    @Test
    func lb201_whileLocationIsOn_anAuthorizationChangeLeavesTheUpdatesRunning() async throws {
        // Precise to approximate, say: a revoked permission ends the shared flow by itself.
        let useCases = SharedDependencies.shared.preview().locationOn()
        let authorization = FakeLocationAuthorization(status: .granted)
        let viewModel = NearMeViewModel(useCases: useCases) { authorization }
        let observation = Task { await viewModel.observe() }
        let following = Task { await viewModel.followAuthorization() }
        defer {
            observation.cancel()
            following.cancel()
        }
        try await waitUntil { viewModel.state.nearYou is NearYouUiLocationOn }

        authorization.change(to: .granted)
        try await Task.sleep(for: .milliseconds(100))

        #expect(viewModel.checks == 0)
    }
}
