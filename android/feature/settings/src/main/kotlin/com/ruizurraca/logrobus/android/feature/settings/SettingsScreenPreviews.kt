package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState

// Settings' states (spec §7.3), from SettingsPreviewData: its content, data never synced, a sync status that cannot
// be read, and the first load.

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    LogroBusTheme { SettingsScreen(SettingsPreviewData.content, onEvent = {}, onBack = {}) }
}

@PreviewLightDark
@Composable
private fun SettingsNeverSyncedPreview() {
    LogroBusTheme { SettingsScreen(SettingsPreviewData.neverSynced, onEvent = {}, onBack = {}) }
}

@PreviewLightDark
@Composable
private fun SettingsUnknownStatusPreview() {
    LogroBusTheme { SettingsScreen(SettingsPreviewData.unknown, onEvent = {}, onBack = {}) }
}

@PreviewLightDark
@Composable
private fun SettingsLoadingPreview() {
    LogroBusTheme { SettingsScreen(SettingsUiState.Loading, onEvent = {}, onBack = {}) }
}
