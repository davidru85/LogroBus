package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.color.onColorFor
import com.ruizurraca.logrobus.core.common.geo.BoundingBox
import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import com.ruizurraca.logrobus.core.common.geo.distanceMetersTo
import com.ruizurraca.logrobus.feature.nearme.domain.SearchStops
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.DefaultObserveStopDetail
import com.ruizurraca.logrobus.feature.stops.domain.FetchedArrivals
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopArrivals
import com.ruizurraca.logrobus.feature.stops.domain.ObserveStopDetail
import com.ruizurraca.logrobus.transit.data.StopArrivalsReader
import com.ruizurraca.logrobus.transit.data.StopCallsAnswer
import com.ruizurraca.logrobus.transit.data.SyncStatusRepositoryImpl
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.ArrivalsRepository
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import com.ruizurraca.logrobus.transit.domain.LinePattern
import com.ruizurraca.logrobus.transit.domain.LineStop
import com.ruizurraca.logrobus.transit.domain.LinesRepository
import com.ruizurraca.logrobus.transit.domain.NearbyStopRow
import com.ruizurraca.logrobus.transit.domain.NearbyStopsSource
import com.ruizurraca.logrobus.transit.domain.ServedLine
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import com.ruizurraca.logrobus.transit.domain.Stop
import com.ruizurraca.logrobus.transit.domain.StopRepository
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import com.ruizurraca.logrobus.transit.domain.WalkDistance
import com.ruizurraca.logrobus.transit.domain.WalkDistances
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.time.Duration.Companion.minutes

/**
 * Sample lines for previews, Swift tests and debug builds, until LB-008 part 2 downloads the real catalogue (owner
 * decision, 2026-11-02). They have the network's shape: its codes, no line 8, and lines 4 and B1, and 9 and B2, sharing
 * a colour (DD-08). Their ids, names and colours are placeholders, not the source's (OQ-11). Line 11 is not
 * operational, so its status chip shows (FR-LINE-02). Each heads for each end of its name, outbound first, so a
 * favourite card shows both headsigns (FR-LINE-13).
 */
internal object SampleLines {
    private const val RED = 0xFFE30613
    private const val BLUE = 0xFF1565C0

    /**
     * LB-105: the stop ids of a sample line, derived from its own: each direction gets a block of [STOPS_PER_DIRECTION],
     * the outbound one first, so a call scripted for one direction can never land on the other's stop.
     */
    const val STOPS_PER_DIRECTION = 100
    const val INBOUND_OFFSET = 50

    val all: List<Line> =
        listOf(
            line(1, "1", "Lardero - Hospital San Pedro", 0xFF0FDCFC),
            line(2, "2", "Yagüe - Varea", 0xFF2E7D32),
            line(3, "3", "Campillo - El Cubo", 0xFFF9A825),
            line(4, "4", "Casa Blanca - Yagüe", RED),
            line(5, "5", "Los Lirios - Cascajos", 0xFF6A1B9A),
            line(6, "6", "Valdegastea - Polígono Cantabria", 0xFFEF6C00),
            line(7, "7", "Ayuntamiento - La Estrella", 0xFF8D6E63),
            line(9, "9", "Puente Madre - IES", BLUE),
            line(10, "10", "El Arco - Hospital San Pedro", 0xFFAD1457),
            line(11, "11", "Artesanos - Manuel de Falla", 0xFF00897B, isOperational = false),
            line(101, "B1", "Varea - La Cava", RED),
            line(102, "B2", "Yagüe - Lardero", BLUE),
            line(103, "B3", "Valdegastea - El Cubo", 0xFF37474F),
        )

    private fun line(
        id: Int,
        code: String,
        displayName: String,
        color: Long,
        isOperational: Boolean = true,
    ) = Line(
        id = id,
        code = code,
        displayName = displayName,
        color = color,
        onColor = onColorFor(color),
        kind = if (code.startsWith("B")) LineKind.NIGHT else LineKind.DAY,
        isOperational = isOperational,
        // "Lardero - Hospital San Pedro" runs towards Hospital San Pedro, then back towards Lardero.
        headsigns = displayName.split(" - ").reversed(),
        // LB-104, LB-105: both directions with their ends as stops, in the order a rider counts them, so the detail's
        // toggle, its ordered list and a preview's scripted arrivals all have something to show. The ids are the
        // preview's own (OQ-11), derived from the line's, and each direction's stop ids are its own too.
        patterns =
            mapOf(
                Direction.OUTBOUND to
                    LinePattern(
                        headsign = displayName.split(" - ").last(),
                        stops = stops(id * STOPS_PER_DIRECTION, displayName.split(" - ")),
                    ),
                Direction.INBOUND to
                    LinePattern(
                        headsign = displayName.split(" - ").first(),
                        stops = stops(id * STOPS_PER_DIRECTION + INBOUND_OFFSET, displayName.split(" - ").reversed()),
                    ),
            ),
    )

