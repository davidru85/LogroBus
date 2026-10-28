package com.ruizurraca.logrobus.core.presentation

import com.ruizurraca.logrobus.core.common.AppError
import kotlin.test.Test
import kotlin.test.assertEquals

/** FR-ERR-05 and spec §7.3: an Error state names one of three reasons: network, server or data. */
class ErrorReasonTest {
    @Test
    fun lb010_eachAppErrorShowsItsReason() {
        assertEquals(ErrorReason.NETWORK, ErrorReason.of(AppError.Network))
        assertEquals(ErrorReason.SERVER, ErrorReason.of(AppError.Server(503)))
        assertEquals(ErrorReason.DATA, ErrorReason.of(AppError.Data))
    }

    @Test
    fun lb010_aMissingResourceIsADataError() {
        // A screen that treats NotFound as Empty maps it before it gets here.
        assertEquals(ErrorReason.DATA, ErrorReason.of(AppError.NotFound))
    }
}
