package com.ruizurraca.logrobus.android.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination

/** Shows the label of the shared start destination (FR-NAV-02). LB-012 replaces it with the tab shell. */
@Composable
internal fun StartDestinationPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(TopLevelDestination.start.labelRes))
    }
}
