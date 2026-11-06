package com.ruizurraca.logrobus.feature.planner.domain

import com.ruizurraca.logrobus.core.location.LocationFix
import com.ruizurraca.logrobus.core.location.LocationPermission
import com.ruizurraca.logrobus.core.location.LocationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** FR-PLAN-11: a new plan starts from the user's location when the app may locate them, and from nowhere otherwise. */
class ReadInitialOriginTest {
    @Test
    fun lb301_withLocationPermission_approximateOrPrecise_theOriginIsMyLocation() {
        listOf(LocationPermission.Approximate, LocationPermission.Precise).forEach { permission ->
            val origin = DefaultReadInitialOrigin(FakeLocationProvider(permission))()

            assertEquals(PlanPlace.MyLocation, origin, "with $permission")
        }
    }

    @Test
    fun lb301_withoutLocationPermission_thereIsNoOrigin() {
        assertNull(DefaultReadInitialOrigin(FakeLocationProvider(LocationPermission.NotGranted))())
    }

    @Test
    fun lb301_readingTheOrigin_startsNoLocationUpdates() {
        // The root only shows "My location": the fix is read when a plan is requested (LB-306), never before.
        val provider = FakeLocationProvider(LocationPermission.Precise)

        DefaultReadInitialOrigin(provider)()

        assertEquals(0, provider.updateRequests)
    }

    private class FakeLocationProvider(
        private val permission: LocationPermission,
    ) : LocationProvider {
        var updateRequests = 0
            private set

        override fun permission() = permission

        override fun locations(): Flow<LocationFix> {
            updateRequests++
            return emptyFlow()
        }
    }
}
