package com.ruizurraca.logrobus.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Spec §5.7: an ETA is a value. A poll that brings the same ETA equals the last one, so a StateFlow does not emit it
 * again and the screen does not redraw (architecture.md §4.5).
 */
class EtaUiTest {
    private fun eta(display: EtaDisplay) =
        EtaUi(display, lowConfidence = false, scheduled = false, delay = null, cancelled = false)

    @Test
    fun lb011_theSameEtaIsEqual() {
        assertEquals(eta(EtaDisplay.Minutes(4)), eta(EtaDisplay.Minutes(4)))
        assertEquals(eta(EtaDisplay.ClockTime(9, 5)), eta(EtaDisplay.ClockTime(9, 5)))
        assertEquals(eta(EtaDisplay.Now), eta(EtaDisplay.Now))
    }

    @Test
    fun lb011_anEtaThatChangesIsNotEqual() {
        assertNotEquals(eta(EtaDisplay.Minutes(4)), eta(EtaDisplay.Minutes(3)))
        assertNotEquals(eta(EtaDisplay.ClockTime(9, 5)), eta(EtaDisplay.ClockTime(9, 6)))
        assertNotEquals(eta(EtaDisplay.Minutes(4)), eta(EtaDisplay.Minutes(4)).copy(lowConfidence = true))
        assertNotEquals<EtaDelay>(EtaDelay.Late(3), EtaDelay.Early(3))
        assertEquals(EtaDelay.Late(3), EtaDelay.Late(3))
    }
}
