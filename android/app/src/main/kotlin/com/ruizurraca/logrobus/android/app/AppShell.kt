package com.ruizurraca.logrobus.android.app

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.ruizurraca.logrobus.android.core.designsystem.Tokens
import com.ruizurraca.logrobus.android.core.designsystem.rememberScreenTransitions
import com.ruizurraca.logrobus.android.core.navigation.AboutDataKey
import com.ruizurraca.logrobus.android.core.navigation.LinesKey
import com.ruizurraca.logrobus.android.core.navigation.LocalScrollToTopRequests
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
    val navigationSuiteType = navigationSuiteType(currentWindowAdaptiveInfo())
    NavigationSuiteScaffold(
        navigationItems = {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == navigator.selected
                NavigationSuiteItem(
                    selected = isSelected,
                    onClick = { navigator.select(destination) },
                    icon = { Icon(painterResource(destination.iconRes(isSelected)), contentDescription = null) },
                    label = { TabLabel(stringResource(destination.labelRes), navigationSuiteType.shrinksTabLabels) },
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
        val transitions = rememberScreenTransitions()
        NavDisplay(
            entries = navigator.shownTabs.flatMap { entries.getValue(it) },
            transitionSpec = { transitions.crossFade() },
            popTransitionSpec = { transitions.crossFade() },
            predictivePopTransitionSpec = { transitions.crossFade() },
            onBack = navigator::goBack,
        )
    }
}

/**
 * FR-NAV-05 goes by width alone: a short bar on compact widths, and a collapsed wide rail from the medium width up.
 * Material's recommendation would keep a bar on a short window, such as a phone in landscape, or on a tabletop posture.
 */
internal fun navigationSuiteType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType =
    if (adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        NavigationSuiteType.WideNavigationRailCollapsed
    } else {
        NavigationSuiteType.ShortNavigationBarCompact
    }

/** A bar's height is fixed, so its labels shrink to fit; a rail's items grow downwards, so its labels wrap instead. */
internal val NavigationSuiteType.shrinksTabLabels: Boolean
    get() = this in BARS

/**
 * A tab label. In a bar, one line that shrinks to fit its tab but never below its size at 100 % font: the bar's height
 * is fixed, so at 200 % a label that wrapped would draw over its neighbours (NFR-A11Y-03). In a rail, up to two lines
 * at full size.
 */
@Composable
private fun TabLabel(
    text: String,
    shrinkToFit: Boolean,
) {
    if (shrinkToFit) {
        val size = LocalTextStyle.current.fontSize
        val smallest = size / LocalDensity.current.fontScale.coerceAtLeast(1f)
        Text(
            text = text,
            // Keeps a gap between neighbouring labels that fill their tabs.
            modifier = Modifier.padding(horizontal = Tokens.Spacing.xs),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = smallest, maxFontSize = size, stepSize = LABEL_STEP),
        )
    } else {
        Text(text = text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The screen of each key on [tab]'s stack. The content key carries the tab, because More, Settings and About data
 * can be on two stacks at once and must not share their state.
 */
private fun screens(
    tab: TopLevelDestination,
    navigator: TabNavigator,
): (NavKey) -> NavEntry<NavKey> {
    val onTab: (NavKey) -> Any = { "${tab.name}/$it" }
    val openMore = { navigator.open(MoreKey) }
    // A tab root hears its tab's re-selections, to scroll to its top.
    val root: @Composable (@Composable () -> Unit) -> Unit = { content ->
        CompositionLocalProvider(
            LocalScrollToTopRequests provides navigator.scrollToTopRequests(tab),
            content = content,
        )
    }
    return entryProvider {
        entry<LinesKey>(onTab) { root { TabRootPlaceholder(TopLevelDestination.LINES, onOpenMore = openMore) } }
        entry<NearMeKey>(onTab) { root { TabRootPlaceholder(TopLevelDestination.NEAR_ME, onOpenMore = openMore) } }
        entry<PlannerKey>(onTab) { root { TabRootPlaceholder(TopLevelDestination.PLANNER, onOpenMore = openMore) } }
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

// Fine enough that a shrunk label loses no more than it must.
private val LABEL_STEP = 0.5.sp

private val BARS =
    setOf(
        NavigationSuiteType.ShortNavigationBarCompact,
        NavigationSuiteType.ShortNavigationBarMedium,
        NavigationSuiteType.NavigationBar,
    )
