package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent.FavouriteAdd.Kind
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
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

/**
 * Saves the line last, or removes it. A line saved now counts `favourite_add` (NFR-OBS-03): its kind, never the line. A
 * line that already was a favourite, after a double tap, counts nothing.
 */
class DefaultSetFavouriteLine(
    private val favourites: FavouriteLinesRepository,
    private val analytics: AnalyticsTracker,
) : SetFavouriteLine {
    override suspend fun invoke(
        lineId: Int,
        isFavourite: Boolean,
    ): AppResult<Unit> {
        if (!isFavourite) return favourites.remove(lineId)
        return when (val saved = favourites.add(lineId)) {
            is AppResult.Success -> {
                if (saved.value == FavouriteAddition.ADDED) analytics.track(AnalyticsEvent.FavouriteAdd(Kind.LINE))
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> {
                saved
            }
        }
    }
}
