package com.ruizurraca.logrobus.android.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.android.core.designsystem.ScreenTitle
import com.ruizurraca.logrobus.android.core.designsystem.UpButton

/**
 * More (FR-NAV-04): Settings and About data in the MVP; News, Fares, Recharge points and Info join in v1.0
 * (design_guidelines.md §2.1). Its entries belong to other features, so the shell owns it.
 */
@Composable
internal fun MoreScreen(
    onOpenSettings: () -> Unit,
    onOpenAboutData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { ScreenTitle(stringResource(R.string.more_title)) },
                navigationIcon = { UpButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_settings)) },
                modifier = Modifier.clickable(onClick = onOpenSettings),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_about_data)) },
                modifier = Modifier.clickable(onClick = onOpenAboutData),
            )
        }
    }
}
