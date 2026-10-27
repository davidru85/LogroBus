package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.JsonElement

/** Who is calling, for the `User-Agent` header of spec §8.7. */
data class ClientIdentity(
    val appVersion: String,
    val platform: String,
    val osVersion: String,
)

/** Spec §8.7: live endpoints (API-03 to 08) and static ones (API-01, 02) have different request timeouts. */
enum class RequestKind {
    Live,
    Static,
}

/** Every call to the source BFF goes through here (api_spec.md §1). */
class SourceClient internal constructor(
    engine: HttpClientEngine,
    identity: ClientIdentity,
    baseUrl: () -> String,
) {
    suspend fun get(
        path: String,
        kind: RequestKind,
    ): AppResult<JsonElement> = AppResult.Failure(AppError.Network)

    suspend fun post(
        path: String,
        body: JsonElement,
        kind: RequestKind,
    ): AppResult<JsonElement> = AppResult.Failure(AppError.Network)
}
