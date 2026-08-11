package com.mju.onda.driver.feature.inoperation.data

/**
 * 관리자 웹 `cityShuttleStops.ts` 와 동일한 노선·정류장 좌표.
 * 운행 중 정류장 노선 보기 / GPS 진행 판정에 사용.
 */
data class RouteStop(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val order: Int,
)

object RouteStopsCatalog {
    const val CITY_SHUTTLE = "시내 셔틀"
    const val MYONGJI_STATION_SHUTTLE = "명지대역 셔틀"
    const val GIHEUNG_SHUTTLE = "기흥역 통학버스"

    val cityShuttle: List<RouteStop> = listOf(
        RouteStop("city-1", "버스관리사무소", 37.2245, 127.1878, 1),
        RouteStop("city-2", "이마트·상공회의소", 37.2301, 127.1889, 2),
        RouteStop("city-3", "역북동", 37.2335, 127.1895, 3),
        RouteStop("city-4", "동부경찰서", 37.24129, 127.18094, 4),
        RouteStop("city-5", "용인 CGV", 37.23509, 127.20561, 5),
        RouteStop("city-6", "중앙공영주차장", 37.23435, 127.20318, 6),
        RouteStop("city-7", "제3공학관", 37.2215, 127.1868, 7),
    )

    val myongjiStationShuttle: List<RouteStop> = listOf(
        RouteStop("mju-1", "버스관리사무소", 37.2245, 127.1878, 1),
        RouteStop("mju-2", "이마트·상공회의소", 37.2301, 127.1889, 2),
        RouteStop("mju-3", "역북동행정복지센터 건너편", 37.23355, 127.18895, 3),
        RouteStop("mju-4", "명지대역", 37.2381, 127.1905, 4),
        RouteStop("mju-5", "역북동행정복지센터 앞", 37.23345, 127.19005, 5),
        RouteStop("mju-6", "이마트 건너편", 37.22995, 127.18955, 6),
        RouteStop("mju-7", "명진당", 37.2228, 127.1875, 7),
        RouteStop("mju-8", "제3공학관", 37.2215, 127.1868, 8),
    )

    val giheungShuttle: List<RouteStop> = listOf(
        RouteStop("gh-1", "캠퍼스", 37.2248, 127.187, 1),
        RouteStop("gh-2", "기흥역 5번 출구", 37.2754, 127.1159, 2),
    )

    fun stopsForRouteName(routeName: String): List<RouteStop> {
        val key = routeName.trim()
        return when {
            key.contains("명지대") -> myongjiStationShuttle
            key.contains("기흥") -> giheungShuttle
            key.contains("시내") -> cityShuttle
            else -> cityShuttle
        }
    }
}
