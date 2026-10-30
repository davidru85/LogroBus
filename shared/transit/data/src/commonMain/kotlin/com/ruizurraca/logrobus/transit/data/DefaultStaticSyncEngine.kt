package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.database.FavouriteTypes
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.Sync_state
import com.ruizurraca.logrobus.core.database.searchableName
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** The static sync of architecture.md §4.4. */
internal class DefaultStaticSyncEngine(
    private val source: StaticSource,
    private val normaliser: StaticNormaliser,
    private val database: LogroBusDatabase,
    private val clock: AppClock,
    private val dispatchers: AppDispatchers,
    private val logger: Logger,
) : StaticSyncEngine {
    private val mutex = Mutex()

    // One sync at a time: app start and the first screen can ask together, and the second then finds the cache fresh.
    override suspend fun syncIfStale(): AppResult<Unit> = mutex.withLock { failingAsData { sync() } }

    private suspend fun sync(): AppResult<Unit> {
        val synced = withContext(dispatchers.io) { StaticDataset.entries.associateWith(::syncState) }
        if (synced.values.all { it.isFresh() }) return AppResult.Success(Unit)
        return source.lines().then { lines ->
            // Hashing and normalising the payloads is CPU work, kept off the caller's thread, which may be the UI's.
            source.stops().then { stops -> withContext(dispatchers.default) { update(synced, lines, stops) } }
        }
    }

    // architecture.md §4.4: an unchanged payload is not normalised or written, but the sync succeeded.
    private suspend fun update(
        synced: Map<StaticDataset, Sync_state?>,
        lines: JsonElement,
        stops: JsonElement,
    ): AppResult<Unit> {
        val hashes = mapOf(StaticDataset.LINES to contentHash(lines), StaticDataset.STOPS to contentHash(stops))
        val unchanged = hashes.all { (dataset, hash) -> synced[dataset]?.content_hash == hash }
        return if (unchanged) {
            store(catalogue = null, hashes)
        } else {
            normaliser
                .normalise(lines, stops)
                .then(::validated)
                .then { catalogue -> store(catalogue, hashes) }
        }
    }

    /**
     * Spec §8.5's content hash, over what decides the stored catalogue: the normaliser's fingerprint, then the body.
     * The body is the source's JSON decoded and encoded again, which keeps its keys in order, so the same body always
     * hashes the same. A change in whitespace alone hashes the same too, and changes nothing that is stored.
     */
    private fun contentHash(body: JsonElement): String = sha256Hex("${normaliser.fingerprint}\n$body")

    private fun syncState(dataset: StaticDataset): Sync_state? =
        database.syncQueries.syncState(dataset.key).executeAsOneOrNull()

    // Spec §8.5: lines and stops stay fresh for 24 h after their last success. One in the future means the clock moved
    // back, so it proves nothing about the cache's age.
    private fun Sync_state?.isFresh(): Boolean {
        val age = clock.now() - (this?.last_success ?: return false)
        return age >= Duration.ZERO && age < TTL
    }

    // Spec §8.5: too few lines or stops means a broken payload, not a smaller network. Provisional stops (DD-10) come
    // from patterns, not from API-02, so they do not count; but every pattern stop needs one, or a pattern would point
    // at nothing.
    private fun validated(catalogue: StaticCatalogue): AppResult<StaticCatalogue> {
        val lines = catalogue.lines.size
        val stops = catalogue.stops.count { !it.isProvisional }
        val stopIds = catalogue.stops.mapTo(HashSet()) { it.id }
        val unlinked =
            catalogue.lines.sumOf { line ->
                line.patterns.sumOf { it.stops.count { s -> s.stopId !in stopIds } }
            }
        if (lines >= FEWEST_LINES && stops >= FEWEST_STOPS && unlinked == 0) return AppResult.Success(catalogue)
        logger.warn(
            TAG,
            "Rejected a suspicious payload with $lines lines and $stops stops; $unlinked pattern stops lack one",
        )
        return AppResult.Failure(AppError.Data)
    }

    /**
     * Replaces the cache with [catalogue], if there is one, records the sync with each dataset's hash and counts the
     * saved stops it lacks, in one transaction (NFR-REL-03). If any write fails, the transaction rolls back and the
     * cache is as it was.
     */
    private suspend fun store(
        catalogue: StaticCatalogue?,
        hashes: Map<StaticDataset, String>,
    ): AppResult<Unit> {
        val now = clock.now()
        withContext(dispatchers.io) {
            database.transaction {
                catalogue?.let(::replace)
                hashes.forEach { (dataset, hash) -> database.syncQueries.recordSync(dataset.key, now, hash) }
                database.favouriteQueries.countMissingStops(FavouriteTypes.STOP)
            }
        }
        return AppResult.Success(Unit)
    }

    /**
     * The database and normaliser boundary (code_style.md §2.2, NFR-REL-02): whatever [block] throws, a database that
     * cannot be read or written or a normaliser bug, fails this sync alone and leaves the cache as it was. Only the
     * caller's own cancellation propagates.
     */
    @Suppress("TooGenericExceptionCaught") // On purpose: see above.
    private suspend fun failingAsData(block: suspend () -> AppResult<Unit>): AppResult<Unit> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            logger.warn(TAG, "The static sync failed; the previous catalogue is kept", failure)
            AppResult.Failure(AppError.Data)
        }

    private fun replace(catalogue: StaticCatalogue) {
        with(database.lineQueries) {
            deletePatternStops()
            deletePatterns()
            deleteLines()
            catalogue.lines.forEach { line ->
                insertLine(
                    id = line.id.toLong(),
                    code = line.code,
                    raw_name = line.rawName,
                    display_name = line.displayName,
                    color = line.color,
                    on_color = line.onColor,
                    kind = line.kind.key,
                    operational = line.isOperational,
                )
                line.patterns.forEach { pattern ->
                    insertPattern(line.id.toLong(), pattern.direction.key, pattern.headsign, pattern.shapePolyline)
                    pattern.stops.forEachIndexed { seq, stop ->
                        insertPatternStop(
                            line_id = line.id.toLong(),
                            direction = pattern.direction.key,
                            seq = seq.toLong(),
                            stop_id = stop.stopId.toLong(),
                            shape_index = stop.shapeIndex.toLong(),
                        )
                    }
                }
            }
        }
        with(database.stopQueries) {
            deleteStopLines()
            deleteStops()
            catalogue.stops.forEach { stop ->
                insertStop(
                    id = stop.id.toLong(),
                    name = stop.name,
                    lat = stop.lat,
                    lng = stop.lng,
                    operational = stop.isOperational,
                    provisional = stop.isProvisional,
                    normalized_name = searchableName(stop.name),
                )
                stop.lineIds.forEach { lineId -> insertStopLine(stop.id.toLong(), lineId.toLong()) }
            }
        }
    }

    private companion object {
        const val TAG = "StaticSync"
        const val FEWEST_LINES = 5
        const val FEWEST_STOPS = 100
        val TTL = 24.hours
    }
}

/** [next] on a success's value; a failure passes through. */
private inline fun <T, R> AppResult<T>.then(next: (T) -> AppResult<R>): AppResult<R> =
    when (this) {
        is AppResult.Success -> next(value)
        is AppResult.Failure -> this
    }
