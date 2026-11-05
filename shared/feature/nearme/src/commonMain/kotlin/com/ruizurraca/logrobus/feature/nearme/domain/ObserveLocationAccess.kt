package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.location.LocationPermission
import com.ruizurraca.logrobus.core.location.LocationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Whether the app can locate the user (FR-LOC-01, FR-NEAR-06). */
enum class LocationAccess {
    /** No permission, or location is gone: the permission was revoked, or the platform service failed. */
    Off,

    /** The user granted foreground location, approximate or precise, and updates are running. */
    On,
}

/**
 * FR-LOC-01 to 03: whether the app can locate the user. Collected with the permission granted, it keeps the device's
 * location updates running, so collect it only while a screen that needs location is visible. An interface, because
 * Swift calls it and Swift tests replace it (architecture.md §4.6).
 */
fun interface ObserveLocationAccess {
    operator fun invoke(): Flow<LocationAccess>
}

class DefaultObserveLocationAccess(
    private val provider: LocationProvider,
) : ObserveLocationAccess {
    override fun invoke(): Flow<LocationAccess> =
        flow {
            if (provider.permission() == LocationPermission.NotGranted) {
                emit(LocationAccess.Off)
                return@flow
            }
            emit(LocationAccess.On)
            // The updates run while this collects them (FR-LOC-03). Their fixes are for the nearby stops (LB-203).
            provider.locations().collect {}
            // The provider's flow completes when location is gone for good.
            emit(LocationAccess.Off)
        }
}
