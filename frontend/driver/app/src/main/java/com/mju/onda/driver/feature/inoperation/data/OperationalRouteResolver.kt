package com.mju.onda.driver.feature.inoperation.data

import com.mju.onda.driver.core.calendar.AcademicCalendar

/**
 * routes.description / route_name 규칙에 맞는 실제 운행 노선명 해석.
 *
 * - 기흥역 통학버스: 학기 중 평일만 (변형 없음)
 * - 명지대역 셔틀: 학기 중 평일 · 18:00 이후면 `(18시 이후)`
 * - 시내 셔틀: 학기(계절학기 포함) 중 평일만
 * - 시내 셔틀 (주말·공휴일·방학): 주말·공휴일·방학만 (평일 시내와 겹치지 않음)
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

    fun isCityVacationName(routeName: String): Boolean {
        val key = routeName.trim()
        return key.contains("주말") || key.contains("방학") || key.contains("공휴일")
    }

    /** 계열만 (기흥 / 명지대 / 시내). 시내 평일·방학은 서로 다른 운행. */
    fun baseRouteFamily(routeName: String): String {
        val key = routeName.trim()
        return when {
            key.contains("기흥") -> GIHEUNG
            key.contains("명지대") -> MYONGJI_STATION
            key.contains("시내") -> CITY_SHUTTLE
            else -> key
        }
    }

    /** DB 노선명 그대로 (시내 평일 vs 주말·방학을 섞지 않음). */
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

    /**
     * 운행일 기준으로 실제로 써야 하는 시내 노선명.
     * 평일 시내와 주말·방학 시내는 같은 날 동시에 쓰지 않는다.
     * [date] = YYYY-MM-DD (운행일), [departureTime] = HH:mm 또는 HH:mm:ss
     */
    fun resolveOperationalRouteName(
        routeNameFromAssignment: String,
        departureTime: String,
        date: String = AcademicCalendar.todayDateKey(),
    ): String {
        val raw = routeNameFromAssignment.trim()
        if (raw.isBlank()) return raw

        return when (baseRouteFamily(raw)) {
            GIHEUNG -> GIHEUNG

            MYONGJI_STATION, MYONGJI_STATION_AFTER18 -> {
                // DB: 「18시 이후」 노선은 is_active=false (공지상 18:10까지 동일 구간)
                MYONGJI_STATION
            }

            CITY_SHUTTLE -> {
                if (AcademicCalendar.isCityVacationServiceDay(date)) CITY_SHUTTLE_VACATION
                else CITY_SHUTTLE
            }

            else -> raw
        }
    }
}
