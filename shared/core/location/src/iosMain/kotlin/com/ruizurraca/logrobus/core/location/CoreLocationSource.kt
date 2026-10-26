package com.ruizurraca.logrobus.core.location

import com.ruizurraca.logrobus.core.common.AppDispatchers
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLErrorDomain
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * The iOS [LocationProvider], on Core Location. Create it on the main thread. Updates start and stop on the main
 * dispatcher, because Core Location delivers them on the run loop of the thread that created the manager.
 */
fun coreLocationProvider(dispatchers: AppDispatchers): LocationProvider =
    SourceLocationProvider(CoreLocationSource(), dispatchers.main)

internal class CoreLocationSource(
    private val newManager: () -> CLLocationManager = { CLLocationManager() },
) : LocationSource {
    // Created with the source, on the main thread, because permission() may be called from any thread.
    private val statusManager = newManager()

    override fun permission(): LocationPermission =
        permissionOf(statusManager.authorizationStatus, statusManager.accuracyAuthorization)

    override fun start(
        onFix: (LocationFix) -> Unit,
        onEnd: () -> Unit,
    ): AutoCloseable {
        val manager = newManager()
        // The manager holds its delegate weakly; the returned AutoCloseable keeps it alive until updates stop.
        val delegate = FixDelegate(onFix, onEnd)
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
    private val onEnd: () -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        didUpdateLocations.filterIsInstance<CLLocation>().forEach { location -> location.toFixOrNull()?.let(onFix) }
    }

    /** Also called when the manager is created; then the authorization is still granted, as updates only start so. */
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        if (permissionOf(manager.authorizationStatus, manager.accuracyAuthorization) == LocationPermission.NotGranted) {
            onEnd()
        }
    }

    /** kCLErrorDenied: the user revoked access. Other errors, such as kCLErrorLocationUnknown, are transient. */
    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        if (didFailWithError.domain == kCLErrorDomain && didFailWithError.code == kCLErrorDenied) onEnd()
    }
}

@OptIn(ExperimentalForeignApi::class) // CLLocationCoordinate2D is a C struct.
private fun CLLocation.toFixOrNull(): LocationFix? {
    val accuracy = horizontalAccuracy
    return coordinate.useContents { locationFixOrNull(latitude, longitude, accuracy) }
}
