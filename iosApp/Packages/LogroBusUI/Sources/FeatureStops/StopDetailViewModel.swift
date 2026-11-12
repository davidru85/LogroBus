import Observation
// Kotlin types declare no Sendable conformance, so Swift 6 refuses to send a use case to the async function SKIE makes
// of a `suspend` one. Kotlin/Native's memory model lets any thread call them (ADR-0004), so Shared's types are
// imported as pre-concurrency here.
@preconcurrency import Shared

/// What the stop detail sends its ViewModel. Going back is navigation, which the app shell does. Swift-side on purpose:
/// Retry is the only event, and a shared sealed type for one payload-less case would make Swift carry a Kotlin enum for
/// nothing (owner decision, 2026-11-10).
public enum StopDetailEvent {
    /// FR-ERR-05: load again after an Error state.
    case retry
}

/// A stop's detail (LB-205, FR-STOP-03 to FR-STOP-05, FR-STOP-09). It orchestrates only: the shared use cases and the
/// mapper build the state (ADR-0007). The stop's served lines are read from the catalogue first, because API-06
/// asks for
/// exactly those lines (`lines=`), and every ETA and the list's age are measured against the graph's `AppClock`, never
/// the platform clock (FR-ERR-06, spec §8.6). One screen serves both doors (FR-NAV-07): pushed from line detail, or in
/// Near Me's sheet (LB-203).
@MainActor
@Observable
public final class StopDetailViewModel {
    public private(set) var state: any StopDetailUiState = StopDetailUiStateLoading.shared
    /// Bumped by Retry. The view keys its `.task` on it, so a new value starts a new `observe()`.
    public private(set) var attempt = 0
    private let stopId: Int32
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases

    /// FR-SET-01: the user's ETA format, whose default the app opens on. Settings' flow replaces it while the screen
    /// shows, and each new value maps the arrivals again, without a second request.
    private var etaFormat: EtaFormat = .minutes

    /// The last answer of API-06, so a format change maps again without a second collection. `nil` until the first.
    private var lastArrivals: AppResult<StopArrivals>?

    /// FR-STOP-03: the lines the stop's detail asks API-06 for, as the catalogue last gave them, and the ones the
    /// running collection asked with. Kept apart, so a change re-subscribes once: the use case's flow takes its lines
    /// when it starts.
    private var requestedLineIds: [Int32]?
    private var collectedLineIds: [Int32]?

    /// Carries the served lines to the loop that collects, since the arrivals use case's flow is cold and cannot take
    /// new lines once started. Never displayed, so it takes no part in observation.
    @ObservationIgnored private var lineChanges: AsyncStream<[Int32]>.Continuation?

    /// Which `observe()` owns [lineChanges]. SwiftUI cancels the old task when `.task(id:)` restarts and does not wait
    /// for it, so an old loop's cleanup can run after the new loop has installed its own stream; it must not clear the
    /// new one, or the served lines would never be delivered again.
    @ObservationIgnored private var arrivalsGeneration = 0

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(stopId: Int32, useCases: any UseCases = SharedDependencies.shared) {
        self.stopId = stopId
        self.useCases = useCases
    }

    /// LB-208 (FR-STOP-08, FR-FAV-05): whether the stop is saved, and under which alias. `nil` until the saved stops
    /// first arrive, so the star shows nothing rather than the wrong thing.
    public private(set) var saved: (isSaved: Bool, alias: String?)?

    /// FR-FAV-05: a save the limit refused, which the screen reports once.
    public private(set) var limitReached = false

    /// FR-STOP-08: saves the stop. A stop already saved keeps its alias, so this only adds; removing and renaming are the
    /// Edit sheet's, in Near Me.
    public func save(alias: String? = nil) async {
        // SKIE hands the result back with Kotlin's own class for the enum, so it bridges to Swift's case here.
        guard let result = try? await useCases.savedStops().add(stopId: stopId, alias: alias),
              let addition = (result as? AppResultSuccess<__FavouriteAddition>)?.value else { return }
        limitReached = FavouriteAddition._unconditionallyBridgeFromObjectiveC(addition) == .limitReached
    }

    /// FR-STOP-08: removes the saved stop.
    public func remove() async {
        _ = try? await useCases.savedStops().remove(stopId: stopId)
    }

    /// FR-FAV-05: marks the limit's message as shown, so the next save reports it again.
    public func clearLimit() {
        limitReached = false
    }

    /// The stop's own save state: the saved stops' collection, filtered to this stop, so a removal made in Near Me's
    /// sheet clears the star at once.
    private func observeSaved() async {
        for await result in useCases.observeMyStops().invoke() {
            switch onEnum(of: result) {
            case .failure:
                continue
            case .success(let success):
                guard let stops = success.value else { continue }
                let row = stops.stops.first { $0.stopId == stopId }
                saved = (isSaved: row != nil, alias: row?.alias)
            }
        }
    }

