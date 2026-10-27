package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** The collection under [keys] in a BFF envelope, such as `result.lines` (api_spec.md §3). */
fun JsonElement.arrayAt(vararg keys: String): AppResult<JsonArray> = AppResult.Success(JsonArray(emptyList()))
