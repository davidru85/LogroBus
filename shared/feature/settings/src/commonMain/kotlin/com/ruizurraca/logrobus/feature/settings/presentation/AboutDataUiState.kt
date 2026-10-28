package com.ruizurraca.logrobus.feature.settings.presentation

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.transit.domain.StaticDataset

/** About data (LB-404, FR-SET-03). The data source, disclaimer and attributions are fixed text on each platform. */
sealed interface AboutDataUiState {
    data object Loading : AboutDataUiState

    data class Content(
        val lastSyncs: List<LastSyncUi>,
    ) : AboutDataUiState

    /** FR-ERR-05: the message for [reason], and Retry. */
    data class Error(
        val reason: ErrorReason,
    ) : AboutDataUiState
}

/** What About data's screen sends its ViewModel. */
sealed interface AboutDataEvent {
    /** FR-ERR-05: read again after an Error. */
    data object Retry : AboutDataEvent
}

/** A dataset with the time of its last successful sync, or null if it never synced. */
data class LastSyncUi(
    val dataset: StaticDataset,
    val lastSuccess: MadridDateTime?,
)

object AboutDataUiMapper {
    fun state(result: AppResult<List<DatasetSync>>): AboutDataUiState =
        when (result) {
            is AppResult.Success -> {
                AboutDataUiState.Content(
                    result.value.map { LastSyncUi(it.dataset, it.lastSuccess?.let(MadridDateTime::of)) },
                )
            }

            is AppResult.Failure -> {
                AboutDataUiState.Loading
            }
        }
}
