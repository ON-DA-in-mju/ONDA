package com.mju.onda.driver.feature.inoperation.data

/**
 * DB 조회 실패 시에만 쓰는 fallback 정류장 목록.
 * 관리자 `cityShuttleStops.ts` / Supabase route_stops 와 맞춰 둔다.
 * 노선명 해석(학기/방학·18시)은 [OperationalRouteResolver] 에서 한 뒤 이 목록을 조회한다.
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
    const val CITY_SHUTTLE_VACATION = "시내 셔틀 (주말·공휴일·방학)"
    const val MYONGJI_STATION_SHUTTLE = "명지대역 셔틀"
    const val MYONGJI_STATION_AFTER18 = "명지대역 셔틀 (18시 이후)"
    const val GIHEUNG_SHUTTLE = "기흥역 통학버스"

    val cityShuttle: List<RouteStop> = listOf(
        RouteStop("city-1", "버스관리사무소", 37.22405, 127.18735, 1),
        RouteStop("city-2", "상공회의소", 37.2318, 127.1894, 2),
        RouteStop("city-3", "진입로(럭스나인 앞)", 37.2362, 127.1915, 3),
        RouteStop("city-4", "동부경찰서 중앙지구대", 37.2349, 127.1988, 4),
        RouteStop("city-5", "용인CGV", 37.23509, 127.20561, 5),
        RouteStop("city-6", "중앙공영주차장", 37.23455, 127.2072, 6),
        RouteStop("city-7", "진입로(역북동 주민센터)", 37.23446, 127.1883, 7),
        RouteStop("city-8", "이마트", 37.23143, 127.18916, 8),
        RouteStop("city-9", "제1공학관", 37.22185, 127.18615, 9),
        RouteStop("city-10", "제3공학관", 37.22125, 127.18675, 10),
        RouteStop("city-11", "함박관", 37.22135, 127.18555, 11),
        RouteStop("city-12", "창조관", 37.22305, 127.18665, 12),
        RouteStop("city-13", "버스관리사무소", 37.22405, 127.18735, 13),
    )

    val cityShuttleVacation: List<RouteStop> = listOf(
        RouteStop("cityv-1", "생활관(명현관)", 37.22015, 127.18515, 1),
        RouteStop("cityv-2", "함박관", 37.22135, 127.18555, 2),
        RouteStop("cityv-3", "정문", 37.22455, 127.18875, 3),
        RouteStop("cityv-4", "상공회의소", 37.2318, 127.1894, 4),
        RouteStop("cityv-5", "진입로(럭스나인 앞)", 37.2362, 127.1915, 5),
        RouteStop("cityv-6", "동부경찰서 중앙지구대", 37.2349, 127.1988, 6),
        RouteStop("cityv-7", "용인CGV", 37.23509, 127.20561, 7),
        RouteStop("cityv-8", "중앙공영주차장", 37.23455, 127.2072, 8),
        RouteStop("cityv-9", "경전철 명지대역", 37.23811, 127.19057, 9),
        RouteStop("cityv-10", "진입로(역북동 주민센터)", 37.23446, 127.1883, 10),
        RouteStop("cityv-11", "이마트", 37.23143, 127.18916, 11),
        RouteStop("cityv-12", "제1공학관", 37.22185, 127.18615, 12),
        RouteStop("cityv-13", "생활관(명현관)", 37.22015, 127.18515, 13),
    )

    val myongjiStationShuttle: List<RouteStop> = listOf(
        RouteStop("mju-1", "버스관리사무소", 37.22405, 127.18735, 1),
        RouteStop("mju-2", "상공회의소", 37.2318, 127.1894, 2),
        RouteStop("mju-3", "진입로(럭스나인 앞)", 37.2362, 127.1915, 3),
        RouteStop("mju-4", "경전철 명지대역", 37.23811, 127.19057, 4),
        RouteStop("mju-5", "명지대역 사거리 정류장", 37.23755, 127.19185, 5),
        RouteStop("mju-6", "진입로(역북동 주민센터)", 37.23446, 127.1883, 6),
        RouteStop("mju-7", "이마트", 37.23143, 127.18916, 7),
        RouteStop("mju-8", "명진당", 37.22255, 127.18695, 8),
        RouteStop("mju-9", "제3공학관", 37.22125, 127.18675, 9),
        RouteStop("mju-10", "함박관", 37.22135, 127.18555, 10),
        RouteStop("mju-11", "창조관", 37.22305, 127.18665, 11),
        RouteStop("mju-12", "버스관리사무소", 37.22405, 127.18735, 12),
    )

    val giheungShuttle: List<RouteStop> = listOf(
        RouteStop("gh-1", "채플관 앞", 37.22415, 127.18705, 1),
        RouteStop("gh-2", "기흥역 5번 출구", 37.27597, 127.11669, 2),
        RouteStop("gh-3", "채플관 앞", 37.22415, 127.18705, 3),
    )

    fun stopsForRouteName(routeName: String): List<RouteStop> {
        val key = routeName.trim()
        return when {
            key == CITY_SHUTTLE_VACATION ||
                (key.contains("시내") &&
                    (key.contains("방학") || key.contains("주말") || key.contains("공휴일"))) ->
                cityShuttleVacation
            key == MYONGJI_STATION_AFTER18 ||
                (key.contains("명지대") && key.contains("18")) ->
                myongjiStationShuttle
            key.contains("명지대") -> myongjiStationShuttle
            key.contains("기흥") -> giheungShuttle
            key == CITY_SHUTTLE || key.contains("시내") -> cityShuttle
            else -> cityShuttle
        }
    }
}
