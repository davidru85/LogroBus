import Observation
// Kotlin types declare no Sendable conformance, so Swift 6 refuses to send a use case to the async function SKIE makes
// of a `suspend` one. Kotlin/Native's memory model lets any thread call them (ADR-0004), so Shared's types are
// imported as pre-concurrency here.
@preconcurrency import Shared

/// The detail of one line (LB-103, LB-104, LB-105). It orchestrates only: the shared use cases and mappers build the
/// state (ADR-0007). The map of both shapes waits for LB-013.
@MainActor
@Observable
public final class LineDetailViewModel {
    public private(set) var state: any LineDetailUiState = LineDetailUiStateLoading.shared
    /// Bumped by Retry. The view keys its `.task` on it, so a new value starts a new `observe()`.
    public private(set) var attempt = 0
    /// FR-FAV-05: the limit the star has just reached, for the screen's message, until the screen says it has shown it.
    public private(set) var favouriteLimit: Int?
    /// FR-LINE-05 (LB-105): the next arrival of each stop of the shown direction, keyed by the stop's id, as
    /// `arrivalsByStop` maps it. A stop with no call at all keeps no entry, which its row draws as "–".
    public private(set) var stopArrivals: [Int32: StopArrivalUi] = [:]
    private let lineId: Int32
    // Kept rather than resolved here: SwiftUI may build a view's ViewModel on every parent update and keep only the
    // first, so the init stays cheap.
    private let useCases: any UseCases
    // The last toggle's write. Each waits for the one before: SKIE may start two Kotlin calls in either order, so two
    // quick taps could otherwise land the wrong way round.
    private var favouriteWrite: Task<Void, Never>?

    /// The last value the use case emitted, so a direction picked after it maps again without a second collection.
    private var lastResult: AppResult<LinesWithFavourites>?

    /// FR-LINE-04: the direction the toggle has selected; `nil` until one is picked, so the line's first shows.
    private var selectedDirection: Direction?

    /// FR-LINE-05: the calls the arrivals use case last emitted, and the direction they belong to. Kept, so a new ETA
    /// format maps again without a second collection; cleared whenever the direction changes, so one direction's
    /// calls never land on the other's stop list.
    private var lastArrivals: [Arrival] = []
    private var arrivalsDirection: Direction?

    /// FR-SET-01: the user's ETA format, whose default the app opens on. Settings' flow replaces it while the screen
    /// shows, and each new value maps the ETAs again.
    private var etaFormat: EtaFormat = .minutes

    /// The direction the running arrivals collection asks for, and the last one started, so a change re-subscribes
    /// once: the use case's flow takes its direction when it starts.
    private var requestedDirection: Direction?
    private var collectedDirection: Direction?

    /// Carries a direction change to the loop that collects, since the use case's flow is cold and cannot take a new
    /// direction once started. Never displayed, so it takes no part in observation.
    @ObservationIgnored private var directionChange: AsyncStream<Direction>.Continuation?

    /// Takes the use cases from the graph, or from `SharedDependencies.shared.preview()` in previews and tests.
    public init(lineId: Int32, useCases: any UseCases = SharedDependencies.shared) {
        self.lineId = lineId
        self.useCases = useCases
    }

    public func onEvent(_ event: any LineDetailEvent) {
        switch onEnum(of: event) {
        case .retry:
            state = LineDetailUiStateLoading.shared
            stopArrivals = [:]
            lastArrivals = []
            arrivalsDirection = nil
            requestedDirection = nil
            // A new `observe()` collects again, so the direction it was collecting must not count as one already
            // asked for.
            collectedDirection = nil
            attempt += 1
        case .setFavourite(let favourite):
            // FR-LINE-09: the toggle changes through the use case's flow.
            let previous = favouriteWrite
            favouriteWrite = Task { [useCases, lineId] in
                await previous?.value
                let toggle = try? await useCases.setFavouriteLine().invoke(
                    lineId: lineId, isFavourite: favourite.isFavourite
                )
                if let limit = limitReached(by: toggle) { self.favouriteLimit = limit }
            }
        case .favouriteLimitShown:
            favouriteLimit = nil
        case .selectDirection(let selected):
            // The patterns are cached: a new direction maps the same result again, and no line use case runs. Its
            // calls are a collection of their own, asked for below.
            selectedDirection = selected.direction
            show()
        }
    }

