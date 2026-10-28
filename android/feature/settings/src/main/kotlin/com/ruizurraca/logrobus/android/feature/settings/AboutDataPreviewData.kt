package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.presentation.ErrorReason
import com.ruizurraca.logrobus.feature.settings.domain.DatasetSync
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.AboutDataUiState
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlin.time.Instant

/** One state per UX state of About data, built with the shared mapper, for previews and LB-011's screenshots. */
internal object AboutDataPreviewData {
    private val sync = Instant.parse("2026-10-28T09:30:00Z")

    val loading: AboutDataUiState = AboutDataUiState.Loading
    val synced = AboutDataUiMapper.state(AppResult.Success(StaticDataset.entries.map { DatasetSync(it, sync) }))
    val neverSynced = AboutDataUiMapper.state(AppResult.Success(StaticDataset.entries.map { DatasetSync(it, null) }))
    val error: AboutDataUiState = AboutDataUiState.Error(ErrorReason.DATA)
}

// Material's baseline light and dark schemes until LB-011's brand theme.
@Composable
private fun PreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(content = content)
    }
}

@PreviewLightDark
@Composable
private fun AboutDataLoadingPreview() {
    PreviewTheme { AboutDataScreen(AboutDataPreviewData.loading, onEvent = {}) }
}

@PreviewLightDark
@Composable
private fun AboutDataSyncedPreview() {
    PreviewTheme { AboutDataScreen(AboutDataPreviewData.synced, onEvent = {}) }
}

@PreviewLightDark
@Composable
private fun AboutDataNeverSyncedPreview() {
    PreviewTheme { AboutDataScreen(AboutDataPreviewData.neverSynced, onEvent = {}) }
}

@PreviewLightDark
@Composable
private fun AboutDataErrorPreview() {
    PreviewTheme { AboutDataScreen(AboutDataPreviewData.error, onEvent = {}) }
}
