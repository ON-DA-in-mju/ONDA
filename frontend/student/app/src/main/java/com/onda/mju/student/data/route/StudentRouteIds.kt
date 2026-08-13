package com.onda.mju.student.data.route

import com.onda.mju.student.R

/**
 * UI route id ↔ DB route_name (계열) 매핑.
 */
object StudentRouteIds {
    const val GIHEUNG = "giheung"
    const val MYEONGJI_STATION = "myeongji_station"
    const val CITY_SHUTTLE = "city_shuttle"

    /** Stop-guide screens historically used shorter ids. */
    fun normalizeUiId(routeId: String): String = when (routeId) {
        "myeongji" -> MYEONGJI_STATION
        "city" -> CITY_SHUTTLE
        else -> routeId
    }

    fun guideUiId(routeId: String): String = when (normalizeUiId(routeId)) {
        GIHEUNG -> "giheung"
        MYEONGJI_STATION -> "myeongji"
        else -> "city"
    }

    fun uiIdForRouteName(routeName: String): String? = when (OperationalRouteResolver.baseRouteFamily(routeName)) {
        OperationalRouteResolver.GIHEUNG -> GIHEUNG
        OperationalRouteResolver.MYONGJI_STATION -> MYEONGJI_STATION
        OperationalRouteResolver.CITY_SHUTTLE -> CITY_SHUTTLE
        else -> null
    }

    fun dbNameForUiId(routeId: String): String = when (normalizeUiId(routeId)) {
        GIHEUNG -> OperationalRouteResolver.GIHEUNG
        MYEONGJI_STATION -> OperationalRouteResolver.MYONGJI_STATION
        else -> OperationalRouteResolver.CITY_SHUTTLE
    }

    fun displayName(routeId: String): String = dbNameForUiId(routeId)

    fun imageRes(routeId: String): Int = when (normalizeUiId(routeId)) {
        GIHEUNG -> R.drawable.route_thumb_giheung
        MYEONGJI_STATION -> R.drawable.route_thumb_myeongji
        else -> R.drawable.route_thumb_city
    }

    val orderedUiIds: List<String> = listOf(GIHEUNG, MYEONGJI_STATION, CITY_SHUTTLE)
}
