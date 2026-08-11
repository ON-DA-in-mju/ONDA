package com.onda.mju.student.ui.screen.notice

import androidx.compose.ui.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Timetable domain models (Mock now).
 * Later swap [sampleTimetableRoutes] / loaders with Supabase schedules + routes.
 */

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

fun sampleTimetableRoutes(): List<TimetableRoute> = listOf(
    giheungRoute(),
    myeongjiStationRoute(),
    cityShuttleRoute(),
)

fun sampleTimetableRoute(routeId: String): TimetableRoute =
    sampleTimetableRoutes().firstOrNull { it.id == routeId } ?: sampleTimetableRoutes().first()

/**
 * Resolve row status using the same labels/colors as the previous timetable mock.
 * Approximate: before departure → 예정, within ~35 min after → 운행 중, else → 종료.
 */
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

private fun dep(time: String, count: String? = null) =
    TimetableDeparture(departureTime = time, vehicleCount = count)

private fun giheungRoute(): TimetableRoute {
    val toGiheung = TimetableDirection(
        id = "giheung_to_station",
        label = "버스관리사무소 → 기흥역 5번 출구",
    )
    val toOffice = TimetableDirection(
        id = "giheung_to_office",
        label = "기흥역 5번 출구 → 버스관리사무소",
    )
    return TimetableRoute(
        id = "giheung",
        name = "기흥역 통학버스",
        summary = "버스관리사무소 ⇄ 기흥역 5번 출구",
        weekdayDirections = listOf(toGiheung, toOffice),
        weekendDirections = listOf(toGiheung, toOffice),
        schedules = listOf(
            TimetableSchedule(
                dayType = TimetableDayType.Weekday,
                directionId = toGiheung.id,
                operates = true,
                departures = listOf(
                    dep("08:00", "1대"),
                    dep("09:05", "3대"),
                    dep("09:10", "2대"),
                    dep("10:00", "3대"),
                    dep("10:05", "2대"),
                    dep("12:00", "1대"),
                    dep("13:00", "1대"),
                    dep("14:00", "1대"),
                    dep("15:15", "2대"),
                    dep("16:15", "3대"),
                    dep("17:15", "5대"),
                    dep("18:15", "2대"),
                    dep("19:15", "1대"),
                ),
            ),
            TimetableSchedule(
                dayType = TimetableDayType.Weekday,
                directionId = toOffice.id,
                operates = true,
                departures = listOf(
                    dep("08:15", "3대"),
                    dep("08:20", "2대"),
                    dep("09:15", "3대"),
                    dep("09:20", "2대"),
                    dep("10:15", "3대"),
                    dep("10:20", "2대"),
                    dep("12:15", "1대"),
                    dep("13:15", "1대"),
                    dep("14:15", "1대"),
                    dep("15:30", "2대"),
                    dep("16:30", "3대"),
                    dep("17:30", "1대"),
                    dep("18:30", "1대"),
                    dep("19:30", "1대"),
                ),
            ),
            TimetableSchedule(
                dayType = TimetableDayType.WeekendVacation,
                directionId = toGiheung.id,
                operates = false,
            ),
            TimetableSchedule(
                dayType = TimetableDayType.WeekendVacation,
                directionId = toOffice.id,
                operates = false,
            ),
        ),
    )
}

