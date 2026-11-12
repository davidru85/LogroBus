package com.ruizurraca.logrobus.android.feature.nearme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.favourites.domain.SavedStopRepository
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveMyStops
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopRowUi
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopsUiMapper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * LB-208 part 3 (FR-FAV-02, FR-STOP-08): the "My stops" edit sheet. It reorders, renames and deletes the saved stops,
 * and shows them in the user's order, each under its alias or the catalogue's name.
 *
 * A write that fails leaves the rows as they were and the sheet open, so the user can try again; there is no error
 * state here, as the save action has none.
 */
class MyStopsEditViewModel(
    private val saved: SavedStopRepository,
    observeMyStops: ObserveMyStops,
) : ViewModel() {
    /** The rows, in the user's order, built by the same mapper the section uses, so the two never disagree. */
    val rows: StateFlow<List<SavedStopRowUi>> =
        observeMyStops()
            .map { result ->
                when (result) {
                    is AppResult.Failure -> {
                        emptyList()
                    }

                    is AppResult.Success -> {
                        SavedStopsUiMapper.myStops(result.value.stops, result.value.names).rows
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), emptyList())

    /** FR-FAV-02: the user's new order, top to bottom. */
    fun reorder(stopIds: List<Int>) {
        viewModelScope.launch { saved.reorder(stopIds) }
    }

    /** FR-STOP-08: a new alias for a saved stop. */
    fun rename(
        stopId: Int,
        alias: String?,
    ) {
        viewModelScope.launch { saved.rename(stopId, alias) }
    }

    /** FR-STOP-08: removes a saved stop, including one the catalogue no longer holds. */
    fun delete(stopId: Int) {
        viewModelScope.launch { saved.remove(stopId) }
    }

    private companion object {
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
