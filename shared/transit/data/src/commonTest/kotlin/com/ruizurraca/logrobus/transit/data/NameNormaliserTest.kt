package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NameNormaliserTest {
    private val pradoviejo = NameNormaliser(mapOf("PRADOVIEJOO" to "PRADOVIEJO"))

    @Test
    fun dd11_namesAreTrimmed() {
        assertEquals("Provisional Templarios", NameNormaliser(emptyMap()).normalise("Provisional Templarios "))
    }

    @Test
    fun dd11_aMissingOrBlankNameIsNull() {
        listOf(null, "", "  ").forEach { raw -> assertNull(pradoviejo.normalise(raw), raw) }
    }

    @Test
    fun dd11_anOverrideFixesATypoInsideAName() {
        assertEquals("4-PALACIO DE CONGRESOS-PRADOVIEJO", pradoviejo.normalise("4-PALACIO DE CONGRESOS-PRADOVIEJOO"))
    }

    @Test
    fun dd11_theCompiledDefaultsFixPradoviejo() {
        val overrides = CompiledDefaultsRemoteConfig().get(RemoteConfigKey.NameOverrides)

        assertEquals("PRADOVIEJO", NameNormaliser(overrides).normalise("PRADOVIEJOO "))
    }

    @Test
    fun dd11_anOverrideReplacesWholeWordsOnly() {
        val normaliser = NameNormaliser(mapOf("PRADO" to "PRADERA", "SAN" to "SANTO"))

        assertEquals("PRADOVIEJOO", normaliser.normalise("PRADOVIEJOO"))
        assertEquals("SANTA ANA", normaliser.normalise("SANTA ANA"))
        assertEquals("EL PRADERA", normaliser.normalise("EL PRADO"))
        assertEquals("LOGROÑO-PRADERA", normaliser.normalise("LOGROÑO-PRADO"))
    }

    @Test
    fun dd11_anOverrideMatchesItsExactCase() {
        assertEquals("Pradoviejoo", pradoviejo.normalise("Pradoviejoo"))
    }

    @Test
    fun dd11_anOverrideMayHoldSpacesAndPunctuation() {
        val normaliser = NameNormaliser(mapOf("AV. DE LA PAZ" to "AVENIDA DE LA PAZ", "C/" to "CALLE"))

        assertEquals("AVENIDA DE LA PAZ 12", normaliser.normalise("AV. DE LA PAZ 12"))
        assertEquals("CALLE VARA DE REY", normaliser.normalise("C/ VARA DE REY"))
    }

    @Test
    fun dd11_aLetterNextToAnAccentedOneIsPartOfTheSameWord() {
        val normaliser = NameNormaliser(mapOf("LOGRO" to "X"))

        assertEquals("LOGROÑO", normaliser.normalise("LOGROÑO"))
    }
}