    /// Runs while the screen shows: the view calls it from `.task {}`, which cancels it when the screen leaves, and
    /// with it the arrivals collection, so nothing polls once the detail is gone (FR-LINE-05, spec §8.6).
    public func observe() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observeLine() }
            group.addTask { await self.observePreferences() }
            group.addTask { await self.observeArrivals() }
        }
    }

    private func observeLine() async {
        for await result in useCases.observeLinesWithFavourites().invoke() {
            lastResult = result
            show()
        }
    }

    /// FR-SET-01: the user's ETA format, from Settings' own flow. A change keeps the calls and maps them again.
    private func observePreferences() async {
        for await preferences in useCases.observePreferences().invoke() {
            let changed = preferences.etaFormat != etaFormat
            etaFormat = preferences.etaFormat
            if changed { mapArrivals() }
        }
    }

    /// FR-LINE-05: the calls of the shown direction, collected while the screen shows. The first collection starts as
    /// soon as the line gives its direction, and each later change cancels the running one and starts another.
    private func observeArrivals() async {
        let (changes, change) = AsyncStream<Direction>.makeStream()
        directionChange = change
        defer {
            directionChange = nil
            change.finish()
        }
        // A new loop starts a new collection: whatever the last one was asking for ended with it, so the direction
        // that comes back is this loop's first.
        collectedDirection = nil
        // A direction known before this loop started, such as a line that arrived while it was being set up, or a
        // screen that left and came back.
        if let requestedDirection { change.yield(requestedDirection) }
        var observation: Task<Void, Never>?
        defer { observation?.cancel() }
        for await direction in changes {
            // The stream may carry a direction twice, once from the line's mapping and once from this loop's own
            // first read; a collection already asking for it is not restarted.
            guard direction != collectedDirection else { continue }
            collectedDirection = direction
            observation?.cancel()
            observation = Task { await self.collectArrivals(direction) }
        }
    }

    /// FR-LINE-05: one collection of the direction's calls. A failure keeps the line and its stops on screen and drops
    /// the ETAs to "–": the repository has logged it, and the detail's own Retry is the action that asks again.
    private func collectArrivals(_ direction: Direction) async {
        // The calls of the direction shown before belong to it, never to this one.
        lastArrivals = []
        arrivalsDirection = direction
        mapArrivals()
        for await result in useCases.observeLineArrivals().invoke(lineId: lineId, direction: direction) {
            guard !Task.isCancelled else { return }
            switch onEnum(of: result) {
            case .success(let success):
                // Kotlin's `List<Arrival>` bridges as an array of the Kotlin class.
                lastArrivals = (success.value as? [Arrival]) ?? []
            case .failure:
                lastArrivals = []
            }
            mapArrivals()
        }
    }

    private func show() {
        guard let result = lastResult else { return }
        state = LineDetailUiMapper.shared.state(result: result, lineId: lineId, direction: selectedDirection)
        mapArrivals()
        guard case .content(let content) = onEnum(of: state) else { return }
        requestedDirection = content.direction
        directionChange?.yield(content.direction)
    }

    /// LB-105 (FR-LINE-05): `arrivalsByStop` decides which call each row shows and how it reads, from the clock the
    /// app counts by (spec §8.6). Nothing here filters by line: the use case was asked for this line and direction,
    /// and a direction whose calls have not arrived yet shows none rather than the other's.
    private func mapArrivals() {
        guard case .content(let content) = onEnum(of: state) else {
            stopArrivals = [:]
            return
        }
        let calls = content.direction == arrivalsDirection ? lastArrivals : []
        let rows = arrivalsByStop(
            stops: content.stops, arrivals: calls, now: useCases.clock().now(), format: etaFormat
        )
        // Read one stop at a time rather than iterating the shared map: a `Map<Int, T>` reaches Swift as an
        // `NSDictionary`, and its own iterator's force-cast of each key crashes. A lookup by `KotlinInt` does not.
        var arrivals: [Int32: StopArrivalUi] = [:]
        for stop in content.stops {
            if let row = rows[KotlinInt(value: stop.stopId)] { arrivals[stop.stopId] = row }
        }
        stopArrivals = arrivals
    }
}
