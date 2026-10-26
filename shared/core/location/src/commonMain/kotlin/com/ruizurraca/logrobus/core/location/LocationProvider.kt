package com.ruizurraca.logrobus.core.location

import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration.Companion.seconds

/** The foreground location permission (FR-LOC-01). The user may grant approximate location only. */
enum class LocationPermission {
    NotGranted,
    Approximate,
    Precise,
}

/**
 * One position report, with its horizontal accuracy in metres when the platform gives one. Its `toString` never
 * prints the coordinates, because [GeoPoint]'s does not (NFR-PRIV-02).
 */
data class LocationFix(
    val point: GeoPoint,
    val accuracyMeters: Double?,
)

/**
 * The device's location, used on the device only (NFR-PRIV-02). Asking for the permission is left to the UI (LB-201).
 */
interface LocationProvider {
    fun permission(): LocationPermission

    /**
     * Location fixes while collected: updates start with the collection and stop when it is cancelled, so they stop
     * when no visible screen needs them (FR-LOC-03). A slow collector gets the newest fix. Without permission, the
     * flow completes without starting updates.
     */
    fun locations(): Flow<LocationFix>
}

/** How often a collected [LocationProvider] asks for a new fix, at most, where the platform supports it (Android). */
internal val LOCATION_UPDATE_INTERVAL = 5.seconds

/** The distance, in metres, the device must move before a new fix is delivered. */
internal const val LOCATION_UPDATE_DISTANCE_METERS = 10.0

/** What each platform provides behind [LocationProvider]: the Fused Location Provider, or Core Location. */
internal interface LocationSource {
    fun permission(): LocationPermission

    /** Starts location updates, delivered to [onFix] on the main thread, and returns what stops them. */
    fun start(onFix: (LocationFix) -> Unit): AutoCloseable
}

/** The shared half of each platform's [LocationProvider]: updates start and stop on [mainDispatcher]. */
internal class SourceLocationProvider(
    private val source: LocationSource,
    private val mainDispatcher: CoroutineDispatcher,
) : LocationProvider {
    override fun permission(): LocationPermission = source.permission()

    override fun locations(): Flow<LocationFix> =
        callbackFlow {
            if (source.permission() == LocationPermission.NotGranted) {
                close()
                return@callbackFlow
            }
            val updates = source.start { trySend(it) }
            awaitClose { updates.close() }
        }.buffer(Channel.CONFLATED)
            .flowOn(mainDispatcher)
}

internal fun locationPermission(
    granted: Boolean,
    precise: Boolean,
): LocationPermission =
    when {
        !granted -> LocationPermission.NotGranted
        precise -> LocationPermission.Precise
        else -> LocationPermission.Approximate
    }

/** A fix from platform values, or null when [accuracyMeters] is negative, which marks invalid coordinates. */
internal fun locationFixOrNull(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Double?,
): LocationFix? =
    if (accuracyMeters != null && accuracyMeters < 0) {
        null
    } else {
        LocationFix(GeoPoint(latitude, longitude), accuracyMeters)
    }
