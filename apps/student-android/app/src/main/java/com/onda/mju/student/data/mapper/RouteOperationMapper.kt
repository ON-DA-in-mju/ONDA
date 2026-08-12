package com.onda.mju.student.data.mapper

import com.onda.mju.student.R
import com.onda.mju.student.data.remote.dto.OperationDto
import com.onda.mju.student.ui.screen.route.RouteStatus
import com.onda.mju.student.ui.screen.route.RouteUiModel
import java.time.LocalTime
import java.time.ZoneId

/**
 * Maps today's Supabase operations (+ nested schedule/route) into route-list UI models.
 */

private data class RouteDisplayMeta(
    val id: String,
    val name: String,
    val fromLabel: String,
    val toLabel: String,
    val imageRes: Int,
)

private val routeDisplayByName: Map<String, RouteDisplayMeta> = mapOf(
    "기흥역 통학버스" to RouteDisplayMeta(
        id = "giheung",
        name = "기흥역 통학버스",
        fromLabel = "명지대",
        toLabel = "기흥역",
        imageRes = R.drawable.route_thumb_giheung,
    ),
    "명지대역 셔틀" to RouteDisplayMeta(
        id = "myeongji_station",
        name = "명지대역 셔틀",
        fromLabel = "명지대",
        toLabel = "명지대역",
        imageRes = R.drawable.route_thumb_myeongji,
    ),
    "시내 셔틀" to RouteDisplayMeta(
        id = "city_shuttle",
        name = "시내 셔틀",
        fromLabel = "명지대",
        toLabel = "시내 순환",
        imageRes = R.drawable.route_thumb_city,
    ),
)

private val routeDisplayOrder: List<String> = listOf(
    "기흥역 통학버스",
    "명지대역 셔틀",
    "시내 셔틀",
)

fun List<OperationDto>.toRouteUiModels(): List<RouteUiModel> {
    val grouped = asSequence()
        .mapNotNull { operation ->
            val routeName = operation.schedule?.route?.routeName ?: return@mapNotNull null
            if (routeName !in routeDisplayByName) return@mapNotNull null
            routeName to operation
        }
        .groupBy({ it.first }, { it.second })

    return routeDisplayOrder.map { routeName ->
        val meta = routeDisplayByName.getValue(routeName)
        val operations = grouped[routeName].orEmpty()
        val inProgressCount = operations.count { it.status == "IN_PROGRESS" }
        val status = if (inProgressCount > 0) {
            RouteStatus.RUNNING
        } else {
            RouteStatus.SCHEDULED
        }
        val now = LocalTime.now(ZoneId.of("Asia/Seoul"))
        val nextDeparture = operations
            .asSequence()
            .filter { it.status == "SCHEDULED" }
            .mapNotNull { it.schedule?.departureTime?.toLocalTimeOrNull() }
            .filter { !it.isBefore(now) }
            .minOrNull()
            ?.toUiDepartureTime()
            ?: "-"

        RouteUiModel(
            id = meta.id,
            name = meta.name,
            fromLabel = meta.fromLabel,
            toLabel = meta.toLabel,
            status = status,
            activeVehicleCount = inProgressCount.takeIf { it > 0 },
            nextDeparture = nextDeparture,
            isFavorite = false,
            imageRes = meta.imageRes,
        )
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? {
    // "08:20:00" / "08:20" -> LocalTime
    val parts = split(':')
    if (parts.size < 2) return null
    return try {
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) {
        null
    }
}

private fun LocalTime.toUiDepartureTime(): String {
    return "%02d:%02d".format(hour, minute)
}
