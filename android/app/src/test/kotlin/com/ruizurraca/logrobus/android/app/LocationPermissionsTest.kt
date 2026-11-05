package com.ruizurraca.logrobus.android.app

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FR-LOC-01 and security_privacy.md §3: the app declares foreground location, approximate and precise, so that the
 * user may grant approximate only, and never background location.
 */
@RunWith(AndroidJUnit4::class)
class LocationPermissionsTest {
    @Test
    fun lb201_theAppAsksForForegroundLocationOnly_approximateOrPrecise() {
        val location = MergedManifest.permissions.filter { "LOCATION" in it }.toSet()

        assertEquals(
            setOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            location,
        )
    }
}
