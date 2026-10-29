package com.ruizurraca.logrobus.android.app

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination

/** The root of a tab until its feature lands (LB-101, LB-201, LB-301): its top app bar alone. */
@Composable
internal fun TabRootPlaceholder(
    destination: TopLevelDestination,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(destination.labelRes)) }) },
    ) {}
}
