package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for public.operations / schedules / routes / buses PostgREST responses.
 * Column names follow Supabase snake_case via [SerialName].
 */

@Serializable
data class OperationDto(
    val id: String,
    @SerialName("operation_date")
    val operationDate: String,
    val status: String,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("ended_at")
    val endedAt: String? = null,
    @SerialName("schedule_id")
    val scheduleId: String? = null,
    @SerialName("bus_id")
    val busId: String? = null,
    val schedule: ScheduleWithRouteDto? = null,
    val bus: BusDto? = null,
)

/**
 * Flat operations row for Realtime postgres changes (no nested relations).
 */
@Serializable
data class OperationRealtimeDto(
    val id: String,
    @SerialName("operation_date")
    val operationDate: String,
    val status: String,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("ended_at")
    val endedAt: String? = null,
    @SerialName("schedule_id")
    val scheduleId: String? = null,
    @SerialName("bus_id")
    val busId: String? = null,
)

@Serializable
data class ScheduleWithRouteDto(
    val id: String,
    @SerialName("departure_time")
    val departureTime: String,
    @SerialName("route_id")
    val routeId: String,
    val route: RouteDto? = null,
)

@Serializable
data class ScheduleDto(
    @SerialName("departure_time")
    val departureTime: String,
    @SerialName("route_id")
    val routeId: String,
)

@Serializable
data class RouteDto(
    val id: String,
    @SerialName("route_name")
    val routeName: String,
    val direction: String? = null,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("start_location")
    val startLocation: String? = null,
    @SerialName("end_location")
    val endLocation: String? = null,
)

@Serializable
data class BusDto(
    val id: String,
    @SerialName("bus_name")
    val busName: String? = null,
    @SerialName("vehicle_number")
    val vehicleNumber: String? = null,
)
