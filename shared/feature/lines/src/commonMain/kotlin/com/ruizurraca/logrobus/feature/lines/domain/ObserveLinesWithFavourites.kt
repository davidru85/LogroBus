package com.ruizurraca.logrobus.feature.lines.domain

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteLinesRepository
import com.ruizurraca.logrobus.transit.domain.Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** FR-LINE-12: the lines, and the favourite ones' ids in the user's order. */
data class LinesWithFavourites(
    val lines: List<Line>,
    val favouriteIds: List<Int>,
)

/**
 * The Lines tab's data: [ObserveLines] with the favourite lines (LB-103). An interface, because Swift calls it and
 * Swift tests replace it (architecture.md §4.6).
 */
fun interface ObserveLinesWithFavourites {
    operator fun invoke(): Flow<AppResult<LinesWithFavourites>>
}

/**
 * Emits again whenever the lines or the favourites change, so a toggled favourite shows at once (FR-LINE-15). The
 * lines' failure is the result. Favourites that cannot be read, a damaged database the repository has logged, leave
 * the lines with none: the list still shows (spec §7.3).
 */
class DefaultObserveLinesWithFavourites(
    private val observeLines: ObserveLines,
    private val favourites: FavouriteLinesRepository,
) : ObserveLinesWithFavourites {
    override fun invoke(): Flow<AppResult<LinesWithFavourites>> =
        combine(observeLines(), favourites.observeFavouriteLineIds()) { lines, favouriteIds ->
            val ids = (favouriteIds as? AppResult.Success)?.value.orEmpty()
            when (lines) {
                is AppResult.Success -> AppResult.Success(LinesWithFavourites(lines.value, ids))
                is AppResult.Failure -> lines
            }
        }
}