    /** [names] as the pattern's stops, numbered from [base]: a placeholder in Logroño, which no preview reads. */
    private fun stops(
        base: Int,
        names: List<String>,
    ) = names.mapIndexed { index, name ->
        // LB-203: stops are spread out, so a "Near you" preview shows a real order and real distances rather than
        // every stop sitting on one point. The step comes from the stop's own id, which is unique, so no two stops
        // share a point and no two distances tie. One id is about 0.7 m, which puts the five nearest sample stops
        // between 70 m and 330 m, well inside FR-STOP-11's 800 m.
        LineStop(
            stopId = base + index,
            name = name,
            lat = 42.4650 + (base + index) * 0.000006,
            lng = -2.4456,
        )
    }
}

/** The sample lines as a repository, for a preview's or a screen test's scripted graph (LB-008 part 2). */
private object SampleLinesRepository : LinesRepository {
    override fun observeLines(): Flow<AppResult<List<Line>>> = flowOf(AppResult.Success(SampleLines.all))
}

/**
 * The sample lines' calls, for a preview's or a screen test's scripted graph (LB-105). Each line's outbound first stop
 * has one in four minutes, and its second in nine and low-confidence, so a detail has an ETA and DD-09's "~" to show
 * without a source. The instants are read from the clock when the call is collected, so a row reads "in 4 min" rather
 * than a time that has passed. It filters by line and direction as the real repository does.
 */
private class SampleArrivalsRepository(
    private val clock: AppClock,
) : ArrivalsRepository {
    override fun observeLineArrivals(
        lineId: Int,
        direction: Direction?,
    ): Flow<AppResult<List<Arrival>>> =
        flowOf(
            AppResult.Success(
                SampleLines.all
                    .filter { it.id == lineId }
                    .flatMap { line -> line.patterns.entries.filter { direction == null || it.key == direction } }
                    .flatMapIndexed { index, (patternDirection, pattern) ->
                        pattern.stops.take(2).mapIndexed { stopIndex, stop ->
                            Arrival(
                                lineId = lineId,
                                stopId = stop.stopId,
                                direction = patternDirection,
                                expectedAt = clock.now() + (4 + 5 * stopIndex + index).minutes,
                                isLowConfidence = stopIndex == 1,
                            )
                        }
                    },
            ),
        )
}

/**
 * The sample lines' stops, for a preview's or a screen test's scripted graph (LB-205). A stop the sample lines serve
 * comes back with the lines that reach it and the headsign of each pattern, as the real repository's join gives; a
 * stop no sample line serves is the data failure the screen shows.
 */
private object SampleStopsRepository : StopRepository {
    override fun observeStop(stopId: Int): Flow<AppResult<Stop>> {
        val calls =
            SampleLines.all.flatMap { line ->
                line.patterns.mapNotNull { (direction, pattern) ->
                    pattern.stops
                        .firstOrNull { it.stopId == stopId }
                        ?.let { stop -> stop to servedLineOf(line, pattern.headsign, direction) }
                }
            }
        if (calls.isEmpty()) return flowOf(AppResult.Failure(AppError.Data))
        val (stop, _) = calls.first()
        return flowOf(
            AppResult.Success(
                Stop(
                    id = stop.stopId,
                    name = stop.name,
                    lat = stop.lat,
                    lng = stop.lng,
                    isOperational = true,
                    isProvisional = false,
                    servedLines = calls.map { (_, served) -> served },
                ),
            ),
        )
    }

    private fun servedLineOf(
        line: Line,
        headsign: String,
        direction: Direction,
    ) = ServedLine(
        lineId = line.id,
        code = line.code,
        headsign = headsign,
        direction = direction,
        color = line.color,
        onColor = line.onColor,
    )
}

/**
 * The scripted static data of a preview or a screen test, beside `SharedDependencies.preview()` (ADR-0007): the sample
 * lines, and a sync that succeeds without a source. A test starts its graph over this module instead of the transit
 * data one, so a screen shows the lines it scripted rather than the empty cache an unreachable source leaves (LB-008
 * part 2). No production graph builds it: `LogroBusApplication` and `LogroBusApp` call `initKoin`.
 */
