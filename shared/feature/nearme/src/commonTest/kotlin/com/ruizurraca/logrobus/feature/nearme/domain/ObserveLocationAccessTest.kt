package com.ruizurraca.logrobus.feature.nearme.domain

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.location.LocationFix
import com.ruizurraca.logrobus.core.location.LocationPermission
import com.ruizurraca.logrobus.core.location.LocationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-LOC-01 to 03: location access, and updates that run only while a visible screen collects it. */
class ObserveLocationAccessTest {
    @Test
    fun lb201_withoutPermission_accessIsOff_andLocationNeverStarts() =
        runTest {
            val provider = FakeLocationProvider(LocationPermission.NotGranted)

            DefaultObserveLocationAccess(provider)().test {
                assertEquals(LocationAccess.Off, awaitItem())
                awaitComplete()
            }
            assertEquals(0, provider.starts)
        }

    @Test
    fun lb201_withApproximateLocation_accessIsOn_andUpdatesRunOnlyWhileCollected() =
        runTest {
            val provider = FakeLocationProvider(LocationPermission.Approximate)

            DefaultObserveLocationAccess(provider)().test {
                assertEquals(LocationAccess.On, awaitItem())
                assertEquals(1, provider.running)
                // The screen leaves: its collection is cancelled.
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(0, provider.running)
        }

    @Test
    fun lb201_whenLocationIsRevoked_accessTurnsOff_andUpdatesStop() =
        runTest {
            val provider = FakeLocationProvider(LocationPermission.Precise)

            DefaultObserveLocationAccess(provider)().test {
                assertEquals(LocationAccess.On, awaitItem())
                provider.revoke()
                assertEquals(LocationAccess.Off, awaitItem())
                awaitComplete()
            }
            assertEquals(0, provider.running)
        }

    /** Counts the updates that run, as the platform providers start and stop them with the collection. */
    private class FakeLocationProvider(
        private var permission: LocationPermission,
    ) : LocationProvider {
        private val revoked = MutableStateFlow(false)
        var starts = 0
            private set
        var running = 0
            private set

        override fun permission() = permission

        // Like the platform providers: without permission it completes at once, and it completes when revoked.
        override fun locations(): Flow<LocationFix> =
            flow {
                if (permission == LocationPermission.NotGranted) return@flow
                starts++
                running++
                try {
                    revoked.first { it }
                } finally {
                    running--
                }
            }

        fun revoke() {
            permission = LocationPermission.NotGranted
            revoked.value = true
        }
    }
}
