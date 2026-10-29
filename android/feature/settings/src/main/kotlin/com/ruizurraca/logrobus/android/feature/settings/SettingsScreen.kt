package com.ruizurraca.logrobus.android.feature.settings

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { UpButton(onClick = onBack) },
            )
        },
    ) {}
}