private fun myeongjiStationRoute(): TimetableRoute {
    val toStation = TimetableDirection(
        id = "myeongji_to_station",
        label = "버스관리사무소 → 명지대역 사거리 정류장",
    )
    val toOffice = TimetableDirection(
        id = "myeongji_to_office",
        label = "진입로(역북동 주민센터) → 버스관리사무소",
    )
    // 운행구분 "명지대역" only (시내 rows excluded). No vehicle-count in source → null.
    val outboundTimes = listOf(
        "08:00", "08:15", "08:20", "08:25", "08:35", "08:45", "08:50",
        "09:00", "09:15", "09:25", "09:30", "09:35", "09:40", "09:55",
        "10:00", "10:20", "10:30", "10:40", "10:45",
        "11:00", "11:25", "11:30", "11:45", "11:55",
        "12:05", "12:20", "12:30", "12:45",
        "13:00", "13:25", "13:40",
        "14:00", "14:10", "14:15", "14:30", "14:50",
        "15:00", "15:10", "15:25", "15:30", "15:55",
        "16:10", "16:25", "16:30", "16:50",
        "17:00", "17:10", "17:20", "17:30", "17:45",
        "18:00",
    )
    val inboundTimes = listOf(
        "08:15", "08:30", "08:35", "08:40", "08:50",
        "09:00", "09:05", "09:15", "09:30", "09:40", "09:45", "09:50", "09:55",
        "10:10", "10:15", "10:35", "10:45", "10:55",
        "11:00", "11:15", "11:40", "11:45",
        "12:00", "12:10", "12:20", "12:35", "12:45",
        "13:00", "13:15", "13:40", "13:55",
        "14:15", "14:25", "14:30", "14:45",
        "15:05", "15:15", "15:25", "15:40", "15:45",
        "16:10", "16:25", "16:40", "16:45",
        "17:05", "17:15", "17:25", "17:35", "17:45",
        "18:00", "18:15",
    )
    return TimetableRoute(
        id = "myeongji_station",
        name = "명지대역 셔틀버스",
        summary = "버스관리사무소 ⇄ 명지대역 사거리",
        weekdayDirections = listOf(toStation, toOffice),
        weekendDirections = listOf(toStation, toOffice),
        schedules = listOf(
            TimetableSchedule(
                dayType = TimetableDayType.Weekday,
                directionId = toStation.id,
                operates = true,
                departures = outboundTimes.map { dep(it) },
            ),
            TimetableSchedule(
                dayType = TimetableDayType.Weekday,
                directionId = toOffice.id,
                operates = true,
                departures = inboundTimes.map { dep(it) },
            ),
            TimetableSchedule(
                dayType = TimetableDayType.WeekendVacation,
                directionId = toStation.id,
                operates = false,
            ),
            TimetableSchedule(
                dayType = TimetableDayType.WeekendVacation,
                directionId = toOffice.id,
                operates = false,
            ),
        ),
    )
}

private fun cityShuttleRoute(): TimetableRoute {
    val weekdayToParking = TimetableDirection(
        id = "city_weekday_to_parking",
        label = "버스관리사무소 → 중앙공영주차장",
    )
    val weekdayToOffice = TimetableDirection(
        id = "city_weekday_to_office",
        label = "진입로(역북동 주민센터) → 버스관리사무소",
    )
    val weekendToParking = TimetableDirection(
        id = "city_weekend_to_parking",
        label = "생활관(명현관) → 중앙공영주차장",
    )
    val weekendFromStation = TimetableDirection(
        id = "city_weekend_from_station",
        label = "경전철 명지대역 → 생활관(명현관)",
    )
    // 운행구분 "시내" only. No vehicle-count in source → null.
    val weekdayOutbound = listOf(
        "08:05", "08:55", "10:10", "11:20", "13:10", "14:20", "15:40", "16:35", "18:10",
    )
    val weekdayInbound = listOf(
        "08:20", "09:10", "10:25", "11:35", "13:25", "14:35", "15:55", "16:50", "18:25",
    )
    val weekendOutbound = listOf(
        "08:20", "09:20", "10:20", "11:20", "12:20",
        "13:20", "15:20", "16:20", "17:20", "18:00",
    )
    // 학교 도착 예정 - 10분
    val weekendFromStationTimes = listOf(
        "08:35", "09:35", "10:35", "11:35", "12:35",
        "13:35", "15:35", "16:35", "17:35", "18:15",
    )
    return TimetableRoute(
        id = "city_shuttle",
        name = "시내 셔틀버스",
        summary = "시내 순환 · 평일/주말·방학 노선 상이",
        weekdayDirections = listOf(weekdayToParking, weekdayToOffice),
        weekendDirections = listOf(weekendToParking, weekendFromStation),
        schedules = listOf(
            TimetableSchedule(
                dayType = TimetableDayType.Weekday,
                directionId = weekdayToParking.id,
                operates = true,
                departures = weekdayOutbound.map { dep(it) },
            ),
            TimetableSchedule(
                dayType = TimetableDayType.Weekday,
                directionId = weekdayToOffice.id,
                operates = true,
                departures = weekdayInbound.map { dep(it) },
            ),
            TimetableSchedule(
                dayType = TimetableDayType.WeekendVacation,
                directionId = weekendToParking.id,
                operates = true,
                departures = weekendOutbound.map { dep(it) },
            ),
            TimetableSchedule(
                dayType = TimetableDayType.WeekendVacation,
                directionId = weekendFromStation.id,
                operates = true,
                departures = weekendFromStationTimes.map { dep(it) },
            ),
        ),
    )
}
