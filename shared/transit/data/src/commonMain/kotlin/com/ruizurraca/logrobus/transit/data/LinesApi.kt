package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.network.Decoded
import com.ruizurraca.logrobus.core.network.RequestKind
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.network.arrayAt
import com.ruizurraca.logrobus.core.network.decodeEach
import com.ruizurraca.logrobus.transit.data.dto.LineDto
import com.ruizurraca.logrobus.transit.data.dto.StopDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

/**
 * API-01 and API-02 as DTOs (architecture.md §4.2): the two static payloads, each item decoded on its own so that one
 * malformed entry costs that entry alone (NFR-REL-02). The static sync is the only caller: it normalises what comes
 * back through [StaticNormaliser].
 *
 * Both endpoints take the static timeout of spec §8.7, since they run in the daily sync, never while a screen waits.
 */
internal class LinesApi(
    private val client: SourceClient,
    private val logger: Logger,
) {
    /** API-01: the lines, each with its ascending and descending patterns. */
    suspend fun lines(): AppResult<Decoded<LineDto>> =
        client
            .get("/api/linesDiscovery/lines", RequestKind.Static)
            .decodeCollection(LINES, LineDto.serializer(), logger)

    /** API-02: the stops, each with the lines that serve it. */
    suspend fun stops(): AppResult<Decoded<StopDto>> =
        client
            .get("/api/linesDiscovery/stops", RequestKind.Static)
            .decodeCollection(STOPS, StopDto.serializer(), logger)

    private companion object {
        const val LINES = "lines"
        const val STOPS = "stops"
    }
}

/**
 * `result.[key]` of a call, decoded item by item (api_spec.md §3), for both [LinesApi] and [RealtimeApi]. A failure
 * passes through unchanged, and the [Decoded.dropped] count goes back to the caller with the items; [decodeEach] has
 * already logged each dropped one.
 */
internal fun <T> AppResult<JsonElement>.decodeCollection(
    key: String,
    serializer: KSerializer<T>,
    logger: Logger,
): AppResult<Decoded<T>> =
    when (this) {
        is AppResult.Failure -> {
            this
        }

        is AppResult.Success -> {
            when (val items = value.arrayAt("result", key)) {
                is AppResult.Success -> AppResult.Success(items.value.decodeEach(serializer, logger))
                is AppResult.Failure -> items
            }
        }
    }
