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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ruizurraca.logrobus.android.core.designsystem.LogroBusTheme
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.UpButton

/**
 * Settings, opened from More (LB-012). Its preferences come with LB-403 (FR-SET-01): until then, its top app bar
 * alone. [onBack] leaves it.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
        // Empty until the content lands; the padding keeps it clear of the bars meanwhile.
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    LogroBusTheme { SettingsScreen(onBack = {}) }
}