    public func onEvent(_ event: StopDetailEvent) {
        switch event {
        case .retry:
            state = StopDetailUiStateLoading.shared
            lastArrivals = nil
            requestedLineIds = nil
            // A new `observe()` collects again, so the lines it was collecting must not count as already asked for.
            collectedLineIds = nil
            attempt += 1
        }
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves, and
    /// it the arrivals collection, so nothing polls once the detail is gone (spec §8.6).
    public func observe() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observeStop() }
            group.addTask { await self.observePreferences() }
            group.addTask { await self.observeArrivals() }
            group.addTask { await self.observeSaved() }
        }
    }

    /// FR-STOP-03: the stop with its served lines, from the cached catalogue, so its header shows before the live
    /// source answers. Its lines are what API-06 is asked for.
    private func observeStop() async {
        for await result in useCases.observeStopDetail().invoke(stopId: stopId) {
            switch onEnum(of: result) {
            case .success(let success):
                guard let stop = success.value else { continue }
                // Kotlin's `List<ServedLine>` reaches Swift as the array of the Kotlin class.
                let lineIds = stop.servedLines.map(\.lineId)
                requestedLineIds = lineIds
                lineChanges?.yield(lineIds)
            case .failure(let failure):
                // FR-ERR-05: without the stop there is nothing to show, and Retry is the way out. The reason is the
                // same mapping the shared `ErrorReason.of` makes, kept here because that companion is not exported.
                state = StopDetailUiStateError(reason: Self.reason(of: failure.error))
            }
        }
    }

    /// FR-SET-01: the user's ETA format, from Settings' own flow. A change keeps the answer and maps it again.
    private func observePreferences() async {
        for await preferences in useCases.observePreferences().invoke() {
            let changed = preferences.etaFormat != etaFormat
            etaFormat = preferences.etaFormat
            if changed { show() }
        }
    }

    /// FR-STOP-03: API-06 for the stop's served lines, collected while the screen shows. The first collection starts as
    /// soon as the catalogue gives the lines, and each later change cancels the running one and starts another.
    private func observeArrivals() async {
        let (changes, change) = AsyncStream<[Int32]>.makeStream()
        lineChanges = change
        arrivalsGeneration += 1
        let generation = arrivalsGeneration
        defer {
            change.finish()
            // Only the loop that still owns the stream clears it: a cancelled predecessor must leave its successor's
            // stream alone.
            if arrivalsGeneration == generation { lineChanges = nil }
        }
        // A new loop starts a new collection: whatever the last one was asking for ended with it.
        collectedLineIds = nil
        // Lines known before this loop started, such as a catalogue answer that arrived while it was being set up.
        if let requestedLineIds { change.yield(requestedLineIds) }
        var observation: Task<Void, Never>?
        defer { observation?.cancel() }
        for await lineIds in changes {
            // The stream may carry the same lines twice, once from the catalogue's mapping and once from this loop's
            // own first read; a collection already asking for them is not restarted.
            guard lineIds != collectedLineIds else { continue }
            collectedLineIds = lineIds
            observation?.cancel()
            observation = Task { await self.collectArrivals(lineIds) }
        }
    }

    /// One collection of API-06. A failure becomes the Error state (FR-ERR-05), as the mapper decides: a stale list
    /// re-worded as "Now" would be worse than none.
    private func collectArrivals(_ lineIds: [Int32]) async {
        // Swift runs a cancelled task's body anyway, so the check comes before any write: a superseded collection must
        // not leave its answer on screen.
        guard !Task.isCancelled else { return }
        lastArrivals = nil
        for await result in useCases.observeStopArrivals().invoke(
            stopId: stopId, lineIds: lineIds.map { KotlinInt(value: $0) }
        ) {
            guard !Task.isCancelled else { return }
            lastArrivals = result
            show()
        }
    }

    /// FR-STOP-03 to FR-STOP-05: the mapper builds the state from the last answer, the graph's clock and the user's
    /// format. `now` is read here, when the row renders, so the first answer corrects the next render (FR-ERR-06).
    private func show() {
        guard let result = lastArrivals else { return }
        state = StopDetailUiMapper.shared.state(
            result: result, stopId: stopId, now: useCases.clock().now(), format: etaFormat
        )
    }

    /// FR-ERR-05: `ErrorReason.of`'s rule, which the shared companion spells out for both platforms. Swift cannot call
    /// it, because SKIE exports `ErrorReason` as an enum without the Kotlin companion, so the mapping is repeated here.
    private static func reason(of error: any AppError) -> ErrorReason {
        switch onEnum(of: error) {
        case .network: .network
        case .server: .server
        case .data, .notFound: .data
        }
    }
}
