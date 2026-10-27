package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.common.warn
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val TAG = "SourceClient"

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
    clock: ServerCorrectedClock,
    private val baseUrl: () -> String,
    private val logger: Logger,
) {
    constructor(
        identity: ClientIdentity,
        clock: ServerCorrectedClock,
        baseUrl: () -> String,
        logger: Logger,
    ) : this(platformEngine(), identity, clock, baseUrl, logger)

    internal val http =
        HttpClient(engine) {
            expectSuccess = false
            install(UserAgent) { agent = identity.userAgent }
            install(HttpTimeout) { connectTimeoutMillis = CONNECT_TIMEOUT.inWholeMilliseconds }
            install(ContentEncoding) { gzip() }
            // Spec §8.6: every response, an error included, carries the server's clock.
            install(
                createClientPlugin("ServerDate") {
                    onResponse { response ->
                        response.headers[HttpHeaders.Date]?.let(::parseHttpDate)?.let(clock::onServerDate)
                    }
                },
            )
            defaultRequest { accept(ContentType.Application.Json) }
        }

    suspend fun get(
        path: String,
        kind: RequestKind,
    ): AppResult<JsonElement> = send(HttpMethod.Get, path, kind)

    suspend fun post(
        path: String,
        body: JsonElement,
        kind: RequestKind,
    ): AppResult<JsonElement> =
        send(HttpMethod.Post, path, kind) { setBody(TextContent(body.toString(), ContentType.Application.Json)) }

    // api_spec.md §7: no connection, DNS failures and timeouts are IOExceptions on every engine; cancellation is not.
    // NFR-PRIV-02: a failure is logged by method, path and error type, never with a body or an error message.
    private suspend fun send(
        method: HttpMethod,
        path: String,
        kind: RequestKind,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): AppResult<JsonElement> {
        val result =
            try {
                http
                    .request(baseUrl().trimEnd('/') + path) {
                        this.method = method
                        timeout { requestTimeoutMillis = kind.timeout.inWholeMilliseconds }
                        configure()
                    }.toResult()
            } catch (e: IOException) {
                logger.warn(TAG, "${method.value} $path failed: Network (${e::class.simpleName})")
                return AppResult.Failure(AppError.Network)
            }
        if (result is AppResult.Failure) logger.warn(TAG, "${method.value} $path failed: ${result.error}")
        return result
    }

    // DD-15: any 2xx is a success; POSTs answer 201.
    private suspend fun HttpResponse.toResult(): AppResult<JsonElement> =
        if (status.isSuccess()) {
            parse(bodyAsText())
        } else {
            AppResult.Failure(AppError.Server(status.value))
        }

    // api_spec.md §7: a body that is not JSON fails this resource only. The reader takes an unquoted word, such as
    // an HTML page without spaces, as a literal; every source response is an object or an array, so a scalar is Data.
    private fun parse(body: String): AppResult<JsonElement> {
        val element =
            try {
                SourceJson.parseToJsonElement(body)
            } catch (_: SerializationException) {
                null
            }
        return if (element is JsonObject || element is JsonArray) {
            AppResult.Success(element)
        } else {
            AppResult.Failure(AppError.Data)
        }
    }
}