fun previewDataModule(): Module =
    module {
        // The rest of what the real transit data module binds: a screen test reaches About data's last syncs too, and
        // a missing binding fails at the first ViewModel that asks (LB-008 part 2).
        single<SyncStatusRepository> { SyncStatusRepositoryImpl(get(), get(), get()) }
        single<StaticSyncEngine> {
            object : StaticSyncEngine {
                override suspend fun syncIfStale() = AppResult.Success(Unit)
            }
        }
        single<LinesRepository> { SampleLinesRepository }
        single<ArrivalsRepository> { SampleArrivalsRepository(get()) }
        // LB-205: stop detail's header and its use cases, over the sample lines' own stops. The real module binds the
        // calls through API-06; a scripted graph draws its sample calls instead, so no test waits on a source.
        single<StopRepository> { SampleStopsRepository }
        // LB-209 (FR-NEAR-05): a UI test's search finds the sample stops its screens show. Without this it would read
        // the real module's database search, which an empty test database never matches.
        single<SearchStops> { SampleStopSearch().searchStops() }
        factory<ObserveStopDetail> { DefaultObserveStopDetail(get()) }
        factory<ObserveStopArrivals> {
            // The calls are scripted on the same path the real module takes: the reader that polls API-06 is replaced
            // by a scripted one, so a screen test's detail has rows and its polling ends with the screen (LB-205).
            DefaultObserveStopArrivals(stops = get()) { stopId, lineIds ->
                get<StopArrivalsReader>().observeStopArrivals(stopId, lineIds).map { answer ->
                    // The reader answers with the calls and the instant they arrived; the use case takes them as its
                    // own type, as the real module converts them.
                    when (answer) {
                        is AppResult.Success -> {
                            AppResult.Success(FetchedArrivals(answer.value.arrivals, answer.value.fetchedAt))
                        }

                        is AppResult.Failure -> {
                            answer
                        }
                    }
                }
            }
        }
        single<StopArrivalsReader> { SampleStopArrivalsReader(get()) }
        // LB-203: "Near you" over the sample lines' own stops, so a screen test's section has rows without a source.
        single<NearbyStopsSource> {
            NearbyStopsSource {
                AppResult.Success(
                    SampleLines.all
                        .flatMap { line -> line.patterns.values.flatMap { pattern -> pattern.stops } }
                        .distinctBy { stop -> stop.stopId }
                        .map { stop ->
                            NearbyStopRow(stopId = stop.stopId, name = stop.name, lat = stop.lat, lng = stop.lng)
                        },
                )
            }
        }
        // LB-203: API-08 scripted, so a preview's section shows walking distances without a source. Each walk is the
        // straight line times a detour factor, so the list's order is real and its minutes are plausible.
        single<WalkDistances> {
            WalkDistances { request ->
                val here = GeoPoint(request.originLat, request.originLng)
                AppResult.Success(
                    request.destinations.map { destination ->
                        val straight = GeoPoint(destination.lat, destination.lng).distanceMetersTo(here)
                        val walk = straight * PREVIEW_WALK_DETOUR
                        WalkDistance(
                            stopId = destination.id,
                            distanceMeters = walk.toInt(),
                            durationSeconds = (walk / PREVIEW_WALKING_METERS_PER_SECOND).toInt(),
                        )
                    },
                )
            }
        }
    }

/** The walking speed a preview assumes, in metres per second: about 4.3 km/h, a plain walking pace. */
private const val PREVIEW_WALKING_METERS_PER_SECOND = 1.2

/** How much longer a walk is than the straight line, as street grids usually make it. */
private const val PREVIEW_WALK_DETOUR = 1.3

/**
 * The sample stop's calls, scripted so a screen test's detail has rows without a source (LB-205): the same shape
 * `SampleArrivalsRepository` gives a line, filtered by the stop. The instant is read when the calls are collected, so
 * the rows read "in N min" rather than a time that has passed.
 */
private class SampleStopArrivalsReader(
    private val clock: AppClock,
) : StopArrivalsReader {
    override fun observeStopArrivals(
        stopId: Int,
        lineIds: List<Int>,
    ): Flow<AppResult<StopCallsAnswer>> =
        flowOf(
            AppResult.Success(
                StopCallsAnswer(
                    arrivals =
                        SampleLines.all
                            .filter { line -> lineIds.isEmpty() || line.id in lineIds }
                            .flatMap { line ->
                                line.patterns.mapNotNull { (direction, pattern) ->
                                    val index = pattern.stops.indexOfFirst { it.stopId == stopId }
                                    if (index < 0) {
                                        null
                                    } else {
                                        Arrival(
                                            lineId = line.id,
                                            stopId = stopId,
                                            direction = direction,
                                            expectedAt = clock.now() + (4 + 5 * index).minutes,
                                            isLowConfidence = index == 1,
                                        )
                                    }
                                }
                            },
                    fetchedAt = clock.now(),
                ),
            ),
        )
}
