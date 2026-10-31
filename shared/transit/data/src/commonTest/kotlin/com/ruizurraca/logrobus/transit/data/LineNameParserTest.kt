package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlin.test.Test
import kotlin.test.assertEquals

class LineNameParserTest {
    @Test
    fun lb006_aLineNameGivesItsCodeAndItsTerminiInTitleCase() {
        assertEquals(
            LineName(code = "1", displayName = "Lardero - Hospital San Pedro", kind = LineKind.DAY),
            LineNameParser.parse("1-LARDERO-HOSPITAL SAN PEDRO"),
        )
    }

    @Test
    fun lb006_theSpecExampleWithItsOverride() {
        val overrides = CompiledDefaultsRemoteConfig().get(RemoteConfigKey.NameOverrides)
        val rawName = NameNormaliser(overrides).normalise("4-PALACIO DE CONGRESOS-PRADOVIEJOO")!!

        assertEquals("Palacio de Congresos - Pradoviejo", LineNameParser.parse(rawName).displayName)
    }

    @Test
    fun lb006_aCodeStartingWithBIsANightLine() {
        assertEquals(
            LineName(code = "B3", displayName = "Lardero - El Campillo", kind = LineKind.NIGHT),
            LineNameParser.parse("B3-LARDERO-EL CAMPILLO"),
        )
    }

    @Test
    fun lb006_spanishLinkWordsAreLowerCaseExceptFirst() {
        assertEquals(
            "La Grajera - Avenida de la Paz y del Ebro",
            LineNameParser.parse("12-LA GRAJERA-AVENIDA DE LA PAZ Y DEL EBRO").displayName,
        )
        assertEquals(
            "Los Lirios - El Arco de las Torres",
            LineNameParser.parse("7-LOS LIRIOS-EL ARCO DE LAS TORRES").displayName,
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
    fun lb006_aTerminusWithAHyphenFallsBackToTheOutboundPatternsFirstAndLastStops() {
        val termini = "Avenida de la Paz" to "Cascajos"

        assertEquals(
            LineName(code = "2", displayName = "Avenida de la Paz - Cascajos", kind = LineKind.DAY),
            LineNameParser.parse("2-AVENIDA DE LA PAZ-SAN MILLÁN-CASCAJOS", termini),
        )
    }

    @Test
    fun lb006_withoutPatternsAnUnclearNameShowsEachOfItsParts() {
        assertEquals(
            "Avenida de la Paz - San Millán - Cascajos",
            LineNameParser.parse("2-AVENIDA DE LA PAZ-SAN MILLÁN-CASCAJOS").displayName,
        )
        assertEquals(LineName(code = "13", displayName = "13", kind = LineKind.DAY), LineNameParser.parse("13"))
    }
}
