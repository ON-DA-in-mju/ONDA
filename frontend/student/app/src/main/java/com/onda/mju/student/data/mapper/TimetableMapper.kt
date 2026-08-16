package com.onda.mju.student.data.mapper

import com.onda.mju.student.data.remote.dto.RouteDetailDto
import com.onda.mju.student.data.remote.dto.ScheduleDetailDto
import com.onda.mju.student.data.remote.repository.RouteStopInfo
import com.onda.mju.student.data.route.OperationalRouteResolver
import com.onda.mju.student.data.route.StudentRouteIds
import com.onda.mju.student.ui.screen.notice.TimetableDayType
import com.onda.mju.student.ui.screen.notice.TimetableDeparture
import com.onda.mju.student.ui.screen.notice.TimetableDirection
import com.onda.mju.student.ui.screen.notice.TimetableRoute
import com.onda.mju.student.ui.screen.notice.TimetableSchedule

private val Weekdays = setOf("MON", "TUE", "WED", "THU", "FRI")
private val Weekend = setOf("SAT", "SUN")

fun buildTimetableRoutes(
    schedules: List<ScheduleDetailDto>,
    routes: List<RouteDetailDto> = emptyList(),
    routeStops: Map<String, List<RouteStopInfo>> = emptyMap(),
): List<TimetableRoute> {
    if (schedules.isEmpty() && routes.isEmpty()) return emptyList()

    return StudentRouteIds.orderedUiIds.map { uiId ->
        val baseName = StudentRouteIds.dbNameForUiId(uiId)
        val routeMeta = routes.firstOrNull {
            OperationalRouteResolver.baseRouteFamily(it.routeName) == baseName &&
                !it.routeName.contains("주말") &&
                !it.routeName.contains("18시")
        } ?: routes.firstOrNull {
            OperationalRouteResolver.baseRouteFamily(it.routeName) == baseName
        }

        val weekdayTimes = schedules
            .asSequence()
            .filter { row ->
                val name = row.routes?.routeName ?: return@filter false
                val family = OperationalRouteResolver.baseRouteFamily(name)
                if (family != baseName) return@filter false
                if (baseName == OperationalRouteResolver.CITY_SHUTTLE &&
                    OperationalRouteResolver.isCityVacationName(name)
                ) {
                    return@filter false
                }
                row.semester.equals("SEMESTER", ignoreCase = true) &&
                    row.weekday.uppercase() in Weekdays
            }
            .map { formatHm(it.departureTime) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()

        val weekendTimes = schedules
            .asSequence()
            .filter { row ->
                val name = row.routes?.routeName ?: return@filter false
                val family = OperationalRouteResolver.baseRouteFamily(name)
                if (family != baseName) return@filter false
                if (baseName == OperationalRouteResolver.CITY_SHUTTLE) {
                    return@filter OperationalRouteResolver.isCityVacationName(name)
                }
                val weekendish =
                    row.weekday.uppercase() in Weekend ||
                        row.semester.equals("VACATION", ignoreCase = true) ||
                        name.contains("주말") ||
                        name.contains("방학")
                weekendish
            }
            .map { formatHm(it.departureTime) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()

        val weekdayLabel = directionLabel(baseName, routeMeta, routeStops, vacation = false)
        val weekendLabel = directionLabel(baseName, routeMeta, routeStops, vacation = true)
        val weekdayDir = TimetableDirection(id = "${uiId}_weekday", label = weekdayLabel)
        val weekendDir = TimetableDirection(id = "${uiId}_weekend", label = weekendLabel)

        TimetableRoute(
            id = uiId,
            name = StudentRouteIds.displayName(uiId),
            summary = summaryFor(uiId, routeMeta),
            weekdayDirections = listOf(weekdayDir),
            weekendDirections = listOf(weekendDir),
            schedules = listOf(
                TimetableSchedule(
                    dayType = TimetableDayType.Weekday,
                    directionId = weekdayDir.id,
                    operates = weekdayTimes.isNotEmpty(),
                    departures = weekdayTimes.map { TimetableDeparture(it) },
                ),
                TimetableSchedule(
                    dayType = TimetableDayType.WeekendVacation,
                    directionId = weekendDir.id,
                    operates = weekendTimes.isNotEmpty(),
                    departures = weekendTimes.map { TimetableDeparture(it) },
                ),
            ),
        )
    }
}

private fun directionLabel(
    baseName: String,
    routeMeta: RouteDetailDto?,
    routeStops: Map<String, List<RouteStopInfo>>,
    vacation: Boolean,
): String {
    val operational = if (vacation && baseName == OperationalRouteResolver.CITY_SHUTTLE) {
        OperationalRouteResolver.CITY_SHUTTLE_VACATION
    } else {
        baseName
    }
    val stops = routeStops[operational].orEmpty()
    if (stops.size >= 2) {
        return "${stops.first().name} → ${stops.last().name}"
    }
    val start = routeMeta?.startLocation?.takeIf { it.isNotBlank() }
    val end = routeMeta?.endLocation?.takeIf { it.isNotBlank() }
    if (start != null && end != null) return "$start → $end"
    return when (baseName) {
        OperationalRouteResolver.GIHEUNG -> "채플관 앞 → 기흥역 5번 출구"
        OperationalRouteResolver.MYONGJI_STATION -> "버스관리사무소 → 명지대역 사거리 정류장"
        else -> if (vacation) "생활관(명현관) → 중앙공영주차장" else "버스관리사무소 → 중앙공영주차장"
    }
}

private fun summaryFor(uiId: String, routeMeta: RouteDetailDto?): String {
    val start = routeMeta?.startLocation
    val end = routeMeta?.endLocation
    if (!start.isNullOrBlank() && !end.isNullOrBlank()) {
        return if (start == end) "$start 순환" else "$start ⇄ $end"
    }
    return when (uiId) {
        StudentRouteIds.GIHEUNG -> "채플관 앞 ⇄ 기흥역 5번 출구"
        StudentRouteIds.MYEONGJI_STATION -> "버스관리사무소 ⇄ 명지대역"
        else -> "시내 순환 · 평일/주말·방학 노선 상이"
    }
}

private fun formatHm(raw: String): String {
    val parts = raw.trim().split(':')
    if (parts.size < 2) return raw.trim().take(5)
    val hh = parts[0].toIntOrNull() ?: return raw.take(5)
    val mm = parts[1].toIntOrNull() ?: return raw.take(5)
    return "%02d:%02d".format(hh, mm)
}
