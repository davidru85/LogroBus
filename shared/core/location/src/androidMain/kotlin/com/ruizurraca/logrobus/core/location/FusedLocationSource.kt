package com.ruizurraca.logrobus.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ruizurraca.logrobus.core.common.AppDispatchers

/** The Android [LocationProvider], on the Fused Location Provider of Google Play services (`architecture.md` §5.1). */
fun fusedLocationProvider(
    context: Context,
    dispatchers: AppDispatchers,
): LocationProvider = SourceLocationProvider(FusedLocationSource(context.applicationContext), dispatchers.main)

internal class FusedLocationSource(
    private val context: Context,
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
) : LocationSource {
    override fun permission(): LocationPermission {
        val fine = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        return locationPermission(granted = fine || coarse, precise = fine)
    }

    // SourceLocationProvider starts updates only after permission() reported a grant.
    @SuppressLint("MissingPermission")
    override fun start(
        onFix: (LocationFix) -> Unit,
        onEnd: () -> Unit,
    ): AutoCloseable {
        val callback =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.forEach { location -> location.toFixOrNull()?.let(onFix) }
                }
            }
        val request =
            LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL.inWholeMilliseconds)
                .setMinUpdateDistanceMeters(LOCATION_UPDATE_DISTANCE_METERS.toFloat())
                .build()
        // A failed request, for example without Google Play services, ends the updates. Location services switched off
        // do not: the provider resumes when they are switched back on.
        client.requestLocationUpdates(request, callback, Looper.getMainLooper()).addOnFailureListener { onEnd() }
        return AutoCloseable { client.removeLocationUpdates(callback) }
    }

    private fun isGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

private fun Location.toFixOrNull(): LocationFix? =
    locationFixOrNull(latitude, longitude, accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null)
