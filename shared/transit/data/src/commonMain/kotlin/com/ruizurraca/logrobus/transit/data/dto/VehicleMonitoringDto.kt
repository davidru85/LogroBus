package com.ruizurraca.logrobus.transit.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
internal data class VehicleMonitoringResponseDto(
    val result: VehicleMonitoringResultDto? = null,
)

@Serializable
internal data class VehicleMonitoringResultDto(
    val activities: JsonArray? = null,
)

@Serializable
internal data class VehicleActivityDto(
    val vehicleRef: String? = null, // DD-03: fleet number; NOT the ET id space
    val vehicleMonitoringRef: String? = null,
    val lineRef: String? = null, // DD-06: may be "[object Object]"
    val publishedLineName: String? = null,
    val directionRef: String? = null, // DD-04, DD-07: "Ida" | "Vuelta" | ""
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationRecordedAtTime: String? = null,
    val nextStopRef: String? = null, // DD-01: "0632" or ""
    val nextStopAimedArrivalTime: String? = null, // DD-05: local time mislabelled Z
    val nextStopExpectedArrivalTime: String? = null, // DD-05: same defect
    val delaySeconds: Int? = null,
    val confidenceLevel: String? = null,
    val monitored: Boolean? = null,
    val predictionInaccurate: Boolean? = null,
)
