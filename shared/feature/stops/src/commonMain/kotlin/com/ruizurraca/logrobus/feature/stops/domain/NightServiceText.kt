package com.ruizurraca.logrobus.feature.stops.domain

import com.ruizurraca.logrobus.core.config.LocalizedText
import com.ruizurraca.logrobus.core.config.RemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey

/**
 * FR-STOP-06 (LB-206): the sentence an empty arrival list adds for a stop only Búho lines serve, from remote config
 * (OQ-08, NFR-REL-05). It is the config's answer or the default compiled into the app; the sentence never lives in
 * shared code.
 */
fun interface NightServiceText {
    /**
     * The sentence as [language] names it, Spanish for anything but English, which is what the app ships and defaults
     * to. A plain string rather than [LocalizedText], so Swift sees the same one-word call as Kotlin.
     */
    fun forLanguage(language: String): String
}

class NightServiceTextOf(
    private val config: RemoteConfig,
) : NightServiceText {
    override fun forLanguage(language: String): String {
        val text = config.get(RemoteConfigKey.NightServiceText)
        return if (language.lowercase().startsWith("en")) text.en else text.es
    }
}
