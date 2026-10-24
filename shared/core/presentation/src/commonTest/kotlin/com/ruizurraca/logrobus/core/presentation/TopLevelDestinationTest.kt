package com.ruizurraca.logrobus.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-NAV-01 and FR-NAV-02: exactly three top-level destinations, in tab order, and the app opens on Lines. */
class TopLevelDestinationTest {
    @Test
    fun lb012_thereAreThreeTopLevelDestinationsInTabOrder() {
        assertEquals(
            listOf(TopLevelDestination.LINES, TopLevelDestination.NEAR_ME, TopLevelDestination.PLANNER),
            TopLevelDestination.entries,
        )
    }

    @Test
    fun lb012_theAppOpensOnLines() {
        assertEquals(TopLevelDestination.LINES, TopLevelDestination.start)
    }
}
