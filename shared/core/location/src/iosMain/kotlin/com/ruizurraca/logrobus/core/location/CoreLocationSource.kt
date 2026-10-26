package com.ruizurraca.logrobus.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineDispatcher
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.darwin.NSObject

/**
 * The iOS [LocationProvider], on Core Location. [mainDispatcher] must run on the main thread, because Core Location
 * delivers updates on the run loop of the thread that created the manager.
 */
fun coreLocationProvider(mainDispatcher: CoroutineDispatcher): LocationProvider =
    SourceLocationProvider(CoreLocationSource(), mainDispatcher)

internal class CoreLocationSource(
    private val newManager: () -> CLLocationManager = { CLLocationManager() },
) : LocationSource {
    private val statusManager by lazy { newManager() }

    override fun permission(): LocationPermission =
        permissionOf(statusManager.authorizationStatus, statusManager.accuracyAuthorization)

    override fun start(onFix: (LocationFix) -> Unit): AutoCloseable {
        val manager = newManager()
        // The manager holds its delegate weakly; the returned AutoCloseable keeps it alive until updates stop.
        val delegate = FixDelegate(onFix)
        manager.delegate = delegate
        manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        manager.distanceFilter = LOCATION_UPDATE_DISTANCE_METERS
        manager.startUpdatingLocation()
        return AutoCloseable {
            manager.stopUpdatingLocation()
            if (manager.delegate === delegate) manager.delegate = null
        }
    }
}

private val grantedStatuses = setOf(kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways)

internal fun permissionOf(
    status: CLAuthorizationStatus,
    accuracy: CLAccuracyAuthorization,
): LocationPermission =
    locationPermission(
        granted = status in grantedStatuses,
        precise = accuracy == CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy,
    )

private class FixDelegate(
    private val onFix: (LocationFix) -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        didUpdateLocations.filterIsInstance<CLLocation>().forEach { location -> location.toFixOrNull()?.let(onFix) }
    }
}

@OptIn(ExperimentalForeignApi::class) // CLLocationCoordinate2D is a C struct.
private fun CLLocation.toFixOrNull(): LocationFix? {
    val accuracy = horizontalAccuracy
    return coordinate.useContents { locationFixOrNull(latitude, longitude, accuracy) }
}
