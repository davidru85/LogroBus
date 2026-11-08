package com.ruizurraca.logrobus.transit.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
internal data class LinesResponseDto(
    val result: LinesResultDto? = null,
)

@Serializable
internal data class LinesResultDto(
    val lines: JsonArray? = null,
)

@Serializable
internal data class LineDto(
    val id: Int? = null,
    val name: String? = null,
    val color: String? = null,
    val status: Int? = null,
    val stops: LineStopsDto? = null,
)

@Serializable
internal data class LineStopsDto(
    val asc: List<LineStopDto> = emptyList(),
    val desc: List<LineStopDto> = emptyList(),
)

@Serializable
internal data class LineStopDto(
    val id: Int? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val status: Int? = null,
    val onwardShape: List<LatLngDto> = emptyList(),
)

@Serializable
internal data class LatLngDto(
    val lat: Double? = null,
    val lng: Double? = null,
)
