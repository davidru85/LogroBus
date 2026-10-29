package com.ruizurraca.logrobus.android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.ruizurraca.logrobus.android.core.navigation.LinesKey
import com.ruizurraca.logrobus.android.core.navigation.NearMeKey
import com.ruizurraca.logrobus.android.core.navigation.PlannerKey
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination

/**
 * The selected tab and one back stack per tab (FR-NAV-03, ADR-0005). Every stack starts at its tab's root and never
 * loses it.
 */
@Stable
internal class TabNavigator(
    selected: MutableState<TopLevelDestination>,
    private val stacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
) {
    var selected: TopLevelDestination by selected
        private set

    fun stack(destination: TopLevelDestination): NavBackStack<NavKey> = stacks.getValue(destination)

    /** The tabs whose stacks are shown: the start tab's under the selected one, so back from another root lands on it. */
    val shownTabs: List<TopLevelDestination>
        get() = listOf(TopLevelDestination.start, selected).distinct()

    /** Selects [destination]. Re-selecting the selected tab pops it to its root (FR-NAV-03). */
    fun select(destination: TopLevelDestination) {
        if (destination == selected) {
            stack(destination).popToRoot()
        } else {
            selected = destination
        }
    }

    /** Opens [key] on the selected tab's stack. */
    fun open(key: NavKey) {
        stack(selected).add(key)
    }

    /** Back: pops the selected tab; at the root of any tab but the start one, returns to the start tab (FR-NAV-02). */
    fun goBack() {
        val stack = stack(selected)
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else {
            selected = TopLevelDestination.start
        }
    }

    private fun NavBackStack<NavKey>.popToRoot() {
        while (size > 1) removeAt(lastIndex)
    }
}

/** A [TabNavigator] that survives configuration changes and process death, as its keys are serializable. */
@Composable
internal fun rememberTabNavigator(): TabNavigator {
    val selected = rememberSaveable { mutableStateOf(TopLevelDestination.start) }
    val stacks = TopLevelDestination.entries.associateWith { rememberNavBackStack(it.rootKey) }
    return remember(selected, stacks) { TabNavigator(selected, stacks) }
}

/** The key of each tab's root screen. */
internal val TopLevelDestination.rootKey: NavKey
    get() =
        when (this) {
            TopLevelDestination.LINES -> LinesKey
            TopLevelDestination.NEAR_ME -> NearMeKey
            TopLevelDestination.PLANNER -> PlannerKey
        }
