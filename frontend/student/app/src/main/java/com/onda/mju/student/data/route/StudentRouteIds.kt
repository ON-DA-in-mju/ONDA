package com.onda.mju.student.data.route

import com.onda.mju.student.R

/**
 * UI route id ↔ DB route_name 매핑.
 * 노선 탭: 시내 평일 / 시내 주말·방학을 분리해 표시.
 */
object StudentRouteIds {
    const val GIHEUNG = "giheung"
    const val MYEONGJI_STATION = "myeongji_station"
    const val CITY_SHUTTLE = "city_shuttle"
    const val CITY_SHUTTLE_VACATION = "city_shuttle_vacation"

    /** Stop-guide screens historically used shorter ids. */
    fun normalizeUiId(routeId: String): String = when (routeId) {
        "myeongji" -> MYEONGJI_STATION
        "city" -> CITY_SHUTTLE
        "city_vacation", "city_weekend" -> CITY_SHUTTLE_VACATION
        else -> routeId
    }

    fun guideUiId(routeId: String): String = when (normalizeUiId(routeId)) {
        GIHEUNG -> "giheung"
        MYEONGJI_STATION -> "myeongji"
        CITY_SHUTTLE_VACATION -> "city_vacation"
        else -> "city"
    }

    fun uiIdFromGuideId(guideId: String): String = when (guideId) {
        "giheung" -> GIHEUNG
        "myeongji" -> MYEONGJI_STATION
        "city_vacation", "city_weekend" -> CITY_SHUTTLE_VACATION
        "city" -> CITY_SHUTTLE
        else -> normalizeUiId(guideId)
    }

    fun uiIdForRouteName(routeName: String): String? = when (
        OperationalRouteResolver.canonicalRouteName(routeName)
    ) {
        OperationalRouteResolver.GIHEUNG -> GIHEUNG
        OperationalRouteResolver.MYONGJI_STATION -> MYEONGJI_STATION
        OperationalRouteResolver.CITY_SHUTTLE -> CITY_SHUTTLE
        OperationalRouteResolver.CITY_SHUTTLE_VACATION -> CITY_SHUTTLE_VACATION
        else -> null
    }

    fun dbNameForUiId(routeId: String): String = when (normalizeUiId(routeId)) {
        GIHEUNG -> OperationalRouteResolver.GIHEUNG
        MYEONGJI_STATION -> OperationalRouteResolver.MYONGJI_STATION
        CITY_SHUTTLE_VACATION -> OperationalRouteResolver.CITY_SHUTTLE_VACATION
        else -> OperationalRouteResolver.CITY_SHUTTLE
    }

    fun displayName(routeId: String): String = dbNameForUiId(routeId)

    fun imageRes(routeId: String): Int = when (normalizeUiId(routeId)) {
        GIHEUNG -> R.drawable.route_thumb_giheung
        MYEONGJI_STATION -> R.drawable.route_thumb_myeongji
        else -> R.drawable.route_thumb_city
    }

    /** 노선 탭 목록 (시내 평일 + 주말·방학 분리) */
    val routeListUiIds: List<String> = listOf(
        GIHEUNG,
        MYEONGJI_STATION,
        CITY_SHUTTLE,
        CITY_SHUTTLE_VACATION,
    )

    /** 시간표·정류장 안내 등 3개 계열 */
    val orderedUiIds: List<String> = listOf(GIHEUNG, MYEONGJI_STATION, CITY_SHUTTLE)
}
