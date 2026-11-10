@testable import FeatureLines
import Shared
import Testing

/// LB-103 (FR-LINE-09) and ADR-0007: the line detail's ViewModel builds its state with the shared use case and mapper,
/// and its toolbar's toggle sets the line as a favourite.
@MainActor
struct LineDetailViewModelTests {
    @Test
    func lb103_theDetailLoads_thenShowsItsLine() async throws {
        let viewModel = LineDetailViewModel(lineId: 3, useCases: SharedDependencies.shared.preview())
        #expect(viewModel.state is LineDetailUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LineDetailUiStateContent }

        let line = try #require((viewModel.state as? LineDetailUiStateContent)?.line)
        #expect(line.code == "3")
        #expect(line.displayName == "Campillo - El Cubo")
        #expect(!line.isFavourite)
    }

    @Test
    func lb103_theToolbarsToggleSetsTheLineAsAFavourite_orNot() async throws {
        let viewModel = LineDetailViewModel(lineId: 3, useCases: SharedDependencies.shared.preview())
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LineDetailUiStateContent }

        viewModel.onEvent(LineDetailEventSetFavourite(isFavourite: true))
        try await waitUntil { isFavourite(viewModel) == true }

        viewModel.onEvent(LineDetailEventSetFavourite(isFavourite: false))
        try await waitUntil { isFavourite(viewModel) == false }
    }

    @Test
    func lb110_aStarBeyondTheLimitShowsTheLimit_untilTheMessageHasBeenShown() async throws {
        let useCases = (201...220).reduce(SharedDependencies.shared.preview()) { $0.favouriteLine(id: Int32($1)) }
        let viewModel = LineDetailViewModel(lineId: 3, useCases: useCases)
        #expect(viewModel.favouriteLimit == nil)

        viewModel.onEvent(LineDetailEventSetFavourite(isFavourite: true))
        try await waitUntil { viewModel.favouriteLimit == 20 }

        viewModel.onEvent(LineDetailEventFavouriteLimitShown.shared)
        #expect(viewModel.favouriteLimit == nil)
    }

    @Test
    func lb103_aFirstLaunchWithNoNetworkShowsTheNetworkError_andRetryLoadsTheLine() async throws {
        let useCases = SharedDependencies.shared.preview().offline()
        let viewModel = LineDetailViewModel(lineId: 3, useCases: useCases)

        let first = Task { await viewModel.observe() }
        try await waitUntil { viewModel.state is LineDetailUiStateError }
        #expect((viewModel.state as? LineDetailUiStateError)?.reason == .network)

        useCases.synced()
        viewModel.onEvent(LineDetailEventRetry.shared)
        #expect(viewModel.state is LineDetailUiStateLoading)
        #expect(viewModel.attempt == 1)

        // What `.task(id: viewModel.attempt)` does when the attempt changes.
        first.cancel()
        await first.value
        let second = Task { await viewModel.observe() }
        defer { second.cancel() }
        try await waitUntil { viewModel.state is LineDetailUiStateContent }
    }

    @Test
    func lb105_eachStopShowsItsNextCall_andAStopWithoutOneKeepsTheDash() async throws {
        // FR-LINE-05: line 10's outbound pattern has two stops. One call is scripted, at the first, so the second is a
        // row with no call: the criterion's dash. Asking for a stop that does not exist would prove nothing.
        let useCases = SharedDependencies.shared.preview().withOneArrival()
        let viewModel = LineDetailViewModel(lineId: 10, useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.stopArrivals[1000]?.eta != nil }

        let first = try #require(viewModel.stopArrivals[1000]?.eta as? EtaDisplayMinutes)
        #expect(first.minutes == 4)
        #expect(viewModel.stopArrivals[1000]?.isLowConfidence == false)
        #expect(viewModel.stopArrivals.count == 2, "the outbound pattern's two stops")
        #expect(viewModel.stopArrivals[1001]?.eta == nil, "a stop with no call keeps the criterion's dash")
    }

    @Test
    func lb105_anInaccuratePredictionIsMarkedLowConfidence() async throws {
        // DD-09: the source's `predictionInaccurate`, which the preview's second scripted call carries.
        let viewModel = LineDetailViewModel(lineId: 10, useCases: SharedDependencies.shared.preview().withArrivals())
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.stopArrivals[1001]?.eta != nil }

        let second = try #require(viewModel.stopArrivals[1001]?.eta as? EtaDisplayMinutes)
        #expect(second.minutes == 7)
        #expect(viewModel.stopArrivals[1001]?.isLowConfidence == true, "DD-09: the second call is the rougher one")
    }

    @Test
    func lb105_aNewDirectionAsksForItsOwnCalls() async throws {
        let useCases = SharedDependencies.shared.preview().withArrivals()
        let viewModel = LineDetailViewModel(lineId: 10, useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.stopArrivals[1000]?.eta != nil }
        #expect(useCases.arrivalCollections == 1)

        viewModel.onEvent(LineDetailEventSelectDirection(direction: .inbound))
        try await waitUntil { useCases.arrivalCollections == 2 }
        // The outbound calls belong to the outbound pattern, so the inbound list never shows them. The script has no
        // inbound call, so every inbound stop is the criterion's dash, and the outbound stops are not in this list.
        #expect(viewModel.stopArrivals[1000] == nil, "the outbound stops are not the inbound pattern's")
        #expect(viewModel.stopArrivals[1050]?.eta == nil, "the script has no inbound call")
        #expect(viewModel.stopArrivals.count == 2, "the inbound pattern's two stops, and nothing else")
    }

    @Test
    func lb105_theUsersFormatRewordsTheCallsAlreadyReceived_withoutAskingAgain() async throws {
        let useCases = SharedDependencies.shared.preview().withArrivals()
        let viewModel = LineDetailViewModel(lineId: 10, useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.stopArrivals[1000]?.eta != nil }
        let collections = useCases.arrivalCollections

        useCases.settings(theme: .system, etaFormat: .clockTime)
        try await waitUntil { viewModel.stopArrivals[1000]?.eta is EtaDisplayClockTime }
        #expect(useCases.arrivalCollections == collections, "FR-SET-01: re-worded, not asked again")
    }

    @Test
    func lb105_aRestartedObservationKeepsAskingForTheNewDirection() async throws {
        // SwiftUI restarts `.task(id:)` on Retry. The new observation must still collect the line and answer a
        // direction change; the ViewModel's own guard against a cancelled loop clearing its successor's stream is
        // defensive, and awaiting the first task here serialises the two, so this test cannot prove that guard.
        let useCases = SharedDependencies.shared.preview().withArrivals()
        let viewModel = LineDetailViewModel(lineId: 10, useCases: useCases)
        let first = Task { await viewModel.observe() }
        try await waitUntil { viewModel.stopArrivals[1000]?.eta != nil }
        first.cancel()
        await first.value

        let second = Task { await viewModel.observe() }
        defer { second.cancel() }
        try await waitUntil { viewModel.stopArrivals[1000]?.eta != nil }
        let before = useCases.arrivalCollections

        viewModel.onEvent(LineDetailEventSelectDirection(direction: .inbound))
        try await waitUntil { useCases.arrivalCollections == before + 1 }
    }

    private func isFavourite(_ viewModel: LineDetailViewModel) -> Bool? {
        (viewModel.state as? LineDetailUiStateContent)?.line.isFavourite
    }
}
