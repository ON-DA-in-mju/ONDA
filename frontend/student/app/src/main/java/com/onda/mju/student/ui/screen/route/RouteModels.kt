package com.onda.mju.student.ui.screen.route

import androidx.annotation.DrawableRes
import com.onda.mju.student.R
import com.onda.mju.student.data.route.StudentRouteIds

/**
 * Runtime status for a shuttle route.
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
 * UI model for the route list. Populated from today's operations (+ routes metadata).
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

/** Loading / offline skeleton until operations arrive. */
fun sampleRouteList(): List<RouteUiModel> =
    StudentRouteIds.orderedUiIds.map { uiId ->
        RouteUiModel(
            id = uiId,
            name = StudentRouteIds.displayName(uiId),
            fromLabel = "명지대",
            toLabel = when (uiId) {
                StudentRouteIds.GIHEUNG -> "기흥역"
                StudentRouteIds.MYEONGJI_STATION -> "명지대역"
                else -> "시내 순환"
            },
            status = RouteStatus.SCHEDULED,
            activeVehicleCount = null,
            nextDeparture = "-",
            isFavorite = false,
            imageRes = StudentRouteIds.imageRes(uiId),
        )
    }

fun List<RouteUiModel>.filterBy(filter: RouteFilter): List<RouteUiModel> = when (filter) {
    RouteFilter.ALL -> this
    RouteFilter.RUNNING -> filter { it.status == RouteStatus.RUNNING }
    RouteFilter.FAVORITE -> filter { it.isFavorite }
}
