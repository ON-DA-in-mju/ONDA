package com.onda.mju.student.ui.screen.route

import androidx.annotation.DrawableRes
import com.onda.mju.student.R

/**
 * Runtime status for a shuttle route.
 * Later replace mock values with driver-app / Supabase "운행 시작" state.
 */
enum class RouteStatus {
    RUNNING,
    SCHEDULED,
}

enum class RouteFilter {
    ALL,
    RUNNING,
    FAVORITE,
}

/**
 * UI model for the route list. Keep fields backend-ready so mock data
 * can be swapped without changing filter/card logic.
 */
data class RouteUiModel(
    val id: String,
    val name: String,
    /** Left endpoint label shown before the bidirectional arrow. */
    val fromLabel: String,
    /** Right endpoint label shown after the bidirectional arrow. */
    val toLabel: String,
    val status: RouteStatus,
    val activeVehicleCount: Int?,
    val nextDeparture: String,
    val isFavorite: Boolean,
    @param:DrawableRes val imageRes: Int,
)

fun sampleRouteList(): List<RouteUiModel> = listOf(
    RouteUiModel(
        id = "giheung",
        name = "기흥역 통학버스",
        fromLabel = "명지대",
        toLabel = "기흥역",
        status = RouteStatus.RUNNING,
        activeVehicleCount = 3,
        nextDeparture = "17:15",
        isFavorite = true,
        imageRes = R.drawable.route_thumb_giheung,
    ),
    RouteUiModel(
        id = "myeongji_station",
        name = "명지대역 셔틀",
        fromLabel = "명지대",
        toLabel = "명지대역",
        status = RouteStatus.RUNNING,
        activeVehicleCount = 4,
        nextDeparture = "16:50",
        isFavorite = false,
        imageRes = R.drawable.route_thumb_myeongji,
    ),
    RouteUiModel(
        id = "city_shuttle",
        name = "시내 셔틀",
        fromLabel = "명지대",
        toLabel = "시내 순환",
        status = RouteStatus.SCHEDULED,
        activeVehicleCount = null,
        nextDeparture = "17:15",
        isFavorite = false,
        imageRes = R.drawable.route_thumb_city,
    ),
)

fun List<RouteUiModel>.filterBy(filter: RouteFilter): List<RouteUiModel> = when (filter) {
    RouteFilter.ALL -> this
    RouteFilter.RUNNING -> filter { it.status == RouteStatus.RUNNING }
    RouteFilter.FAVORITE -> filter { it.isFavorite }
}
