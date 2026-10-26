package com.ruizurraca.logrobus.core.location

import android.Manifest
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** FR-LOC-01 and FR-LOC-03 on Android: the Fused Location Provider behind [LocationProvider]. */
@RunWith(RobolectricTestRunner::class)
class FusedLocationSourceTest {
    private val application = RuntimeEnvironment.getApplication()

    /** Records the calls that the source makes; any other call fails the test. */
    private class FakeFusedClient {
        val requests = mutableListOf<Triple<LocationRequest, LocationCallback, Looper>>()
        val removed = mutableListOf<LocationCallback>()

        val client =
            Proxy.newProxyInstance(
                FusedLocationProviderClient::class.java.classLoader,
                arrayOf(FusedLocationProviderClient::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "requestLocationUpdates" -> {
                        requests += Triple(args[0] as LocationRequest, args[1] as LocationCallback, args[2] as Looper)
                        Tasks.forResult(null)
                    }

                    "removeLocationUpdates" -> {
                        removed += args[0] as LocationCallback
                        Tasks.forResult(null)
                    }

                    else -> {
                        error("Unexpected call: ${method.name}")
                    }
                }
            } as FusedLocationProviderClient
    }

    private fun location(accuracy: Float?) =
        Location("fused").apply {
            latitude = 42.4650
            longitude = -2.4456
            accuracy?.let { this.accuracy = it }
        }

    @Test
    fun lb004_androidPermissionFollowsTheGrantedLocationPermissions() {
        val source = FusedLocationSource(application, FakeFusedClient().client)
        val seen = mutableListOf(source.permission())

        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        seen += source.permission()
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        seen += source.permission()

        assertEquals(
            listOf(LocationPermission.NotGranted, LocationPermission.Approximate, LocationPermission.Precise),
            seen,
        )
    }

    @Test
    fun lb004_androidUpdatesComeFromTheFusedProviderAndStopOnClose() {
        val fake = FakeFusedClient()
        val fixes = mutableListOf<LocationFix>()

        val updates = FusedLocationSource(application, fake.client).start { fixes += it }

        val (request, callback, looper) = fake.requests.single()
        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, request.priority)
        assertEquals(5_000L, request.intervalMillis)
        assertEquals(10f, request.minUpdateDistanceMeters)
        assertSame(Looper.getMainLooper(), looper)

        callback.onLocationResult(LocationResult.create(listOf(location(accuracy = 15f), location(accuracy = null))))
        assertEquals(
            listOf(LocationFix(GeoPoint(42.4650, -2.4456), 15.0), LocationFix(GeoPoint(42.4650, -2.4456), null)),
            fixes,
        )

        updates.close()
        assertEquals(listOf(callback), fake.removed)
    }
}
