package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent.FavouriteAdd.Kind
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository

/**
 * FR-LINE-15, FR-LINE-09: the favourite toggle of a line's row or detail. An interface, because Swift calls it and
 * Swift tests replace it (architecture.md §4.6).
 */
fun interface SetFavouriteLine {
    suspend operator fun invoke(
        lineId: Int,
        isFavourite: Boolean,
    ): AppResult<Unit>
}

/** Saves the line last, or removes it. A saved line counts `favourite_add` (NFR-OBS-03): its kind, never the line. */
class DefaultSetFavouriteLine(
    private val favourites: FavouriteLinesRepository,
    private val analytics: AnalyticsTracker,
) : SetFavouriteLine {
    override suspend fun invoke(
        lineId: Int,
        isFavourite: Boolean,
    ): AppResult<Unit> {
        if (!isFavourite) return favourites.remove(lineId)
        val saved = favourites.add(lineId)
        if (saved is AppResult.Success) analytics.track(AnalyticsEvent.FavouriteAdd(Kind.LINE))
        return saved
    }
}
