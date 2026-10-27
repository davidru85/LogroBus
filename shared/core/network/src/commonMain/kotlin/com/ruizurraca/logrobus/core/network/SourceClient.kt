package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.content.TextContent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Spec §8.7. */
private val CONNECT_TIMEOUT = 5.seconds

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
enum class RequestKind(
    internal val timeout: Duration,
) {
    Live(10.seconds),
    Static(30.seconds),
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
            install(HttpTimeout) { connectTimeoutMillis = CONNECT_TIMEOUT.inWholeMilliseconds }
            install(ContentEncoding) { gzip() }
            defaultRequest { accept(ContentType.Application.Json) }
        }

    suspend fun get(
        path: String,
        kind: RequestKind,
    ): AppResult<JsonElement> {
        val response = http.get(url(path)) { timeout { requestTimeoutMillis = kind.timeout.inWholeMilliseconds } }
        return response.toResult()
    }

    suspend fun post(
        path: String,
        body: JsonElement,
        kind: RequestKind,
    ): AppResult<JsonElement> {
        val response =
            http.post(url(path)) {
                timeout { requestTimeoutMillis = kind.timeout.inWholeMilliseconds }
                setBody(TextContent(body.toString(), ContentType.Application.Json))
            }
        return response.toResult()
    }

    // DD-15: any 2xx is a success; POSTs answer 201.
    private suspend fun HttpResponse.toResult(): AppResult<JsonElement> =
        if (status.isSuccess()) {
            parse(bodyAsText())
        } else {
            AppResult.Failure(AppError.Server(status.value))
        }

    // api_spec.md §7: a body that is not JSON fails this resource only.
    private fun parse(body: String): AppResult<JsonElement> =
        try {
            AppResult.Success(Json.parseToJsonElement(body))
        } catch (_: SerializationException) {
            AppResult.Failure(AppError.Data)
        }

    private fun url(path: String) = baseUrl().trimEnd('/') + path
}
