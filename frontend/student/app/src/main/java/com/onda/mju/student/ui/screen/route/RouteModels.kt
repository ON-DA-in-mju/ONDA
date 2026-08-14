package com.onda.mju.student.ui.screen.route

import androidx.annotation.DrawableRes
import com.onda.mju.student.data.route.StudentRouteIds

/**
 * 노선 카드 상태 — 오늘 operations 집계.
 * RUNNING: IN_PROGRESS 있음
 * SCHEDULED: 앞으로 남은 SCHEDULED 있음
 * ENDED: 오늘 운행이 끝났거나(COMPLETED/CANCELLED만) 남은 배차 없음
 */
enum class RouteStatus {
    RUNNING,
    SCHEDULED,
    ENDED,
}

enum class RouteFilter {
    ALL,
    RUNNING,
    SCHEDULED,
    ENDED,
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
    @param:DrawableRes val imageRes: Int,
)

/** Loading / offline skeleton until operations arrive. */
fun sampleRouteList(): List<RouteUiModel> =
    StudentRouteIds.routeListUiIds.map { uiId ->
        RouteUiModel(
            id = uiId,
            name = StudentRouteIds.displayName(uiId),
            fromLabel = when (uiId) {
                StudentRouteIds.CITY_SHUTTLE_VACATION -> "생활관(명현관)"
                else -> "명지대"
            },
            toLabel = when (uiId) {
                StudentRouteIds.GIHEUNG -> "기흥역"
                StudentRouteIds.MYEONGJI_STATION -> "명지대역"
                StudentRouteIds.CITY_SHUTTLE_VACATION -> "시내 순환 (주말·방학)"
                else -> "시내 순환"
            },
            status = RouteStatus.SCHEDULED,
            activeVehicleCount = null,
            nextDeparture = "-",
            imageRes = StudentRouteIds.imageRes(uiId),
        )
    }

fun List<RouteUiModel>.filterBy(filter: RouteFilter): List<RouteUiModel> = when (filter) {
    RouteFilter.ALL -> this
    RouteFilter.RUNNING -> filter { it.status == RouteStatus.RUNNING }
    RouteFilter.SCHEDULED -> filter { it.status == RouteStatus.SCHEDULED }
    RouteFilter.ENDED -> filter { it.status == RouteStatus.ENDED }
}
