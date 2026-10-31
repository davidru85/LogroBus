package com.ruizurraca.logrobus.core.common.color

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CssColorTest {
    @Test
    fun dd08_anRgbaColourFromApi01IsRead() {
        assertEquals(0xFF0FDCFC, parseCssColor("rgba(15,220,252,1)"))
    }

    @Test
    fun dd08_aHexColourFromApi07IsTheSameColour() {
        assertEquals(0xFF0FDCFC, parseCssColor("#0FDCFC"))
        assertEquals(0xFF0FDCFC, parseCssColor("#0fdcfc"))
        assertEquals(0xFF0FDCFC, parseCssColor("0FDCFC"))
    }

    @Test
    fun dd08_spacesAndRgbWithoutAlphaAreRead() {
        assertEquals(0xFF0FDCFC, parseCssColor(" rgba( 15, 220, 252, 1 ) "))
        assertEquals(0xFF0FDCFC, parseCssColor("rgb(15,220,252)"))
        assertEquals(0xFF0FDCFC, parseCssColor("rgba(15,220,252,1.0)"))
    }

    @Test
    fun dd08_aPartlyTransparentColourKeepsItsAlpha() {
        assertEquals(0x800FDCFC, parseCssColor("rgba(15,220,252,0.5)"))
        assertEquals(0x000FDCFC, parseCssColor("rgba(15,220,252,0)"))
    }

    @Test
    fun dd08_aMissingOrMalformedColourIsNull() {
        listOf(
            null,
            "",
            "red",
            "rgba(15,220)",
            "rgba(15,220,252,1,1)",
            "rgba(256,0,0,1)",
            "rgba(-1,0,0,1)",
            "rgba(15,220,252,1.5)",
            "rgba(15.5,220,252,1)",
            "rgba(a,b,c,1)",
            "#0FDCF",
            "#0FDCFCC",
            "#GGGGGG",
        ).forEach { raw -> assertNull(parseCssColor(raw), raw) }
    }
}
