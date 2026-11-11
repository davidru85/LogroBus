package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.location.LocationFix
import com.ruizurraca.logrobus.core.location.LocationPermission
import com.ruizurraca.logrobus.core.location.LocationProvider
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.geo.BoundingBox
import com.ruizurraca.logrobus.feature.nearme.domain.DefaultGetNearbyStops
import com.ruizurraca.logrobus.feature.nearme.domain.DefaultObserveLocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.DefaultObserveNearYou
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveNearYou
import com.ruizurraca.logrobus.transit.domain.NearbyStopRow
import com.ruizurraca.logrobus.transit.domain.NearbyStopsSource
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveLocationAccess
import com.ruizurraca.logrobus.feature.planner.domain.DefaultReadInitialOrigin
import com.ruizurraca.logrobus.feature.planner.domain.ReadInitialOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * Near Me's location access (LB-201) and How to Get There's initial origin (LB-301) over one scripted provider, off
 * until turned on, as on a fresh install. Like the platform providers, its updates run while collected, counted in
 * [collections], and end when location is turned off, as a revoked permission ends them. [PreviewUseCases] scripts it.
 */
internal class PreviewLocation(
    private val collections: MutableStateFlow<Int>,
) : NearMeUseCases {
    private val permission = MutableStateFlow(LocationPermission.NotGranted)

    // Counted, so that updates running when location is turned off end even if it is turned on again at once.
    private val revocations = MutableStateFlow(0)

    fun turnOn() {
        permission.value = LocationPermission.Precise
    }

    fun turnOff() {
        permission.value = LocationPermission.NotGranted
        revocations.update { it + 1 }
    }

    private val provider =
        object : LocationProvider {
            override fun permission() = this@PreviewLocation.permission.value

            override fun locations(): Flow<LocationFix> =
                flow<LocationFix> {
                    if (permission() == LocationPermission.NotGranted) return@flow
                    val started = revocations.value
                    revocations.first { it > started }
                }.countedIn(collections)
        }

    override fun observeLocationAccess(): ObserveLocationAccess = DefaultObserveLocationAccess(provider)

    /** LB-203: the scripted section, over the sample stops and the preview's own position. */
    override fun observeNearYou(): ObserveNearYou =
        DefaultObserveNearYou(provider) { positions ->
            DefaultGetNearbyStops(sampleNearbyStops(), walkDistances = null)(positions)
        }

    fun readInitialOrigin(): ReadInitialOrigin = DefaultReadInitialOrigin(provider)
}

/**
 * LB-203: the stops a preview's "Near you" lists, over the sample lines' own stops, so a preview or a Swift test shows
 * a section without a catalogue or a source. The walk is left out: API-08 is the source's, and a preview scripts none.
 */
private fun sampleNearbyStops(): NearbyStopsSource =
    object : NearbyStopsSource {
        override fun stopsIn(box: BoundingBox): AppResult<List<NearbyStopRow>> =
            AppResult.Success(
                SampleLines.all
                    .flatMap { line -> line.patterns.values.flatMap { it.stops } }
                    .distinctBy { it.stopId }
                    .map { stop -> NearbyStopRow(stopId = stop.stopId, name = stop.name, lat = stop.lat, lng = stop.lng) },
            )
    }
