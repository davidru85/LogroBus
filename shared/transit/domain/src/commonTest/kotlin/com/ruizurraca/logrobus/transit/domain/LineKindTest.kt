package com.ruizurraca.logrobus.transit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Spec §5.1: the night (Búho) lines' codes start with B, and that is the one place the rule is written, so the
 * catalogue's parser, the stop detail's empty state and the preview data all read the same answer.
 */
class LineKindTest {
    @Test
    fun lb206_aCodeStartingWithBMakesTheLineNight() {
        assertEquals(LineKind.NIGHT, LineKind.of("B3"))
        assertEquals(LineKind.NIGHT, LineKind.of("B1"))
    }

    @Test
    fun lb206_anyOtherCodeIsADayLine() {
        assertEquals(LineKind.DAY, LineKind.of("1"))
        assertEquals(LineKind.DAY, LineKind.of("10"))
        // A code that merely contains a B is still a day line: only the first letter decides.
        assertEquals(LineKind.DAY, LineKind.of("4B"))
        assertEquals(LineKind.DAY, LineKind.of(""))
    }
}
