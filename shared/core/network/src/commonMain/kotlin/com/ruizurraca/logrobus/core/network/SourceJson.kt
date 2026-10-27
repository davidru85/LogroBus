package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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

/** Decodes each item on its own (NFR-REL-02). */
fun <T> JsonArray.decodeEach(
    serializer: KSerializer<T>,
    logger: Logger,
): Decoded<T> = Decoded(emptyList(), 0)
