package com.ruizurraca.logrobus.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** NFR-REL-05: with no remote-config backend (OQ-16), every key returns the default compiled into the app. */
class RemoteConfigTest {
    // api_spec.md §8, row by row.
    private val documentedDefaults: List<Pair<String, Any>> =
        listOf(
            "api_base_url" to "https://transporteurbano.logrono.es",
            "poll_vehicles_ms" to 10_000,
            "poll_arrivals_ms" to 20_000,
            "vehicle_grace_ms" to 45_000,
            "eta_preview_minutes" to 60,
            "feature_live_map" to true,
            "feature_planner" to true,
            "feature_qr" to false,
            "name_overrides" to mapOf("PRADOVIEJOO" to "PRADOVIEJO"),
            "fare_tier_texts" to
                mapOf(
                    "FN20" to LocalizedText(es = "Descuento del 20 %", en = "20 % discount"),
                    "FN50" to LocalizedText(es = "Descuento del 50 %", en = "50 % discount"),
                ),
            "night_service_text" to
                LocalizedText(
                    es = "El servicio nocturno funciona los viernes, sábados y vísperas de festivo.",
                    en = "Night service runs on Friday, Saturday and holiday eves.",
                ),
            "min_supported_version" to "1.0.0",
            "map_style_url" to "",
            "geocoder_province_filter" to "La Rioja",
        )

    @Test
    fun lb004_remoteConfigReturnsTheCompiledDefaultForEveryKey() {
        val config = CompiledDefaultsRemoteConfig()

        assertEquals(documentedDefaults, RemoteConfigKey.all.map { it.name to config.get(it) })
    }

    @Test
    fun lb004_textDefaultsExistInSpanishAndEnglish() {
        val config = CompiledDefaultsRemoteConfig()
        val texts =
            config.get(RemoteConfigKey.FareTierTexts).values + config.get(RemoteConfigKey.NightServiceText)

        texts.forEach { assertTrue(it.es.isNotBlank() && it.en.isNotBlank(), it.toString()) }
    }
}
