package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.network.RequestKind
import com.ruizurraca.logrobus.core.network.SourceClient
import kotlinx.serialization.json.JsonElement

/**
 * API-01 and API-02 as the sync wants them: the payloads themselves, not their DTOs (architecture.md §4.4).
 *
 * [StaticSource] hands [DefaultStaticSyncEngine] a [JsonElement] because the sync hashes it to tell an unchanged
 * payload from a changed one, and only then hands it to the normaliser, which decodes it through the DTOs. A decoded
 * [LinesApi] answer would have lost the bytes the hash is taken over, so this class calls the two endpoints directly
 * and leaves the decoding where it belongs. Both take the static timeout: they run in the daily sync, never while a
 * screen waits (api_spec.md §1).
 */
internal class SourceStaticSource(
    private val client: SourceClient,
) : StaticSource {
    /** API-01: the lines, each with its ascending and descending patterns. */
    override suspend fun lines(): AppResult<JsonElement> = client.get(PATH_LINES, RequestKind.Static)

    /** API-02: the stops, each with the lines that serve it. */
    override suspend fun stops(): AppResult<JsonElement> = client.get(PATH_STOPS, RequestKind.Static)

    private companion object {
        const val PATH_LINES = "/api/linesDiscovery/lines"
        const val PATH_STOPS = "/api/linesDiscovery/stops"
    }
}
