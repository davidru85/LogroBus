package com.ruizurraca.logrobus.transit.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
internal data class EstimatedTimetableResponseDto(
    val result: EstimatedTimetableResultDto? = null,
)

@Serializable
internal data class EstimatedTimetableResultDto(
    val arrivals: JsonArray? = null,
)

@Serializable
internal data class EstimatedCallDto(
    val lineRef: String? = null,
    val directionRef: String? = null, // DD-04: opaque numeric code, e.g. "401"
    val stopPointRef: String? = null, // DD-01: "38"
    val order: Int? = null,
    val vehicleRef: String? = null, // DD-03: internal id; never join with VM
    val journeyCode: String? = null,
    val aimedArrivalTime: String? = null,
    val expectedArrivalTime: String? = null,
    val aimedDepartureTime: String? = null, // DD-07: "" when unknown
    val expectedDepartureTime: String? = null,
    val arrivalStatus: String? = null, // DD-09: only NO_REPORT observed
    val departureStatus: String? = null,
    val delaySeconds: Int? = null,
    val cancellation: Boolean? = null,
    val predictionInaccurate: Boolean? = null,
)
