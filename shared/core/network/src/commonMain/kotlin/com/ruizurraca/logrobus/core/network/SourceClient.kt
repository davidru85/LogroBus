package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.JsonElement

/** Who is calling, for the `User-Agent` header of spec §8.7. */
data class ClientIdentity(
    val appVersion: String,
    val platform: String,
    val osVersion: String,
) {
    val userAgent: String
        get() = "LogronoBus/$appVersion ($platform $osVersion)"
}

/** Spec §8.7: live endpoints (API-03 to 08) and static ones (API-01, 02) have different request timeouts. */
enum class RequestKind {
    Live,
    Static,
}

/** Every call to the source BFF goes through here (api_spec.md §1). */
class SourceClient internal constructor(
    engine: HttpClientEngine,
    identity: ClientIdentity,
    private val baseUrl: () -> String,
) {
    private val http =
        HttpClient(engine) {
            expectSuccess = false
            install(UserAgent) { agent = identity.userAgent }
            install(ContentEncoding) { gzip() }
            defaultRequest { accept(ContentType.Application.Json) }
        }

    suspend fun get(
        path: String,
        kind: RequestKind,
    ): AppResult<JsonElement> {
        http.get(url(path))
        return AppResult.Failure(AppError.Network)
    }

    suspend fun post(
        path: String,
        body: JsonElement,
        kind: RequestKind,
    ): AppResult<JsonElement> {
        http.post(url(path)) { setBody(TextContent(body.toString(), ContentType.Application.Json)) }
        return AppResult.Failure(AppError.Network)
    }

    private fun url(path: String) = baseUrl().trimEnd('/') + path
}
