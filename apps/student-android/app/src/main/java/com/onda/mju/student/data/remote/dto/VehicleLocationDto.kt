package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for public.vehicle_locations PostgREST / Realtime responses.
 * Column names follow Supabase snake_case via [SerialName].
 */
@Serializable
data class VehicleLocationDto(
    val id: String,
    @SerialName("operation_id")
    val operationId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double? = null,
    val heading: Double? = null,
    @SerialName("recorded_at")
    val recordedAt: String,
)
