package com.ruizurraca.logrobus.feature.settings.presentation

import com.ruizurraca.logrobus.core.presentation.MadridDateTime
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.transit.domain.StaticDataset

/** About data (LB-404, FR-SET-03). The data source, disclaimer and attributions are fixed text on each platform. */
sealed interface AboutDataUiState {
    data object Loading : AboutDataUiState

    data class Content(
        val lastSyncs: List<LastSyncUi>,
    ) : AboutDataUiState
}

/** A dataset with the time of its last successful sync, or null if it never synced. */
data class LastSyncUi(
    val dataset: StaticDataset,
    val lastSuccess: MadridDateTime?,
)

object AboutDataUiMapper {
    fun content(syncs: List<DatasetSync>): AboutDataUiState.Content =
        AboutDataUiState.Content(syncs.map { LastSyncUi(it.dataset, it.lastSuccess?.let(MadridDateTime::of)) })
}
