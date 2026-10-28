@testable import FeatureSettings
import Foundation
import Shared
import Testing

/// ADR-0007 and architecture.md §5.2: a Swift ViewModel builds its state with the shared use case and mapper, and
/// leaving its screen stops the use case's collection.
@MainActor
struct AboutDataViewModelTests {
    @Test
    func lb010_theViewModelLoads_thenShowsWhatTheSharedMapperMakesOfTheUseCase() async throws {
        let viewModel = AboutDataViewModel(useCases: SharedDependencies.shared.preview())
        #expect(viewModel.state is AboutDataUiStateLoading)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is AboutDataUiStateContent }

        let content = try #require(viewModel.state as? AboutDataUiStateContent)
        #expect(content.lastSyncs.map(\.dataset) == [.lines, .stops])
        // The preview's syncs happened at 09:30 UTC, which is 10:30 in Madrid (AGENTS.md §5.5).
        #expect(content.lastSyncs.map { $0.lastSuccess?.hour } == [10, 10])
    }

    @Test
    func lb010_leavingTheScreenStopsTheUseCasesCollection() async throws {
        let useCases = SharedDependencies.shared.preview()
        let viewModel = AboutDataViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        try await waitUntil { useCases.activeCollections == 1 }
        // What `.task {}` does when the screen leaves.
        observation.cancel()
        await observation.value

        try await waitUntil { useCases.activeCollections == 0 }
    }

    @Test
    func lb010_theStateStaysLoadingUntilTheUseCaseEmits() async throws {
        let useCases = SharedDependencies.shared.preview().loading()
        let viewModel = AboutDataViewModel(useCases: useCases)

        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        // Subscribed, and still nothing to show.
        try await waitUntil { useCases.activeCollections == 1 }

        #expect(viewModel.state is AboutDataUiStateLoading)
    }

    @Test
    func lb010_aFailureShowsTheErrorState_andRetryReadsAgain() async throws {
        let useCases = SharedDependencies.shared.preview().failing()
        let viewModel = AboutDataViewModel(useCases: useCases)

        await viewModel.observe()
        let error = try #require(viewModel.state as? AboutDataUiStateError)
        #expect(error.reason == .data)

        useCases.neverSynced()
        viewModel.onEvent(AboutDataEventRetry.shared)
        #expect(viewModel.state is AboutDataUiStateLoading)
        #expect(viewModel.attempt == 1)

        // What `.task(id: viewModel.attempt)` does when the attempt changes.
        let observation = Task { await viewModel.observe() }
        defer { observation.cancel() }
        try await waitUntil { viewModel.state is AboutDataUiStateContent }
    }

    @Test(arguments: ["America/New_York", "Asia/Tokyo"])
    func lb405_timesShowOnTheMadridClockInTheGivenLocale_whateverTheDeviceZone(zone: String) throws {
        let deviceZone = NSTimeZone.default
        NSTimeZone.default = try #require(TimeZone(identifier: zone))
        defer { NSTimeZone.default = deviceZone }
        let time = MadridDateTime(year: 2026, month: 10, day: 28, hour: 10, minute: 30)

        let spanish = time.formatted(locale: Locale(identifier: "es_ES"))
        #expect(spanish.contains("10:30"))
        #expect(spanish.contains("oct"))
        #expect(time.formatted(locale: Locale(identifier: "en_US")).contains("10:30 AM"))
    }
}

/// Waits on the main actor until `condition` holds, failing after `timeout`.
@MainActor
private func waitUntil(
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
