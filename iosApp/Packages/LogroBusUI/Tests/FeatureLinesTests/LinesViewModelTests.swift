@testable import FeatureLines
import Foundation
import Shared
import Testing

/// LB-101, ADR-0007 and architecture.md §5.2: the Lines ViewModel builds its state with the shared use case and mapper,
/// retries on Retry, and leaving its screen stops the use case's collection. LB-103: a row's toggle sets its line as a
/// favourite. LB-102: the search field's text filters the lines. LB-110: the edit sheet's new order is saved, and a
/// star beyond the limit shows the limit message once.
@MainActor
struct LinesViewModelTests {
    @Test
    func lb101_theViewModelLoads_thenShowsDayLinesThenNightLines_eachSortedByCode() async throws {
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview())
        #expect(viewModel.state is LinesUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LinesUiStateContent }

        let content = try #require(viewModel.state as? LinesUiStateContent)
        #expect(content.groups.map(\.kind) == [.day, .night])
        #expect(content.groups.map { $0.lines.map(\.code) } == [
            ["1", "2", "3", "4", "5", "6", "7", "9", "10", "11"],
            ["B1", "B2", "B3"]
        ])
        // The sample's line 11 is not operational, so its row shows the status chip (FR-LINE-02).
        #expect(content.groups[0].lines.filter { !$0.isOperational }.map(\.code) == ["11"])
    }

    @Test
    func lb101_aFirstLaunchWithNoNetworkShowsTheNetworkError_andRetryLoadsTheLines() async throws {
        let useCases = SharedDependencies.shared.preview().offline()
        let viewModel = LinesViewModel(useCases: useCases)

        let first = Task { await viewModel.observe() }
        try await waitUntil { viewModel.state is LinesUiStateError }
        let error = try #require(viewModel.state as? LinesUiStateError)
        #expect(error.reason == .network)

        // Connectivity returns, and the user taps Retry.
        useCases.synced()
        viewModel.onEvent(LinesEventRetry.shared)
        #expect(viewModel.state is LinesUiStateLoading)
        #expect(viewModel.attempt == 1)

        // What `.task(id: viewModel.attempt)` does when the attempt changes: the old load stops, a new one starts.
        first.cancel()
        await first.value
        let second = Task { await viewModel.observe() }
        defer { second.cancel() }
        try await waitUntil { viewModel.state is LinesUiStateContent }
    }

    @Test
    func lb101_noLinesShowsTheEmptyState() async throws {
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview().neverSynced())

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }

        try await waitUntil { viewModel.state is LinesUiStateEmpty }
    }

    @Test
    func lb101_theStateStaysLoadingUntilTheLinesArrive() async throws {
        let useCases = SharedDependencies.shared.preview().loading()
        let viewModel = LinesViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        // Subscribed, and still nothing to show.
        try await waitUntil { useCases.activeCollections == 1 }

        #expect(viewModel.state is LinesUiStateLoading)
    }

    @Test
    func lb103_aRowsToggleAddsItsLineAtTheEndOfTheCarousel_andRemovesIt() async throws {
        // FR-LINE-12 and FR-LINE-15: the carousel changes through the use case's flow, at once.
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview().favouriteLine(id: 10))
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { favourites(of: viewModel) == ["10"] }

        viewModel.onEvent(LinesEventSetFavourite(lineId: 3, isFavourite: true))
        try await waitUntil { favourites(of: viewModel) == ["10", "3"] }
        let row = try #require((viewModel.state as? LinesUiStateContent)?.groups[0].lines.first { $0.code == "3" })
        #expect(row.isFavourite)

        viewModel.onEvent(LinesEventSetFavourite(lineId: 10, isFavourite: false))
        try await waitUntil { favourites(of: viewModel) == ["3"] }
    }

    @Test
    func lb103_twoQuickOppositeTapsLeaveTheLastOne() async throws {
        // Each tap's write runs in its own task; the repository applies them in the order they were made.
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview())
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { favourites(of: viewModel) == [] }

        viewModel.onEvent(LinesEventSetFavourite(lineId: 3, isFavourite: true))
        viewModel.onEvent(LinesEventSetFavourite(lineId: 3, isFavourite: false))
        try await Task.sleep(for: .milliseconds(200))

        #expect(favourites(of: viewModel) == [])
    }

    @Test
    func lb102_aSearchFiltersTheList_andClearingItBringsTheListBack() async throws {
        // FR-LINE-03, FR-LINE-16: the shared mapper makes the results of the search field's text, without a new load.
        let useCases = SharedDependencies.shared.preview()
        let viewModel = LinesViewModel(useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LinesUiStateContent }

        viewModel.onEvent(LinesEventSearch(query: "varea"))

        #expect(viewModel.query == "varea")
        let results = try #require(viewModel.state as? LinesUiStateSearchResults)
        #expect(results.groups.map { $0.lines.map(\.code) } == [["2"], ["B1"]])

        viewModel.onEvent(LinesEventSearch(query: ""))

        #expect(viewModel.state is LinesUiStateContent)
        #expect(useCases.activeCollections == 1)
    }

    @Test
    func lb102_aSearchTypedBeforeTheLinesArriveFiltersThem() async throws {
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview())
        viewModel.onEvent(LinesEventSearch(query: "varea"))
        #expect(viewModel.state is LinesUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }

        try await waitUntil { viewModel.state is LinesUiStateSearchResults }
    }

    @Test
    func lb102_aSearchThatFindsNothingShowsNoMatches() async throws {
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview())
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is LinesUiStateContent }

        viewModel.onEvent(LinesEventSearch(query: "tranvia"))

        #expect(viewModel.state is LinesUiStateNoMatches)
    }

    @Test
    func lb102_aRetryKeepsTheSearch() async throws {
        let useCases = SharedDependencies.shared.preview().offline()
        let viewModel = LinesViewModel(useCases: useCases)
        let first = Task { await viewModel.observe() }
        try await waitUntil { viewModel.state is LinesUiStateError }
        viewModel.onEvent(LinesEventSearch(query: "varea"))

        useCases.synced()
        viewModel.onEvent(LinesEventRetry.shared)
        first.cancel()
        await first.value
        let second = Task { await viewModel.observe() }
        defer { second.cancel() }

        try await waitUntil { viewModel.state is LinesUiStateSearchResults }
        #expect(viewModel.query == "varea")
    }

    @Test
    func lb102_aKeyDuringARetrysLoadKeepsLoading() async throws {
        // Retry forgets the failed result, so a key cannot map it again and bring the error back over Loading.
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview().offline())
        let observation = Task { await viewModel.observe() }
        try await waitUntil { viewModel.state is LinesUiStateError }
        observation.cancel()
        await observation.value

        viewModel.onEvent(LinesEventRetry.shared)
        viewModel.onEvent(LinesEventSearch(query: "va"))

        #expect(viewModel.state is LinesUiStateLoading)
    }

    @Test
    func lb110_theEditSheetsNewOrderIsSaved_andTheCarouselFollows() async throws {
        // FR-FAV-02: the order persists through the use case, and the carousel shows it at once.
        let useCases = SharedDependencies.shared.preview()
            .favouriteLine(id: 10).favouriteLine(id: 4).favouriteLine(id: 101)
        let viewModel = LinesViewModel(useCases: useCases)
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { favourites(of: viewModel) == ["10", "4", "B1"] }

        viewModel.onEvent(LinesEventReorderFavourites(lineIds: [101, 10, 4].map { KotlinInt(value: $0) }))

        try await waitUntil { favourites(of: viewModel) == ["B1", "10", "4"] }
    }

    @Test
    func lb110_aStarBeyondTheLimitShowsTheLimit_untilTheMessageHasBeenShown() async throws {
        // FR-FAV-05: 20 lines are favourites, ids the catalogue need not have; the toggle stays off, and says why once.
        let useCases = (201...220).reduce(SharedDependencies.shared.preview()) { $0.favouriteLine(id: Int32($1)) }
        let viewModel = LinesViewModel(useCases: useCases)
        #expect(viewModel.favouriteLimit == nil)

        viewModel.onEvent(LinesEventSetFavourite(lineId: 3, isFavourite: true))
        try await waitUntil { viewModel.favouriteLimit == 20 }

        viewModel.onEvent(LinesEventFavouriteLimitShown.shared)
        #expect(viewModel.favouriteLimit == nil)
    }

    @Test
    func lb110_aStarThatSavesShowsNoLimit() async throws {
        let viewModel = LinesViewModel(useCases: SharedDependencies.shared.preview())
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { favourites(of: viewModel) == [] }

        viewModel.onEvent(LinesEventSetFavourite(lineId: 3, isFavourite: true))
        try await waitUntil { favourites(of: viewModel) == ["3"] }

        #expect(viewModel.favouriteLimit == nil)
    }

    /// The carousel's cards, by code.
    private func favourites(of viewModel: LinesViewModel) -> [String]? {
        (viewModel.state as? LinesUiStateContent)?.favourites.map(\.code)
    }

    @Test
    func lb101_leavingTheScreenStopsTheUseCasesCollection() async throws {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = LinesViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        try await waitUntil { useCases.activeCollections == 1 }
        // What `.task {}` does when the screen leaves.
        observation.cancel()
        await observation.value

        try await waitUntil { useCases.activeCollections == 0 }
    }
}

/// Waits on the main actor until `condition` holds, failing after `timeout`.
@MainActor
func waitUntil(
    timeout: Duration = .seconds(5),
    _ condition: () -> Bool,
    sourceLocation: SourceLocation = #_sourceLocation
) async throws {
    let deadline = ContinuousClock.now + timeout
    while !condition() {
        guard ContinuousClock.now < deadline else {
            Issue.record("The condition did not hold within \(timeout)", sourceLocation: sourceLocation)
            return
        }
        try await Task.sleep(for: .milliseconds(10))
    }
}
