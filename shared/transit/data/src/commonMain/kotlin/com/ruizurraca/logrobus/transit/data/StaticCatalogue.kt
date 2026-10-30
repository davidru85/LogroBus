package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.serialization.json.JsonElement

/** The static payloads as the source sends them: API-01's lines and API-02's stops. */
internal interface StaticSource {
    suspend fun lines(): AppResult<JsonElement>

    suspend fun stops(): AppResult<JsonElement>
}

/**
 * Turns the static payloads into the catalogue to store, fixing the source's quirks (spec §5.3 to §5.6). Line and stop
 * ids are unique in the result: a duplicate fails the write, and the sync with it.
 */
internal interface StaticNormaliser {
    /**
     * Changes whenever the same payloads would normalise differently: a new version of the normaliser, or new name
     * overrides from remote config (DD-11). The sync renormalises an unchanged payload when it changes.
     */
    val fingerprint: String

    fun normalise(
        lines: JsonElement,
        stops: JsonElement,
    ): AppResult<StaticCatalogue>
}

/** The normalised lines and stops a sync stores (spec §5.1, §8.8). */
internal data class StaticCatalogue(
    val lines: List<CatalogueLine>,
    val stops: List<CatalogueStop>,
)

internal data class CatalogueLine(
    val id: Int,
    val code: String,
    val rawName: String,
    val displayName: String,
    /** ARGB. */
    val color: Long,
    /** ARGB. */
    val onColor: Long,
    val kind: LineKind,
    val isOperational: Boolean,
    val patterns: List<CataloguePattern>,
)

internal data class CataloguePattern(
    val direction: Direction,
    val headsign: String,
    /** An encoded polyline, precision 6. */
    val shapePolyline: String,
    /** In order: a stop's position in the list is its sequence. */
    val stops: List<CataloguePatternStop>,
)

internal data class CataloguePatternStop(
    val stopId: Int,
    /** Where the stop's segment starts in the pattern's shape. */
    val shapeIndex: Int,
)

internal data class CatalogueStop(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val isOperational: Boolean,
    val isProvisional: Boolean,
    /** The lines API-02 says serve the stop. */
    val lineIds: List<Int>,
)
