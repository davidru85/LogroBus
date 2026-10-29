package com.ruizurraca.logrobus.android.app

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination

/**
 * The app shell (LB-012): the three tabs of FR-NAV-01, opening on Lines (FR-NAV-02), in a navigation bar on compact
 * windows and a rail on wider ones (FR-NAV-05).
 */
@Composable
internal fun AppShell(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf(TopLevelDestination.start) }
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    NavigationSuiteScaffold(
        navigationItems = {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                NavigationSuiteItem(
                    selected = isSelected,
                    onClick = { selected = destination },
                    icon = { Icon(painterResource(destination.iconRes(isSelected)), contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                    navigationSuiteType = navigationSuiteType,
                )
            }
        },
        modifier = modifier,
        navigationSuiteType = navigationSuiteType,
    ) {
        TabRootPlaceholder(selected)
    }
}
