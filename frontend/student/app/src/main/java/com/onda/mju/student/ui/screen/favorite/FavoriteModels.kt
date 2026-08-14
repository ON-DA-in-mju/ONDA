package com.onda.mju.student.ui.screen.favorite

import androidx.compose.ui.graphics.Color
import com.onda.mju.student.data.route.StudentRouteIds
import com.onda.mju.student.ui.screen.notice.StopGuideItem
import com.onda.mju.student.ui.screen.route.RouteStatus
import com.onda.mju.student.ui.screen.route.RouteUiModel

enum class FavoriteTab {
    Route,
    Stop,
}

data class FavoriteRoute(
    val id: String,
    val name: String,
    val status: String,
    val nextDeparture: String,
    val operatingCount: String,
    val iconBg: Color,
)

data class FavoriteStop(
    val id: String,
    val name: String,
    val routes: List<RouteTag>,
    val iconBg: Color,
)

data class RouteTag(
    val label: String,
    val bg: Color,
    val fg: Color,
)

private val SoftBlue = Color(0xFFDBEAFE)
private val SoftTeal = Color(0xFFCCFBF1)
private val SoftPurple = Color(0xFFEDE9FE)
private val BlueFg = Color(0xFF1D4ED8)
private val TealFg = Color(0xFF0F766E)
private val PurpleFg = Color(0xFF6D28D9)

fun List<RouteUiModel>.toFavoriteRoutes(): List<FavoriteRoute> =
    map { route ->
        FavoriteRoute(
            id = route.id,
            name = route.name,
            status = when (route.status) {
                RouteStatus.RUNNING -> "운행 중"
                RouteStatus.SCHEDULED -> "운행 예정"
                RouteStatus.ENDED -> "운행 종료"
            },
            nextDeparture = route.nextDeparture,
            operatingCount = route.activeVehicleCount?.let { "${it}대" } ?: "-",
            iconBg = when (route.id) {
                StudentRouteIds.GIHEUNG -> SoftBlue
                StudentRouteIds.MYEONGJI_STATION -> SoftTeal
                else -> SoftPurple
            },
        )
    }

fun List<RouteUiModel>.toFavoriteRoutes(favoriteRouteIds: Set<String>): List<FavoriteRoute> {
    if (favoriteRouteIds.isEmpty()) return emptyList()
    val ordered = StudentRouteIds.routeListUiIds.filter { it in favoriteRouteIds } +
        favoriteRouteIds.filter { it !in StudentRouteIds.routeListUiIds }
    return ordered.mapNotNull { id -> firstOrNull { it.id == id } }.toFavoriteRoutes()
}

fun buildFavoriteStops(
    guideItems: List<StopGuideItem>,
    favoriteStopIds: Set<String> = emptySet(),
    limit: Int = 50,
): List<FavoriteStop> {
    val source = if (favoriteStopIds.isEmpty()) {
        emptyList()
    } else {
        guideItems.filter { it.id in favoriteStopIds }
    }
    return source.take(limit).map { item ->
        FavoriteStop(
            id = item.id,
            name = item.name,
            iconBg = when (item.routeId) {
                "giheung" -> SoftBlue
                "myeongji" -> SoftTeal
                else -> SoftPurple
            },
            routes = item.availableRoutes.map { label ->
                val (bg, fg) = when {
                    label.contains("기흥") -> SoftBlue to BlueFg
                    label.contains("명지대") -> SoftTeal to TealFg
                    else -> SoftPurple to PurpleFg
                }
                RouteTag(label = label, bg = bg, fg = fg)
            },
        )
    }
}

@Deprecated("Use routes/stops from DB", ReplaceWith("emptyList()"))
fun sampleFavoriteRoutes(): List<FavoriteRoute> = emptyList()

@Deprecated("Use stops from DB", ReplaceWith("emptyList()"))
fun sampleFavoriteStops(): List<FavoriteStop> = emptyList()
