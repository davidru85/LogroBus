package com.ruizurraca.logrobus.core.location

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** FR-LOC-01 to 03 and NFR-PRIV-02: the shared half of every platform's location provider. */
@OptIn(ExperimentalCoroutinesApi::class) // UnconfinedTestDispatcher
class LocationProviderTest {
    /** Stands in for the Fused Location Provider or Core Location. */
    private class FakeLocationSource(
        var permission: LocationPermission,
    ) : LocationSource {
        val listeners = mutableListOf<(LocationFix) -> Unit>()
        val endListeners = mutableListOf<() -> Unit>()
        var starts = 0
        var stops = 0

        override fun permission() = permission

        override fun start(
            onFix: (LocationFix) -> Unit,
            onEnd: () -> Unit,
        ): AutoCloseable {
            starts++
            listeners += onFix
            endListeners += onEnd
            return AutoCloseable {
                stops++
                listeners -= onFix
                endListeners -= onEnd
            }
        }

        fun report(fix: LocationFix) = listeners.toList().forEach { it(fix) }

        fun end() = endListeners.toList().forEach { it() }
    }

    private val fix = LocationFix(GeoPoint(42.4650, -2.4456), accuracyMeters = 15.0)

    @Test
    fun lb004_locationUpdatesRunOnlyWhileTheFlowIsCollected() =
        runTest {
            val source = FakeLocationSource(LocationPermission.Precise)
            val provider = SourceLocationProvider(source, UnconfinedTestDispatcher(testScheduler))
            val locations = provider.locations()
            assertEquals(0, source.starts)

            locations.test {
                assertEquals(1, source.starts)
                source.report(fix)
                assertEquals(fix, awaitItem())
                assertEquals(0, source.stops)
            }

            assertEquals(1, source.stops)
            assertEquals(emptyList(), source.listeners)
        }

    @Test
    fun lb004_withoutPermissionTheFlowCompletesWithoutStartingUpdates() =
        runTest {
            val source = FakeLocationSource(LocationPermission.NotGranted)
            val provider = SourceLocationProvider(source, UnconfinedTestDispatcher(testScheduler))

            provider.locations().test { awaitComplete() }

            assertEquals(LocationPermission.NotGranted, provider.permission())
            assertEquals(0, source.starts)
        }

    @Test
    fun lb004_theFlowCompletesWhenThePlatformEndsUpdates() =
        runTest {
            // FR-LOC-02: a revoked permission, or a failed platform service, must reach the screen, not hang it.
            val source = FakeLocationSource(LocationPermission.Precise)
            val provider = SourceLocationProvider(source, UnconfinedTestDispatcher(testScheduler))

            provider.locations().test {
                source.end()
                awaitComplete()
            }

            assertEquals(1, source.stops)
        }

    @Test
    fun lb004_permissionIsNotGrantedApproximateOrPrecise() {
        assertEquals(LocationPermission.NotGranted, locationPermission(granted = false, precise = false))
        assertEquals(LocationPermission.NotGranted, locationPermission(granted = false, precise = true))
        assertEquals(LocationPermission.Approximate, locationPermission(granted = true, precise = false))
        assertEquals(LocationPermission.Precise, locationPermission(granted = true, precise = true))
    }

    @Test
    fun lb004_aFixWithANegativeAccuracyIsInvalidAndDropped() {
        assertNull(locationFixOrNull(42.4650, -2.4456, accuracyMeters = -1.0))
        assertEquals(fix, locationFixOrNull(42.4650, -2.4456, accuracyMeters = 15.0))
        assertEquals(fix.copy(accuracyMeters = null), locationFixOrNull(42.4650, -2.4456, accuracyMeters = null))
    }

    @Test
    fun lb004_aLocationFixDoesNotPrintItsCoordinates() {
        listOf("$fix", "${listOf(fix)}").forEach { printed ->
            assertFalse(printed.contains("42.4") || printed.contains("2.4"), printed)
        }
    }
}
