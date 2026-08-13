package com.onda.mju.student.data.route

import com.onda.mju.student.core.calendar.AcademicCalendar

/**
 * DB `routes.route_name` 변형(주말·방학 시내 등)을 UI 노선 계열과 맞춘다.
 */
object OperationalRouteResolver {
    const val CITY_SHUTTLE = "시내 셔틀"
    const val CITY_SHUTTLE_VACATION = "시내 셔틀 (주말·공휴일·방학)"
    const val MYONGJI_STATION = "명지대역 셔틀"
    const val MYONGJI_STATION_AFTER18 = "명지대역 셔틀 (18시 이후)"
    const val GIHEUNG = "기흥역 통학버스"

    fun baseRouteFamily(routeName: String): String {
        val key = routeName.trim()
        return when {
            key.contains("기흥") -> GIHEUNG
            key.contains("명지대") -> MYONGJI_STATION
            key.contains("시내") -> CITY_SHUTTLE
            else -> key
        }
    }

    fun resolveOperationalRouteName(
        routeNameFromAssignment: String,
        date: String = AcademicCalendar.todayDateKey(),
    ): String {
        val raw = routeNameFromAssignment.trim()
        if (raw.isBlank()) return raw
        return when (baseRouteFamily(raw)) {
            GIHEUNG -> GIHEUNG
            MYONGJI_STATION, MYONGJI_STATION_AFTER18 -> MYONGJI_STATION
            CITY_SHUTTLE, CITY_SHUTTLE_VACATION -> {
                if (AcademicCalendar.isCityVacationServiceDay(date)) CITY_SHUTTLE_VACATION
                else CITY_SHUTTLE
            }
            else -> raw
        }
    }
}
