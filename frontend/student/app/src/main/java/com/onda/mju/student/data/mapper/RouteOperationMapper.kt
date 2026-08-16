package com.onda.mju.student.data.mapper

import com.onda.mju.student.core.calendar.AcademicCalendar
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
 * 시내 평일 / 시내 주말·공휴일·방학을 각각 별도 카드로 표시.
 *
 * 상태 (operations.status 집계):
 * - RUNNING: IN_PROGRESS ≥ 1
 * - SCHEDULED: 남은 SCHEDULED ≥ 1
 * - ENDED: 그 외 (완료/취소만 있거나 남은 배차 없음)
 */
fun List<OperationDto>.toRouteUiModels(
    routeDetails: List<RouteDetailDto> = emptyList(),
): List<RouteUiModel> {
    val grouped = asSequence()
        .mapNotNull { operation ->
            val routeName = operation.schedule?.route?.routeName ?: return@mapNotNull null
            val uiId = StudentRouteIds.uiIdForRouteName(routeName) ?: return@mapNotNull null
            uiId to operation
        }
        .groupBy({ it.first }, { it.second })

    val detailsByCanonical = routeDetails.groupBy {
        OperationalRouteResolver.canonicalRouteName(it.routeName)
    }

    val now = LocalTime.now(ZoneId.of("Asia/Seoul"))

    return StudentRouteIds.routeListUiIds.map { uiId ->
        val dbName = StudentRouteIds.dbNameForUiId(uiId)
        val meta = detailsByCanonical[dbName]?.firstOrNull()
        val operations = grouped[uiId].orEmpty()
        val inProgressCount = operations.count { it.status == "IN_PROGRESS" }
        val upcomingScheduled = operations
            .asSequence()
            .filter { it.status == "SCHEDULED" }
            .mapNotNull { op ->
                val t = op.schedule?.departureTime?.toLocalTimeOrNull() ?: return@mapNotNull null
                op to t
            }
            .filter { (_, t) -> !t.isBefore(now) }
            .toList()
        val anyScheduled = operations.any { it.status == "SCHEDULED" }
        val anyFinished = operations.any { it.status == "COMPLETED" || it.status == "CANCELLED" }

        val status = when {
            inProgressCount > 0 -> RouteStatus.RUNNING
            upcomingScheduled.isNotEmpty() || (anyScheduled && !anyFinished) -> RouteStatus.SCHEDULED
            anyFinished || (operations.isNotEmpty() && inProgressCount == 0 && upcomingScheduled.isEmpty()) ->
                RouteStatus.ENDED
            routeOperatesToday(uiId) -> RouteStatus.SCHEDULED
            else -> RouteStatus.ENDED
        }

        val nextDeparture = when (status) {
            RouteStatus.ENDED -> "-"
            else -> upcomingScheduled.minByOrNull { it.second }?.second?.toUiDepartureTime()
                ?: operations
                    .asSequence()
                    .filter { it.status == "SCHEDULED" }
                    .mapNotNull { it.schedule?.departureTime?.toLocalTimeOrNull() }
                    .minOrNull()
                    ?.toUiDepartureTime()
                ?: "-"
        }

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
            imageRes = StudentRouteIds.imageRes(uiId),
        )
    }
}

private fun routeOperatesToday(uiId: String): Boolean {
    val today = AcademicCalendar.todayDateKey()
    return when (uiId) {
        StudentRouteIds.CITY_SHUTTLE -> AcademicCalendar.isSemesterWeekday(today)
        StudentRouteIds.CITY_SHUTTLE_VACATION -> AcademicCalendar.isCityVacationServiceDay(today)
        else -> true
    }
}

private fun defaultFrom(uiId: String): String = when (uiId) {
    StudentRouteIds.GIHEUNG -> "명지대"
    StudentRouteIds.MYEONGJI_STATION -> "명지대"
    StudentRouteIds.CITY_SHUTTLE_VACATION -> "생활관(명현관)"
    else -> "명지대"
}

private fun defaultTo(uiId: String): String = when (uiId) {
    StudentRouteIds.GIHEUNG -> "기흥역"
    StudentRouteIds.MYEONGJI_STATION -> "명지대역"
    StudentRouteIds.CITY_SHUTTLE_VACATION -> "시내 순환 (주말·방학)"
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
