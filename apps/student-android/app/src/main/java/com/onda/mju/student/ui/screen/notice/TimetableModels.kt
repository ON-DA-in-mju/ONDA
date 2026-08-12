package com.onda.mju.student.ui.screen.notice

import androidx.compose.ui.graphics.Color
import com.onda.mju.student.data.route.RouteStopCatalog
import com.onda.mju.student.data.route.StudentRouteIds
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class TimetableDayType {
    Weekday,
    WeekendVacation,
}

data class TimetableDirection(
    val id: String,
    val label: String,
)

data class TimetableDeparture(
    val departureTime: String,
    /** Null when source data has no vehicle-count column → UI shows "-". */
    val vehicleCount: String? = null,
)

data class TimetableSchedule(
    val dayType: TimetableDayType,
    val directionId: String,
    val operates: Boolean,
    val departures: List<TimetableDeparture> = emptyList(),
)

data class TimetableRoute(
    val id: String,
    val name: String,
    val summary: String,
    val weekdayDirections: List<TimetableDirection>,
    val weekendDirections: List<TimetableDirection>,
    val schedules: List<TimetableSchedule>,
)

data class TimetableRowUi(
    val sequence: Int,
    val departureTime: String,
    val vehicleCountLabel: String,
    val statusLabel: String,
    val statusColor: Color,
    val statusBg: Color,
)

private val StatusScheduledColor = Color(0xFF0041F1)
private val StatusScheduledBg = Color(0xFFEDF4FE)
private val StatusRunningColor = Color(0xFF0F766E)
private val StatusRunningBg = Color(0xFFD1FAE5)
private val StatusEndedColor = Color(0xFF6B7280)
private val StatusEndedBg = Color(0xFFF3F4F6)

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun TimetableRoute.directionsFor(dayType: TimetableDayType): List<TimetableDirection> =
    when (dayType) {
        TimetableDayType.Weekday -> weekdayDirections
        TimetableDayType.WeekendVacation ->
            weekendDirections.ifEmpty { weekdayDirections }
    }

fun TimetableRoute.findSchedule(
    dayType: TimetableDayType,
    directionId: String,
): TimetableSchedule? =
    schedules.firstOrNull { it.dayType == dayType && it.directionId == directionId }
        ?: schedules.firstOrNull { it.dayType == dayType }

/** Offline placeholder when schedules fetch failed / empty. */
fun emptyTimetableRoutes(): List<TimetableRoute> =
    StudentRouteIds.orderedUiIds.map { uiId ->
        val config = RouteStopCatalog.config(uiId)
        val label = if (config.outboundStops.isNotEmpty()) {
            "${config.outboundFrom} → ${config.outboundTo}"
        } else {
            StudentRouteIds.displayName(uiId)
        }
        val dir = TimetableDirection(id = "${uiId}_main", label = label)
        TimetableRoute(
            id = uiId,
            name = StudentRouteIds.displayName(uiId),
            summary = label,
            weekdayDirections = listOf(dir),
            weekendDirections = listOf(dir),
            schedules = listOf(
                TimetableSchedule(TimetableDayType.Weekday, dir.id, operates = false),
                TimetableSchedule(TimetableDayType.WeekendVacation, dir.id, operates = false),
            ),
        )
    }

@Deprecated("Use DB-backed timetable from ScheduleRepository", ReplaceWith("emptyTimetableRoutes()"))
fun sampleTimetableRoutes(): List<TimetableRoute> = emptyTimetableRoutes()

fun sampleTimetableRoute(routeId: String): TimetableRoute =
    emptyTimetableRoutes().firstOrNull { it.id == routeId } ?: emptyTimetableRoutes().first()

fun resolveTimetableStatus(
    departureTime: String,
    now: LocalTime = LocalTime.now(),
): Triple<String, Color, Color> {
    val departure = runCatching { LocalTime.parse(departureTime, TimeFormatter) }
        .getOrNull()
        ?: return Triple("운행 예정", StatusScheduledColor, StatusScheduledBg)

    return when {
        now.isBefore(departure) ->
            Triple("운행 예정", StatusScheduledColor, StatusScheduledBg)
        now.isBefore(departure.plusMinutes(35)) ->
            Triple("운행 중", StatusRunningColor, StatusRunningBg)
        else ->
            Triple("운행 종료", StatusEndedColor, StatusEndedBg)
    }
}

fun List<TimetableDeparture>.toRowUi(
    now: LocalTime = LocalTime.now(),
): List<TimetableRowUi> =
    mapIndexed { index, departure ->
        val (label, color, bg) = resolveTimetableStatus(departure.departureTime, now)
        TimetableRowUi(
            sequence = index + 1,
            departureTime = departure.departureTime,
            vehicleCountLabel = departure.vehicleCount ?: "-",
            statusLabel = label,
            statusColor = color,
            statusBg = bg,
        )
    }
