package com.onda.mju.student.data.route

import com.onda.mju.student.core.calendar.AcademicCalendar
import com.onda.mju.student.data.remote.repository.RouteStopInfo
import com.onda.mju.student.ui.screen.route.RouteStopConfig

/**
 * DB `route_stops` 결과를 캐시하고, UI는 [config] / [stopNames] 로 조회한다.
 * 비어 있으면 하드코딩 fallback.
 */
object RouteStopCatalog {
    @Volatile
    private var configsByUiId: Map<String, RouteStopConfig> = emptyMap()

    @Volatile
    private var stopsByRouteName: Map<String, List<RouteStopInfo>> = emptyMap()

    fun update(
        stopsByRouteName: Map<String, List<RouteStopInfo>>,
        date: String = AcademicCalendar.todayDateKey(),
    ) {
        this.stopsByRouteName = stopsByRouteName
        val built = LinkedHashMap<String, RouteStopConfig>()
        for (uiId in StudentRouteIds.orderedUiIds) {
            val operationalName = OperationalRouteResolver.resolveOperationalRouteName(
                StudentRouteIds.dbNameForUiId(uiId),
                date = date,
            )
            val stops = stopsByRouteName[operationalName]
                ?: stopsByRouteName[StudentRouteIds.dbNameForUiId(uiId)]
                ?: emptyList()
            if (stops.isNotEmpty()) {
                built[uiId] = configFromStops(uiId, stops)
            }
        }
        configsByUiId = built
    }

    fun config(routeId: String): RouteStopConfig {
        val id = StudentRouteIds.normalizeUiId(routeId)
        return configsByUiId[id] ?: fallbackConfig(id)
    }

    fun stopInfos(routeId: String): List<RouteStopInfo> {
        val id = StudentRouteIds.normalizeUiId(routeId)
        val operationalName = OperationalRouteResolver.resolveOperationalRouteName(
            StudentRouteIds.dbNameForUiId(id),
        )
        return stopsByRouteName[operationalName]
            ?: stopsByRouteName[StudentRouteIds.dbNameForUiId(id)]
            ?: emptyList()
    }

    fun hasDbData(): Boolean = configsByUiId.isNotEmpty()

    fun configFromStops(routeId: String, stops: List<RouteStopInfo>): RouteStopConfig {
        val names = stops.map { it.name }
        val from = names.firstOrNull().orEmpty()
        val to = names.lastOrNull().orEmpty()
        return RouteStopConfig(
            routeId = StudentRouteIds.normalizeUiId(routeId),
            outboundFrom = from,
            outboundTo = to,
            inboundFrom = "",
            inboundTo = "",
            outboundStops = names,
            inboundStops = emptyList(),
        )
    }

    /** Offline / DB miss fallback — DB 시드와 같은 순환 순서. */
    fun fallbackConfig(routeId: String): RouteStopConfig = when (StudentRouteIds.normalizeUiId(routeId)) {
        StudentRouteIds.GIHEUNG -> RouteStopConfig(
            routeId = StudentRouteIds.GIHEUNG,
            outboundFrom = "채플관 앞",
            outboundTo = "채플관 앞",
            inboundFrom = "",
            inboundTo = "",
            outboundStops = listOf("채플관 앞", "기흥역 5번 출구", "채플관 앞"),
            inboundStops = emptyList(),
        )
        StudentRouteIds.MYEONGJI_STATION -> RouteStopConfig(
            routeId = StudentRouteIds.MYEONGJI_STATION,
            outboundFrom = "버스관리사무소",
            outboundTo = "버스관리사무소",
            inboundFrom = "",
            inboundTo = "",
            outboundStops = listOf(
                "버스관리사무소",
                "상공회의소",
                "진입로(럭스나인 앞)",
                "경전철 명지대역",
                "명지대역 사거리 정류장",
                "진입로(역북동 주민센터)",
                "이마트",
                "명진당",
                "제3공학관",
                "함박관",
                "창조관",
                "버스관리사무소",
            ),
            inboundStops = emptyList(),
        )
        else -> RouteStopConfig(
            routeId = StudentRouteIds.CITY_SHUTTLE,
            outboundFrom = "버스관리사무소",
            outboundTo = "버스관리사무소",
            inboundFrom = "",
            inboundTo = "",
            outboundStops = listOf(
                "버스관리사무소",
                "상공회의소",
                "진입로(럭스나인 앞)",
                "동부경찰서 중앙지구대",
                "용인CGV",
                "중앙공영주차장",
                "진입로(역북동 주민센터)",
                "이마트",
                "제1공학관",
                "제3공학관",
                "함박관",
                "창조관",
                "버스관리사무소",
            ),
            inboundStops = emptyList(),
        )
    }
}
