package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.config.RemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.polling.PollingEngine
import com.ruizurraca.logrobus.transit.domain.ArrivalsRepository
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import kotlin.time.Duration

/**
 * The static sync over the real source, assembled for the composition root (LB-008 part 2, architecture.md §4.4).
 *
 * The engine, its source and its normaliser are all `internal` to this module: they are wiring, not a contract any
 * other module should reach for. The umbrella gets the one thing it needs, a [StaticSyncEngine], and keeps the rest
 * out of its graph. The Koin graph builds one of these per process, so the engine's mutex runs one sync at a time.
 */
class DefaultStaticSync(
    client: SourceClient,
    remoteConfig: RemoteConfig,
    database: LogroBusDatabase,
    clock: AppClock,
    dispatchers: AppDispatchers,
    logger: Logger,
) {
    /**
     * The engine of architecture.md §4.4. DD-11's overrides come from remote config on every construction, so a
     * changed override set changes the normaliser's fingerprint and an unchanged payload is renormalised, which is
     * what [StaticSyncEngine] and the sync rely on.
     */
    val engine: StaticSyncEngine =
        DefaultStaticSyncEngine(
            source = SourceStaticSource(client),
            normaliser = DefaultStaticNormaliser(remoteConfig.get(RemoteConfigKey.NameOverrides), logger),
            database = database,
            clock = clock,
            dispatchers = dispatchers,
            logger = logger,
        )
}

/**
 * LB-105: the arrivals repository over the one source client and the one polling engine, assembled for the composition
 * root. The endpoint class stays `internal`: the graph needs a repository, not the API behind it.
 */
fun arrivalsRepository(
    client: SourceClient,
    polling: PollingEngine,
    window: ArrivalsWindow,
    logger: Logger,
    patternsOf: (lineId: Int) -> Map<Direction, List<Int>> = { emptyMap() },
): ArrivalsRepository =
    ArrivalsRepositoryImpl(
        api = RealtimeApi(client, logger),
        polling = polling,
        interval = window.interval,
        previewMinutes = window.previewMinutes,
        logger = logger,
        patternsOf = patternsOf,
    )

/** How often a line's calls are polled, and how far ahead the source is asked for them (spec §5.7, §8.9). */
data class ArrivalsWindow(
    val interval: Duration,
    val previewMinutes: Int,
)

/**
 * LB-105 (DD-04): the stop ids of each of [lineId]'s patterns in sequence order, keyed by direction, which is what
 * places its calls. The catalogue stores each direction under its [key] (`outbound`, `inbound`), not under a word the
 * source sends, so the grouping reads the key; a row this version does not know is left out rather than guessed.
 */
fun patternsOfLine(
    database: LogroBusDatabase,
    lineId: Int,
): Map<Direction, List<Int>> =
    database.lineQueries
        .selectLinePatternStopIds(lineId.toLong())
        .executeAsList()
        .mapNotNull { row ->
            val direction = Direction.entries.firstOrNull { it.key == row.direction } ?: return@mapNotNull null
            direction to row.stop_id.toInt()
        }.groupBy({ (direction, _) -> direction }, { (_, stopId) -> stopId })
