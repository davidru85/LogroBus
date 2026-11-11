package com.ruizurraca.logrobus.android.feature.stops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.FavouriteAddition
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * LB-208 (FR-STOP-08, FR-FAV-05): a stop's own save state and its alias, for the detail's Save action.
 *
 * A failed save leaves the row as it was and the screen offers the action again, so there is no error state here. The
 * limit is reported once, as the favourite lines' is, and only when a save was actually refused.
 */
class SavedStopViewModel(
    private val stopId: Int,
    private val saved: SavedStopRepository,
) : ViewModel() {
    /** What the detail's toolbar shows: whether the stop is saved, and under which alias. */
    val state: StateFlow<SavedStopUi> =
        saved
            .observeSavedStops()
            .map { result ->
                when (result) {
                    is AppResult.Failure -> {
                        SavedStopUi.Unknown
                    }

                    is AppResult.Success -> {
                        val row = result.value.firstOrNull { it.stopId == stopId }
                        SavedStopUi(isSaved = row != null, alias = row?.alias)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), SavedStopUi.Unknown)

    /** FR-FAV-05: set by a save the limit refused, cleared once the message has been shown. */
    private val limit = MutableStateFlow(false)

    val limitReached: StateFlow<Boolean> = limit

    /**
     * FR-STOP-08: saves the stop under [alias]. Saving a stop that is already saved keeps the alias it has, so the
     * Save action only adds; "Remove" is [remove], and a different alias is [rename], both in the Edit sheet.
     */
    fun save(alias: String?) {
        viewModelScope.launch {
            when (val result = saved.add(stopId, alias)) {
                is AppResult.Failure -> Unit
                is AppResult.Success -> limit.value = result.value == FavouriteAddition.LIMIT_REACHED
            }
        }
    }

    /** FR-STOP-08: removes the saved stop. */
    fun remove() {
        viewModelScope.launch { saved.remove(stopId) }
    }

    /** FR-STOP-08: a new alias for the saved stop. */
    fun rename(alias: String?) {
        viewModelScope.launch { saved.rename(stopId, alias) }
    }

    /** FR-FAV-05: the limit message was shown, so it is not said again. */
    fun limitMessageShown() {
        limit.value = false
    }

    private companion object {
        const val STOP_DELAY_MILLIS = 5_000L
    }
}

/**
 * Whether the stop is saved, and under which alias (FR-STOP-08). [Unknown] until the first read answers, which is when
 * the toolbar shows no Save action rather than a wrong one.
 */
data class SavedStopUi(
    val isSaved: Boolean = false,
    val alias: String? = null,
) {
    companion object {
        val Unknown = SavedStopUi()
    }
}
