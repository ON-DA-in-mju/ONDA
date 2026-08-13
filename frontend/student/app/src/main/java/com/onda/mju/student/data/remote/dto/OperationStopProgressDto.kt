package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for public.operation_stop_progress PostgREST / Realtime responses.
 */
@Serializable
data class OperationStopProgressDto(
    @SerialName("operation_id")
    val operationId: String,
    @SerialName("last_arrived_stop_id")
    val lastArrivedStopId: String? = null,
    @SerialName("last_passed_stop_id")
    val lastPassedStopId: String? = null,
    @SerialName("last_arrived_index")
    val lastArrivedIndex: Int = -1,
    @SerialName("last_passed_index")
    val lastPassedIndex: Int = -1,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
