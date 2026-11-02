package com.ruizurraca.logrobus.android.app

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.ruizurraca.logrobus.android.core.navigation.AboutDataKey
import com.ruizurraca.logrobus.android.core.navigation.LineDetailKey
import com.ruizurraca.logrobus.android.core.navigation.LinesKey
import com.ruizurraca.logrobus.android.core.navigation.MoreKey
import com.ruizurraca.logrobus.core.presentation.TopLevelDestination
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LB-012, FR-NAV-03: re-selecting a tab at its root asks the root to scroll to its top; deeper in, it pops the tab to
 * its root instead.
 */
class TabNavigatorTest {
    private val navigator =
        TabNavigator(
            selected = mutableStateOf(TopLevelDestination.LINES),
            stacks = TopLevelDestination.entries.associateWith { NavBackStack(it.rootKey) },
        )

    @Test
    fun lb012_reselectingATabAtItsRootAsksItToScrollToTheTop() =
        runTest {
            navigator.scrollToTopRequests(TopLevelDestination.LINES).test {
                navigator.select(TopLevelDestination.LINES)

                awaitItem()
            }
        }

    @Test
    fun lb012_reselectingATabDeeperInPopsItToItsRoot_withoutScrolling() =
        runTest {
            navigator.open(MoreKey, from = LinesKey)
            navigator.open(AboutDataKey, from = MoreKey)

            navigator.scrollToTopRequests(TopLevelDestination.LINES).test {
                navigator.select(TopLevelDestination.LINES)

                assertEquals(listOf(LinesKey), navigator.stack(TopLevelDestination.LINES).toList())
                expectNoEvents()
            }
        }

    @Test
    fun lb103_aScreenOpensAnotherOnlyWhileItIsOnTop() {
        // A quick second tap on a row reaches the Lines root while it fades out: it must not push a second detail.
        navigator.open(LineDetailKey(10), from = LinesKey)
        navigator.open(LineDetailKey(10), from = LinesKey)
        navigator.open(LineDetailKey(3), from = LinesKey)

        assertEquals(listOf(LinesKey, LineDetailKey(10)), navigator.stack(TopLevelDestination.LINES).toList())
    }

    @Test
    fun lb012_aScreenOnTwoTabsHasTwoContentKeys() {
        // The decorators key a screen's saved state and ViewModels by its content key, and both tabs' entries are in
        // one NavDisplay, so About data on Lines and on Near Me must not share one.
        val keys = TopLevelDestination.entries.map { screens(it, navigator)(AboutDataKey).contentKey }

        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun lb012_selectingAnotherTabAsksNoTabToScroll() =
        runTest {
            turbineScope {
                val lines = navigator.scrollToTopRequests(TopLevelDestination.LINES).testIn(backgroundScope)
                val nearMe = navigator.scrollToTopRequests(TopLevelDestination.NEAR_ME).testIn(backgroundScope)

                navigator.select(TopLevelDestination.NEAR_ME)

                lines.expectNoEvents()
                nearMe.expectNoEvents()
            }
        }
}
