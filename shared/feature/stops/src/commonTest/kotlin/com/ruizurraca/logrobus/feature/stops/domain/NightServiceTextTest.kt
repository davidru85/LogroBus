package com.ruizurraca.logrobus.feature.stops.domain

import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.core.config.LocalizedText
import com.ruizurraca.logrobus.core.config.RemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * LB-206 (FR-STOP-06, OQ-08, NFR-REL-05): the night-service sentence, read from remote config and worded for the
 * language the app runs in.
 */
class NightServiceTextTest {
    private val configured =
        NightServiceTextOf(
            object : RemoteConfig {
                override fun <T : Any> get(key: RemoteConfigKey<T>): T =
                    @Suppress("UNCHECKED_CAST")
                    when (key) {
                        RemoteConfigKey.NightServiceText -> {
                            LocalizedText(es = "Nocturno en español", en = "Night in English")
                        }

                        else -> {
                            key.default
                        }
                    } as T
            },
        )

    @Test
    fun lb206_eachLanguageGetsItsOwnSentence() {
        assertEquals("Nocturno en español", configured.forLanguage("es"))
        assertEquals("Night in English", configured.forLanguage("en"))
        // The app ships Spanish and English; anything else reads Spanish, which is its default language.
        assertEquals("Nocturno en español", configured.forLanguage("fr"))
    }

    @Test
    fun lb206_withNoConfigTheCompiledDefaultIsShown() {
        // NFR-REL-05: an unreachable config still says when the night service runs.
        val text = NightServiceTextOf(CompiledDefaultsRemoteConfig())

        assertEquals(
            "El servicio nocturno funciona los viernes, sábados y vísperas de festivo.",
            text.forLanguage("es"),
        )
        assertEquals("Night service runs on Friday, Saturday and holiday eves.", text.forLanguage("en"))
    }
}
