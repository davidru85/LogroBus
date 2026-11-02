package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LineNameParserTest {
    @Test
    fun lb006_aLineNameGivesItsCodeAndItsTerminiInTitleCase() {
        assertEquals(
            LineName(code = "1", displayName = "Lardero - Hospital San Pedro", kind = LineKind.DAY),
            LineNameParser.parse("1-LARDERO-HOSPITAL SAN PEDRO"),
        )
    }

    @Test
    fun lb006_pradoviejooShowsAsPalacioDeCongresosPradoviejo() {
        val overrides = CompiledDefaultsRemoteConfig().get(RemoteConfigKey.NameOverrides)
        val name = NameNormaliser(overrides).normalise("4-PALACIO DE CONGRESOS-PRADOVIEJOO")!!

        assertEquals("Palacio de Congresos - Pradoviejo", LineNameParser.parse(name)?.displayName)
    }

    @Test
    fun lb006_aCodeStartingWithBIsANightLine() {
        assertEquals(
            LineName(code = "B3", displayName = "Lardero - El Campillo", kind = LineKind.NIGHT),
            LineNameParser.parse("B3-LARDERO-EL CAMPILLO"),
        )
    }

    @Test
    fun lb006_aLowerCaseCodeIsUpperCased() {
        assertEquals(
            LineName(code = "B3", displayName = "Lardero - El Campillo", kind = LineKind.NIGHT),
            LineNameParser.parse("b3-LARDERO-EL CAMPILLO"),
        )
    }

    @Test
    fun lb006_aCodeMayHaveUpToThreeDigits() {
        // No fixed set of lines (AGENTS.md §5, rule 10): a line 100 would still be read.
        assertEquals("100", LineNameParser.parse("100-LARDERO-EL CAMPILLO")?.code)
    }

    @Test
    fun lb006_aNameThatStartsWithNoLineCodeIsNull() {
        listOf("-LARDERO-EL CAMPILLO", "X-LARDERO-EL CAMPILLO", "LINEA 1-LARDERO-EL CAMPILLO", "B-LARDERO", "1000-A-B")
            .forEach { name -> assertNull(LineNameParser.parse(name), name) }
    }

    @Test
    fun lb006_spanishLinkWordsAreLowerCaseExceptFirst() {
        assertEquals(
            "La Grajera - Avenida de la Paz y del Ebro",
            LineNameParser.parse("12-LA GRAJERA-AVENIDA DE LA PAZ Y DEL EBRO")?.displayName,
        )
        assertEquals(
            "Los Lirios - El Arco de las Torres",
            LineNameParser.parse("7-LOS LIRIOS-EL ARCO DE LAS TORRES")?.displayName,
        )
    }

    @Test
    fun lb006_accentedLettersAndExtraSpacesAreHandled() {
        assertEquals(
            LineName(code = "10", displayName = "Logroño - Yagüe", kind = LineKind.DAY),
            LineNameParser.parse(" 10 - LOGROÑO  -  YAGÜE "),
        )
    }

    @Test
    fun lb006_aTerminusWithAHyphenFallsBackToTheOutboundPatternsStopsAsDelivered() {
        // Stop names already come in their own case (AGENTS.md §7): spec §4.3's "Puente Madre-Hospital", or an "IES".
        val termini = "IES Comercio" to "Puente Madre-Hospital"

        assertEquals(
            LineName(code = "2", displayName = "IES Comercio - Puente Madre-Hospital", kind = LineKind.DAY),
            LineNameParser.parse("2-IES COMERCIO-PUENTE MADRE-HOSPITAL", termini),
        )
    }

    @Test
    fun lb006_withoutPatternsAnUnclearNameShowsEachOfItsParts() {
        assertEquals(
            "Avenida de la Paz - San Millán - Cascajos",
            LineNameParser.parse("2-AVENIDA DE LA PAZ-SAN MILLÁN-CASCAJOS")?.displayName,
        )
        assertEquals(LineName(code = "13", displayName = "13", kind = LineKind.DAY), LineNameParser.parse("13"))
    }
}
