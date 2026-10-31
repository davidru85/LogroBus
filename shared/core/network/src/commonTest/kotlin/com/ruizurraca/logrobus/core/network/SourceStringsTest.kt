package com.ruizurraca.logrobus.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceStringsTest {
    @Test
    fun dd07_anEmptyStringIsNull() {
        assertNull("".trimmedOrNull())
    }

    @Test
    fun dd07_aStringOfOnlySpacesIsNull() {
        assertNull(" \t ".trimmedOrNull())
    }

    @Test
    fun dd07_aMissingStringStaysNull() {
        assertNull(null.trimmedOrNull())
    }

    @Test
    fun dd07_anyOtherStringIsTrimmed() {
        assertEquals("Provisional Templarios", "Provisional Templarios ".trimmedOrNull())
        assertEquals("08:15:55Z", " 08:15:55Z".trimmedOrNull())
    }
}
