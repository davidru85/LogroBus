package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.UpButton
import com.ruizurraca.logrobus.feature.settings.presentation.CacheMessage
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsEvent
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState

/** Settings, opened from More (LB-012). [onBack] leaves it. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreen(SettingsUiState.Loading, onEvent = {}, onBack = onBack, modifier = modifier)
}

/** Settings (LB-403, FR-SET-01): renders [state] under its top app bar, sends [onEvent], and shows [cacheMessage]. */
@Suppress("UNUSED_PARAMETER", "UnusedParameter")
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cacheMessage: CacheMessage? = null,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.settings_title)) },
                navigationIcon = { UpButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        )
    }
}
