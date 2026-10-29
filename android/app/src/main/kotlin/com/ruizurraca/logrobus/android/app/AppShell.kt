package com.ruizurraca.logrobus.android.app

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.ruizurraca.logrobus.android.core.navigation.AboutDataKey
import com.ruizurraca.logrobus.android.core.navigation.LinesKey
import com.ruizurraca.logrobus.android.core.navigation.MoreKey
import com.ruizurraca.logrobus.android.core.navigation.NearMeKey
import com.ruizurraca.logrobus.android.core.navigation.PlannerKey
import com.ruizurraca.logrobus.android.core.navigation.SettingsKey
import com.ruizurraca.logrobus.android.feature.settings.AboutDataRoute
import com.ruizurraca.logrobus.android.feature.settings.SettingsScreen
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination

/**
 * The app shell (LB-012): the three tabs of FR-NAV-01, opening on Lines (FR-NAV-02), in a navigation bar on compact
 * windows and a rail on wider ones (FR-NAV-05). Each tab shows its own back stack (FR-NAV-03).
 */
@Composable
internal fun AppShell(modifier: Modifier = Modifier) {
    val navigator = rememberTabNavigator()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    NavigationSuiteScaffold(
        navigationItems = {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == navigator.selected
                NavigationSuiteItem(
                    selected = isSelected,
                    onClick = { navigator.select(destination) },
                    icon = { Icon(painterResource(destination.iconRes(isSelected)), contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                    navigationSuiteType = navigationSuiteType,
                )
            }
        },
        modifier = modifier,
        navigationSuiteType = navigationSuiteType,
    ) {
        // One set of decorators per tab, so a tab's screens keep their state and ViewModels while another tab shows.
        val entries =
            TopLevelDestination.entries.associateWith { tab ->
                rememberDecoratedNavEntries(
                    backStack = navigator.stack(tab),
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    entryProvider = screens(tab, navigator),
                )
            }
        NavDisplay(
            entries = navigator.shownTabs.flatMap { entries.getValue(it) },
            onBack = navigator::goBack,
        )
    }
}

/**
 * The screen of each key on [tab]'s stack. The content key carries the tab, because More, Settings and About data can be
 * on two stacks at once and must not share their state.
 */
private fun screens(
    tab: TopLevelDestination,
    navigator: TabNavigator,
): (NavKey) -> NavEntry<NavKey> {
    val onTab: (NavKey) -> Any = { "${tab.name}/$it" }
    val openMore = { navigator.open(MoreKey) }
    return entryProvider {
        entry<LinesKey>(onTab) { TabRootPlaceholder(TopLevelDestination.LINES, onOpenMore = openMore) }
        entry<NearMeKey>(onTab) { TabRootPlaceholder(TopLevelDestination.NEAR_ME, onOpenMore = openMore) }
        entry<PlannerKey>(onTab) { TabRootPlaceholder(TopLevelDestination.PLANNER, onOpenMore = openMore) }
        entry<MoreKey>(onTab) {
            MoreScreen(
                onOpenSettings = { navigator.open(SettingsKey) },
                onOpenAboutData = { navigator.open(AboutDataKey) },
                onBack = navigator::goBack,
            )
        }
        entry<SettingsKey>(onTab) { SettingsScreen(onBack = navigator::goBack) }
        entry<AboutDataKey>(onTab) { AboutDataRoute(onBack = navigator::goBack) }
    }
}
