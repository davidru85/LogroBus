package com.ruizurraca.logrobus.feature.nearme.presentation

import com.ruizurraca.logrobus.feature.nearme.domain.LocationAccess

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

    /** Location is on. The nearby stops come with LB-203; until then the section shows nothing. */
    data object LocationOn : NearYouUi

    /**
     * Location is granted but does not work. LB-203 explains it in the section (FR-LOC-02); until then the section
     * shows nothing, and never a row that asks for a permission the app already has.
     */
    data object LocationUnavailable : NearYouUi
}

/** What Near Me's screen sends its ViewModel. */
sealed interface NearMeEvent {
    /**
     * The location permission may have changed: the system's prompt was answered, or the screen is back from the
     * system's settings. Location access is read again.
     */
    data object PermissionMayHaveChanged : NearMeEvent
}

object NearMeUiMapper {
    fun state(access: LocationAccess): NearMeUiState =
        when (access) {
            LocationAccess.Off -> NearMeUiState(NearYouUi.TurnOnLocation)
            LocationAccess.On -> NearMeUiState(NearYouUi.LocationOn)
            LocationAccess.Unavailable -> NearMeUiState(NearYouUi.LocationUnavailable)
        }
}
