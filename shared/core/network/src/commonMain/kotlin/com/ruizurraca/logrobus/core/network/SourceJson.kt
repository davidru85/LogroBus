package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.warn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

private const val TAG = "SourceJson"

/** api_spec.md §3: DTO fields are nullable or defaulted; the mappers, not the decoder, decide what is required. */
internal val SourceJson =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

/**
 * The collection under [keys] in a BFF envelope, such as `result.lines` (api_spec.md §3). A missing key, or a value
 * that is not an object on the way or an array at the end, is a [AppError.Data] error.
 */
fun JsonElement.arrayAt(vararg keys: String): AppResult<JsonArray> {
    val value = keys.fold<String, JsonElement?>(this) { element, key -> (element as? JsonObject)?.get(key) }
    return if (value is JsonArray) AppResult.Success(value) else AppResult.Failure(AppError.Data)
}

/** The items of a collection that decoded, and how many were [dropped]. */
data class Decoded<out T>(
    val items: List<T>,
    val dropped: Int,
)

/**
 * Decodes each item on its own, so one bad item is dropped and logged, never the whole response (NFR-REL-02). The log
 * names the type, the position and the error type only: a decoding error's message quotes the input (NFR-PRIV-02).
 */
fun <T> JsonArray.decodeEach(
    serializer: KSerializer<T>,
    logger: Logger,
): Decoded<T> {
    var dropped = 0
    val items =
        mapIndexedNotNull { index, element ->
            try {
                SourceJson.decodeFromJsonElement(serializer, element)
            } catch (e: IllegalArgumentException) {
                // A SerializationException, or a value that the type itself rejects.
                dropped++
                val type = serializer.descriptor.serialName.substringAfterLast('.')
                logger.warn(TAG, "Dropped $type at index $index (${e::class.simpleName})")
                null
            }
        }
    return Decoded(items, dropped)
}

/** Decodes a bare object, as the transit endpoints send it (api_spec.md §3). */
fun <T> JsonElement.decodeAs(serializer: KSerializer<T>): AppResult<T> = AppResult.Failure(AppError.Network)
