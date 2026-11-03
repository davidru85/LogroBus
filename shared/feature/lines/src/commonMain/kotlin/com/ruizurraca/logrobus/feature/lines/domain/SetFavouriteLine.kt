package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent
import com.ruizurraca.logrobus.core.telemetry.AnalyticsEvent.FavouriteAdd.Kind
import com.ruizurraca.logrobus.core.telemetry.AnalyticsTracker
import com.ruizurraca.logrobus.favourites.domain.FAVOURITES_PER_TYPE
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
    ): AppResult<FavouriteToggle>
}

/** What a favourite toggle did. */
sealed interface FavouriteToggle {
    data object Done : FavouriteToggle

    data class LimitReached(
        val limit: Int,
    ) : FavouriteToggle
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
    ): AppResult<FavouriteToggle> {
        if (!isFavourite) {
            return when (val removed = favourites.remove(lineId)) {
                is AppResult.Success -> AppResult.Success(FavouriteToggle.Done)
                is AppResult.Failure -> removed
            }
        }
        return when (val saved = favourites.add(lineId)) {
            is AppResult.Success -> {
                when (saved.value) {
                    FavouriteAddition.ADDED -> {
                        analytics.track(AnalyticsEvent.FavouriteAdd(Kind.LINE))
                        AppResult.Success(FavouriteToggle.Done)
                    }

                    FavouriteAddition.ALREADY_SAVED -> {
                        AppResult.Success(FavouriteToggle.Done)
                    }

                    FavouriteAddition.LIMIT_REACHED -> {
                        AppResult.Success(FavouriteToggle.LimitReached(FAVOURITES_PER_TYPE))
                    }
                }
            }

            is AppResult.Failure -> {
                saved
            }
        }
    }
}
