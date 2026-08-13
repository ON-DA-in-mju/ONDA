package com.onda.mju.student.data.mapper

import com.onda.mju.student.data.remote.dto.OperationDto
import com.onda.mju.student.data.remote.dto.RouteDetailDto
import com.onda.mju.student.data.route.OperationalRouteResolver
import com.onda.mju.student.data.route.StudentRouteIds
import com.onda.mju.student.ui.screen.route.RouteStatus
import com.onda.mju.student.ui.screen.route.RouteUiModel
import java.time.LocalTime
import java.time.ZoneId

/**
 * Maps today's Supabase operations (+ nested schedule/route) into route-list UI models.
 */
fun List<OperationDto>.toRouteUiModels(
    routeDetails: List<RouteDetailDto> = emptyList(),
): List<RouteUiModel> {
    val grouped = asSequence()
        .mapNotNull { operation ->
            val routeName = operation.schedule?.route?.routeName ?: return@mapNotNull null
            val family = OperationalRouteResolver.baseRouteFamily(routeName)
            val uiId = StudentRouteIds.uiIdForRouteName(family) ?: return@mapNotNull null
            uiId to operation
        }
        .groupBy({ it.first }, { it.second })

    val detailsByFamily = routeDetails.groupBy {
        OperationalRouteResolver.baseRouteFamily(it.routeName)
    }

    return StudentRouteIds.orderedUiIds.map { uiId ->
        val family = StudentRouteIds.dbNameForUiId(uiId)
        val meta = detailsByFamily[family]?.firstOrNull {
            !it.routeName.contains("주말") && !it.routeName.contains("18시")
        } ?: detailsByFamily[family]?.firstOrNull()
        val operations = grouped[uiId].orEmpty()
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

        val fromLabel = meta?.startLocation?.takeIf { it.isNotBlank() } ?: defaultFrom(uiId)
        val toLabel = meta?.endLocation?.takeIf { it.isNotBlank() && it != fromLabel }
            ?: defaultTo(uiId)

        RouteUiModel(
            id = uiId,
            name = StudentRouteIds.displayName(uiId),
            fromLabel = fromLabel,
            toLabel = toLabel,
            status = status,
            activeVehicleCount = inProgressCount.takeIf { it > 0 },
            nextDeparture = nextDeparture,
            isFavorite = false,
            imageRes = StudentRouteIds.imageRes(uiId),
        )
    }
}

private fun defaultFrom(uiId: String): String = when (uiId) {
    StudentRouteIds.GIHEUNG -> "명지대"
    StudentRouteIds.MYEONGJI_STATION -> "명지대"
    else -> "명지대"
}

private fun defaultTo(uiId: String): String = when (uiId) {
    StudentRouteIds.GIHEUNG -> "기흥역"
    StudentRouteIds.MYEONGJI_STATION -> "명지대역"
    else -> "시내 순환"
}

private fun String.toLocalTimeOrNull(): LocalTime? {
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
