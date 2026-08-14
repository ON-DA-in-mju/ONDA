package com.onda.mju.student.data.route

import com.onda.mju.student.core.calendar.AcademicCalendar

/**
 * DB `routes.route_name` 변형(주말·방학 시내 등)을 UI 노선과 맞춘다.
 */
object OperationalRouteResolver {
    const val CITY_SHUTTLE = "시내 셔틀"
    const val CITY_SHUTTLE_VACATION = "시내 셔틀 (주말·공휴일·방학)"
    const val MYONGJI_STATION = "명지대역 셔틀"
    const val MYONGJI_STATION_AFTER18 = "명지대역 셔틀 (18시 이후)"
    const val GIHEUNG = "기흥역 통학버스"

    /** 계열만 (기흥 / 명지대 / 시내). 시내 평일·방학은 같은 계열. */
    fun baseRouteFamily(routeName: String): String {
        val key = routeName.trim()
        return when {
            key.contains("기흥") -> GIHEUNG
            key.contains("명지대") -> MYONGJI_STATION
            key.contains("시내") -> CITY_SHUTTLE
            else -> key
        }
    }

    /** 정확한 DB 노선명 (시내 평일 vs 주말·방학 구분). */
    fun canonicalRouteName(routeName: String): String {
        val key = routeName.trim()
        return when {
            key.contains("기흥") -> GIHEUNG
            key.contains("명지대") && key.contains("18") -> MYONGJI_STATION_AFTER18
            key.contains("명지대") -> MYONGJI_STATION
            key.contains("시내") && isCityVacationName(key) -> CITY_SHUTTLE_VACATION
            key.contains("시내") -> CITY_SHUTTLE
            else -> key
        }
    }

    fun isCityVacationName(routeName: String): Boolean {
        val key = routeName.trim()
        return key.contains("주말") || key.contains("방학") || key.contains("공휴일")
    }

    /**
     * 오늘 날짜 기준으로 실제로 운행되는 시내 노선명.
     * (라이브 기본 선택·정류장 카탈로그 폴백용)
     */
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
