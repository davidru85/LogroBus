package com.ruizurraca.logrobus.feature.nearme.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.geo.BoundingBox
import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import com.ruizurraca.logrobus.core.location.LocationFix
import com.ruizurraca.logrobus.core.location.LocationPermission
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.transit.domain.NearbyStopRow
import com.ruizurraca.logrobus.transit.domain.NearbyStopsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * LB-203 (FR-NEAR-06, FR-HOME-03, FR-LOC-03): "Near you" from one collection. The permission row and the list are fed
 * by the same flow, so the device is asked for fixes once, and the stops follow the fixes.
 */
class DefaultObserveNearYouTest {
    @Test
    fun lb203_withoutPermission_theSectionAsksForIt_andReadsNoPosition() =
        runTest {
            val provider = FakeProvider(LocationPermission.NotGranted)

            val states = observe(provider).toList()

            assertEquals(listOf(LocationAccess.Off), states.map { it.access })
            assertEquals(0, provider.collections, "no permission: the device is never asked for fixes")
        }

    @Test
    fun lb203_withPermission_theFixesFeedTheList() =
        runTest {
            val provider =
                FakeProvider(
                    LocationPermission.Precise,
                    fixes = listOf(GeoPoint(42.4650, -2.4450)),
                )

            val states = observe(provider).toList()

            val withStops = states.first { it.nearby?.stops?.isNotEmpty() == true }
            assertEquals(LocationAccess.On, withStops.access)
            assertEquals(listOf(38), withStops.nearby!!.stops.map { it.stopId })
        }

    @Test
    fun lb203_theDeviceIsAskedForFixesOnce_notOncePerSection() =
        runTest {
            val provider = FakeProvider(LocationPermission.Precise, fixes = listOf(GeoPoint(42.4650, -2.4450)))

            observe(provider).toList()

            assertEquals(1, provider.collections, "a second collector would start the platform's updates twice")
        }

    @Test
    fun lb203_whenLocationGoesForGood_theSectionSaysSo() =
        runTest {
            // The provider's flow completes and the permission is gone: revoked, not a platform failure.
            val provider =
                FakeProvider(LocationPermission.Precise, fixes = listOf(GeoPoint(42.4650, -2.4450))) {
                    it.permission = LocationPermission.NotGranted
                }

            val states = observe(provider).toList()

            assertEquals(LocationAccess.Off, states.last().access)
            assertNull(states.last().nearby)
        }

    @Test
    fun lb203_aPlatformFailureIsUnavailable_notOff() =
        runTest {
            val provider = FakeProvider(LocationPermission.Precise, fixes = listOf(GeoPoint(42.4650, -2.4450)))

            val states = observe(provider).toList()

            assertEquals(LocationAccess.Unavailable, states.last().access, "the permission is still granted")
        }

    private fun observe(provider: FakeProvider): Flow<NearYouState> {
        val source =
            NearbyStopsSource {
                AppResult.Success(
                    listOf(NearbyStopRow(stopId = 38, name = "Gonzalo de Berceo", lat = 42.4652, lng = -2.4450)),
                )
            }
        return DefaultObserveNearYou(provider, DefaultGetNearbyStops(source, walkDistances = null))()
    }

    /**
     * The device, scripted: it answers [fixes] once collected and counts the collections, so a second one is visible.
     * [onEnd] runs when the flow is done, which is how a revocation is scripted.
     */
    private class FakeProvider(
        permission: LocationPermission,
        private val fixes: List<GeoPoint> = emptyList(),
        private val onEnd: (FakeProvider) -> Unit = {},
    ) : LocationProvider {
        var permission: LocationPermission = permission
        var collections = 0
            private set

        override fun permission(): LocationPermission = permission

        override fun locations(): Flow<LocationFix> {
            collections += 1
            return flowOf(*fixes.map { LocationFix(it, accuracyMeters = 10.0) }.toTypedArray()).also { onEnd(this) }
        }
    }
}
