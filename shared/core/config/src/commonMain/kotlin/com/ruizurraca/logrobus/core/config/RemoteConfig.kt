package com.ruizurraca.logrobus.core.config

/** Typed access to remote configuration (spec §8.9, api_spec.md §8). */
interface RemoteConfig {
    fun <T : Any> get(key: RemoteConfigKey<T>): T
}

/**
 * NFR-REL-05: the configuration the app runs on while no remote-config backend exists (OQ-16, LB-015). Every key
 * returns the default compiled into the app.
 */
class CompiledDefaultsRemoteConfig : RemoteConfig {
    override fun <T : Any> get(key: RemoteConfigKey<T>): T = key.default
}

/** A text that remote config provides in both shipped languages, Spanish (the default) and English. */
data class LocalizedText(
    val es: String,
    val en: String,
)

/** A remote-config key: its [name] in the backend and the [default] compiled into the app (api_spec.md §8). */
sealed class RemoteConfigKey<out T : Any>(
    val name: String,
    val default: T,
) {
    /** Switch to a project-owned proxy without a release (spec §8.10). */
    data object ApiBaseUrl : RemoteConfigKey<String>(
        name = "api_base_url",
        default = "https://transporteurbano.logrono.es",
    )

    /** FR-LIVE-02. */
    data object PollVehiclesMs : RemoteConfigKey<Int>(name = "poll_vehicles_ms", default = 10_000)

    /** FR-STOP-05. */
    data object PollArrivalsMs : RemoteConfigKey<Int>(name = "poll_arrivals_ms", default = 20_000)

    /** FR-LINE-07: how long a vehicle stays on the map after its last report. */
    data object VehicleGraceMs : RemoteConfigKey<Int>(name = "vehicle_grace_ms", default = 45_000)

    /** FR-STOP-03. */
    data object EtaPreviewMinutes : RemoteConfigKey<Int>(name = "eta_preview_minutes", default = 60)

    /** Kill switch for the buses layer. */
    data object FeatureLiveMap : RemoteConfigKey<Boolean>(name = "feature_live_map", default = true)

    /** Kill switch for How to Get There. */
    data object FeaturePlanner : RemoteConfigKey<Boolean>(name = "feature_planner", default = true)

    /** FR-FARE-05. */
    data object FeatureQr : RemoteConfigKey<Boolean>(name = "feature_qr", default = false)

    /** DD-11: display-name fixes for stops, applied from config, never from code. */
    data object NameOverrides :
        RemoteConfigKey<Map<String, String>>(name = "name_overrides", default = mapOf("PRADOVIEJOO" to "PRADOVIEJO"))

    /** FR-FARE-06. Neutral texts until the operator defines the tiers' eligibility (OQ-09). */
    data object FareTierTexts : RemoteConfigKey<Map<String, LocalizedText>>(
        name = "fare_tier_texts",
        default =
            mapOf(
                "FN20" to LocalizedText(es = "Descuento del 20 %", en = "20 % discount"),
                "FN50" to LocalizedText(es = "Descuento del 50 %", en = "50 % discount"),
            ),
    )

    /** FR-STOP-06, the spec's sentence until the operator's calendar is data (OQ-08). */
    data object NightServiceText : RemoteConfigKey<LocalizedText>(
        name = "night_service_text",
        default =
            LocalizedText(
                es = "El servicio nocturno funciona los viernes, sábados y vísperas de festivo.",
                en = "Night service runs on Friday, Saturday and holiday eves.",
            ),
    )

    /** Below this version, the app asks the user to update. */
    data object MinSupportedVersion : RemoteConfigKey<String>(name = "min_supported_version", default = "1.0.0")

    /** The map style. Empty until the owner chooses a tile provider (OQ-10, LB-013). */
    data object MapStyleUrl : RemoteConfigKey<String>(name = "map_style_url", default = "")

    /** FR-PLAN-02: geocoder results are limited to this province. */
    data object GeocoderProvinceFilter : RemoteConfigKey<String>(
        name = "geocoder_province_filter",
        default = "La Rioja",
    )

    companion object {
        /** Every key, in the order of api_spec.md §8. */
        val all: List<RemoteConfigKey<Any>> =
            listOf(
                ApiBaseUrl,
                PollVehiclesMs,
                PollArrivalsMs,
                VehicleGraceMs,
                EtaPreviewMinutes,
                FeatureLiveMap,
                FeaturePlanner,
                FeatureQr,
                NameOverrides,
                FareTierTexts,
                NightServiceText,
                MinSupportedVersion,
                MapStyleUrl,
                GeocoderProvinceFilter,
            )
    }
}
