package com.ruizurraca.logrobus.transit.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
internal data class StopsResponseDto(
    val result: StopsResultDto? = null,
)

@Serializable
internal data class StopsResultDto(
    val stops: JsonArray? = null,
)

@Serializable
internal data class StopDto(
    val id: Int? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val lines: List<Int> = emptyList(),
    val status: Int? = null,
)
