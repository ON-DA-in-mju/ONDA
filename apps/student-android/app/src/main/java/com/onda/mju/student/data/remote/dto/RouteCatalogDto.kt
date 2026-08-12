package com.onda.mju.student.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteDetailDto(
    val id: String,
    @SerialName("route_name")
    val routeName: String,
    val direction: String? = null,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("start_location")
    val startLocation: String? = null,
    @SerialName("end_location")
    val endLocation: String? = null,
)

@Serializable
data class RouteStopRowDto(
    @SerialName("stop_order")
    val stopOrder: Int,
    @SerialName("stop_id")
    val stopId: String? = null,
    @SerialName("expected_minutes")
    val expectedMinutes: Int? = null,
    val stops: StopDto? = null,
    val routes: RouteNameEmbedDto? = null,
)

@Serializable
data class RouteNameEmbedDto(
    val id: String? = null,
    @SerialName("route_name")
    val routeName: String? = null,
)

@Serializable
data class ScheduleDetailDto(
    val id: String,
    @SerialName("route_id")
    val routeId: String,
    @SerialName("departure_time")
    val departureTime: String,
    val weekday: String,
    val semester: String,
    val routes: RouteDetailDto? = null,
)
