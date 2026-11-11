package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import com.ruizurraca.logrobus.core.location.LocationPermission
import com.ruizurraca.logrobus.core.location.LocationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * FR-NEAR-06 and FR-HOME-03: what Near Me's "Near you" section needs, from one collection.
 *
 * It exists because `LocationProvider.locations()` starts the platform's updates with each collection: two collectors
 * — one for the permission row, one for the list — would ask the device for fixes twice, and stop them independently
 * (FR-LOC-03). So the section is fed by a single flow that reads the permission once, then keeps the fixes and the
 * stops together.
 */
fun interface ObserveNearYou {
    operator fun invoke(): Flow<NearYouState>
}

/** Near Me's section as the screen shows it: the access, and the stops when there is one. */
data class NearYouState(
    val access: LocationAccess,
    val nearby: NearbyStops? = null,
)

class DefaultObserveNearYou(
    private val provider: LocationProvider,
    private val nearbyStops: GetNearbyStops,
) : ObserveNearYou {
    override fun invoke(): Flow<NearYouState> =
        flow {
            if (provider.permission() == LocationPermission.NotGranted) {
                emit(NearYouState(LocationAccess.Off))
                return@flow
            }
            // The stops and the fixes share the one collection the provider gives (FR-LOC-03). A fix the platform
            // never delivers leaves the section on its empty first answer, never on a stale one.
            val positions: Flow<GeoPoint> = provider.locations().map { it.point }
            emitAll(
                nearbyStops(positions).map { stops ->
                    NearYouState(LocationAccess.On, stops)
                },
            )
            // The provider's flow completes when location is gone for good: revoked, or a platform service failure.
            val revoked = provider.permission() == LocationPermission.NotGranted
            emit(NearYouState(if (revoked) LocationAccess.Off else LocationAccess.Unavailable))
        }
}
