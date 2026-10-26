package com.ruizurraca.logrobus.core.location

import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** FR-LOC-01 and FR-LOC-03 on iOS: Core Location behind [LocationProvider]. */
@OptIn(ExperimentalForeignApi::class) // CLLocationCoordinate2DMake
class CoreLocationSourceTest {
    /** Records start and stop instead of asking the simulator for locations. */
    private class RecordingManager : CLLocationManager() {
        var starts = 0
        var stops = 0

        override fun startUpdatingLocation() {
            starts++
        }

        override fun stopUpdatingLocation() {
            stops++
        }
    }

    private fun location(horizontalAccuracy: Double) =
        CLLocation(
            coordinate = CLLocationCoordinate2DMake(42.4650, -2.4456),
            altitude = 0.0,
            horizontalAccuracy = horizontalAccuracy,
            verticalAccuracy = -1.0,
            timestamp = NSDate(),
        )

    @Test
    fun lb004_iosPermissionFollowsTheAuthorizationAndItsAccuracy() {
        val full = CLAccuracyAuthorization.CLAccuracyAuthorizationFullAccuracy
        val reduced = CLAccuracyAuthorization.CLAccuracyAuthorizationReducedAccuracy

        listOf(kCLAuthorizationStatusNotDetermined, kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted)
            .forEach { assertEquals(LocationPermission.NotGranted, permissionOf(it, full)) }
        assertEquals(LocationPermission.Approximate, permissionOf(kCLAuthorizationStatusAuthorizedWhenInUse, reduced))
        assertEquals(LocationPermission.Precise, permissionOf(kCLAuthorizationStatusAuthorizedWhenInUse, full))
        assertEquals(LocationPermission.Precise, permissionOf(kCLAuthorizationStatusAuthorizedAlways, full))
    }

    @Test
    fun lb004_iosUpdatesComeFromCoreLocationAndStopOnClose() {
        val manager = RecordingManager()
        val fixes = mutableListOf<LocationFix>()

        val updates = CoreLocationSource(newManager = { manager }).start { fixes += it }

        assertEquals(1, manager.starts)
        assertEquals(kCLLocationAccuracyNearestTenMeters, manager.desiredAccuracy)
        assertEquals(10.0, manager.distanceFilter)

        manager.delegate!!.locationManager(manager, didUpdateLocations = listOf(location(15.0), location(-1.0)))
        assertEquals(listOf(LocationFix(GeoPoint(42.4650, -2.4456), 15.0)), fixes)

        updates.close()
        assertEquals(1, manager.stops)
        assertNull(manager.delegate)
    }
}
