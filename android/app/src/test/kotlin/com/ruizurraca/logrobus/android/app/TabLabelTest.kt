package com.ruizurraca.logrobus.android.app

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LB-012, NFR-A11Y-03: a bar's height is fixed, so its labels shrink to fit at large font sizes; a rail's items grow
 * downwards, so its labels keep their size and wrap.
 */
class TabLabelTest {
    @Test
    fun lb012_onlyABarShrinksItsTabLabels() {
        val shrinking =
            listOf(
                NavigationSuiteType.ShortNavigationBarCompact,
                NavigationSuiteType.ShortNavigationBarMedium,
                NavigationSuiteType.WideNavigationRailCollapsed,
                NavigationSuiteType.WideNavigationRailExpanded,
            ).associateWith { it.shrinksTabLabels }

        assertEquals(listOf(true, true, false, false), shrinking.values.toList())
    }
}
