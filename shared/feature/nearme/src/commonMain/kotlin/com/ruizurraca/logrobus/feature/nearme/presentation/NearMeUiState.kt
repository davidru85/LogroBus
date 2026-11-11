package com.ruizurraca.logrobus.feature.nearme.presentation

import com.ruizurraca.logrobus.feature.nearme.domain.LocationAccess
import com.ruizurraca.logrobus.feature.nearme.domain.NearbyStops
import kotlin.math.ceil

/**
 * Near Me (spec §7.4, design_guidelines.md §3.2). Until its map (LB-202) and sheet (LB-203) land, it holds the "Near
 * you" section only (owner decision, 2026-11-05).
 */
data class NearMeUiState(
    val nearYou: NearYouUi = NearYouUi.Unknown,
)

/** The "Near you" section of Near Me's sheet. */
sealed interface NearYouUi {
    /** Until location access is known, a moment after the screen opens: nothing shows, so the row never flashes. */
    data object Unknown : NearYouUi

    /** FR-NEAR-06: without location, one row, "Turn on location to see nearby stops", opens the permission flow. */
    data object TurnOnLocation : NearYouUi

    /**
     * Location is on: the stops within reach, nearest first (FR-HOME-03). [isRefined] tells a walking distance from the
     * straight line, so the screen says which one it shows.
     */
    data class LocationOn(
        val stops: List<NearbyStopRowUi> = emptyList(),
        val isRefined: Boolean = false,
    ) : NearYouUi

    /**
     * Location is granted but does not work. LB-203 explains it in the section (FR-LOC-02); until then the section
     * shows nothing, and never a row that asks for a permission the app already has.
     */
    data object LocationUnavailable : NearYouUi
}

/**
 * One stop of "Near you" (FR-HOME-03): its name and how far it is. [walkingMinutes] is the walk's own time, which only
 * API-08 knows; until then the row says the straight-line distance instead of pretending to know the walk.
 */
data class NearbyStopRowUi(
    val stopId: Int,
    val name: String,
    val distanceMeters: Int,
    val walkingMinutes: Int? = null,
)

/** What Near Me's screen sends its ViewModel. */
sealed interface NearMeEvent {
    /**
     * The location permission may have changed: the system's prompt was answered, or the screen is back from the
     * system's settings. Location access is read again.
     */
    data object PermissionMayHaveChanged : NearMeEvent
}

/** Seconds in a minute, so the walk's rounding names what it divides by. */
private const val SECONDS_PER_MINUTE = 60.0

object NearMeUiMapper {
    fun state(
        access: LocationAccess,
        nearby: NearbyStops? = null,
    ): NearMeUiState =
        when (access) {
            LocationAccess.Off -> NearMeUiState(NearYouUi.TurnOnLocation)
            LocationAccess.On -> NearMeUiState(nearby.toUi())
            LocationAccess.Unavailable -> NearMeUiState(NearYouUi.LocationUnavailable)
        }

    /**
     * FR-HOME-03: the stops the use case gave, or an empty section until the first position arrives. A walk's duration
     * becomes whole minutes, rounded up, so a 40 s walk never reads "0 min".
     */
    private fun NearbyStops?.toUi(): NearYouUi.LocationOn =
        NearYouUi.LocationOn(
            stops =
                this?.stops.orEmpty().map { stop ->
                    NearbyStopRowUi(
                        stopId = stop.stopId,
                        name = stop.name,
                        distanceMeters = stop.distanceMeters,
                        // Rounded up, so a walk of 90 s reads 2 min and a walk under a minute reads 1: a 40 s walk
                        // that said "0 min" would read as no walk at all.
                        walkingMinutes =
                            stop.walkingDuration?.let { ceil(it.inWholeSeconds / SECONDS_PER_MINUTE).toInt() },
                    )
                },
            isRefined = this?.isRefined ?: false,
        )
}
