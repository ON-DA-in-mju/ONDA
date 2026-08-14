package com.mju.onda.driver.feature.inoperation.data

import com.mju.onda.driver.core.calendar.AcademicCalendar

/**
 * routes.description / route_name 규칙에 맞는 실제 운행 노선명 해석.
 *
 * - 기흥역 통학버스: 학기 중 평일만 (변형 없음)
 * - 명지대역 셔틀: 학기 중 평일 · 18:00 이후면 `(18시 이후)`
 * - 시내 셔틀: 학기 중 평일 / 주말·공휴일·방학 변형
 */
object OperationalRouteResolver {
    const val CITY_SHUTTLE = "시내 셔틀"
    const val CITY_SHUTTLE_VACATION = "시내 셔틀 (주말·공휴일·방학)"
    const val MYONGJI_STATION = "명지대역 셔틀"
    const val MYONGJI_STATION_AFTER18 = "명지대역 셔틀 (18시 이후)"
    const val GIHEUNG = "기흥역 통학버스"

    private const val AFTER18_MINUTES = 18 * 60

    fun departureToMinutes(departureTime: String): Int {
        val parts = departureTime.trim().take(5).split(":")
        val hh = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val mm = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hh * 60 + mm
    }

    fun isAfter18Departure(departureTime: String): Boolean =
        departureToMinutes(departureTime) >= AFTER18_MINUTES

    fun baseRouteFamily(routeName: String): String {
        val key = routeName.trim()
        return when {
            key.contains("기흥") -> GIHEUNG
            key.contains("명지대") -> MYONGJI_STATION
            key.contains("시내") -> CITY_SHUTTLE
            else -> key
        }
    }

    /**
     * DB `routes.route_name` 에 대응하는 실제 운행 노선명.
     * [date] = YYYY-MM-DD (운행일), [departureTime] = HH:mm 또는 HH:mm:ss
     */
    fun resolveOperationalRouteName(
        routeNameFromAssignment: String,
        departureTime: String,
        date: String = AcademicCalendar.todayDateKey(),
    ): String {
        val raw = routeNameFromAssignment.trim()
        if (raw.isBlank()) return raw

        val family = baseRouteFamily(raw)

        return when (family) {
            GIHEUNG -> GIHEUNG

            MYONGJI_STATION, MYONGJI_STATION_AFTER18 -> {
                // DB: 「18시 이후」 노선은 is_active=false (공지상 18:10까지 동일 구간)
                MYONGJI_STATION
            }

            CITY_SHUTTLE, CITY_SHUTTLE_VACATION -> {
                if (AcademicCalendar.isCityVacationServiceDay(date)) CITY_SHUTTLE_VACATION
                else CITY_SHUTTLE
            }

            else -> raw
        }
    }
}
