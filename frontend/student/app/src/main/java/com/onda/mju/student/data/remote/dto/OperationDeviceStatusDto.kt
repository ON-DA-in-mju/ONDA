package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for public.operation_device_status PostgREST / Realtime responses.
 */
@Serializable
data class OperationDeviceStatusDto(
    @SerialName("operation_id")
    val operationId: String,
    @SerialName("gps_ok")
    val gpsOk: Boolean? = null,
    @SerialName("gps_enabled")
    val gpsEnabled: Boolean? = null,
    @SerialName("last_location_at")
    val lastLocationAt: String? = null,
    @SerialName("last_accuracy")
    val lastAccuracy: Double? = null,
    @SerialName("updated_at")
    val updatedAt: String,
)
