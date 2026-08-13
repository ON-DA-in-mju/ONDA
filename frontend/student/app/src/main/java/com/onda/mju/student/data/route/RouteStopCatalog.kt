package com.onda.mju.student.data.route

import com.onda.mju.student.core.calendar.AcademicCalendar
import com.onda.mju.student.data.remote.repository.RouteStopInfo
import com.onda.mju.student.ui.screen.route.RouteStopConfig
import com.onda.mju.student.ui.screen.route.StopWaypoint

/**
 * DB `route_stops` + `stops` 캐시.
 * 정류장 이름·순서·위경도는 DB 값만 사용한다 (하드코딩 목록 없음).
 */
object RouteStopCatalog {
    @Volatile
    private var configsByUiId: Map<String, RouteStopConfig> = emptyMap()

    @Volatile
    private var infosByUiId: Map<String, List<RouteStopInfo>> = emptyMap()

    @Volatile
    private var revision: Int = 0

    fun revision(): Int = revision

    fun update(
        stopsByRouteName: Map<String, List<RouteStopInfo>>,
        date: String = AcademicCalendar.todayDateKey(),
    ) {
        val configs = LinkedHashMap<String, RouteStopConfig>()
        val infos = LinkedHashMap<String, List<RouteStopInfo>>()
        for (uiId in StudentRouteIds.orderedUiIds) {
            val operationalName = OperationalRouteResolver.resolveOperationalRouteName(
                StudentRouteIds.dbNameForUiId(uiId),
                date = date,
            )
            val stops = stopsByRouteName[operationalName]
                ?: stopsByRouteName[StudentRouteIds.dbNameForUiId(uiId)]
                ?: emptyList()
            if (stops.isNotEmpty()) {
                infos[uiId] = stops
                configs[uiId] = configFromStops(uiId, stops)
            }
        }
        infosByUiId = infos
        configsByUiId = configs
        revision += 1
    }

    fun config(routeId: String): RouteStopConfig {
        val id = StudentRouteIds.normalizeUiId(routeId)
        return configsByUiId[id] ?: emptyConfig(id)
    }

    fun stopInfos(routeId: String): List<RouteStopInfo> {
        val id = StudentRouteIds.normalizeUiId(routeId)
        return infosByUiId[id].orEmpty()
    }

    /** DB lat/lng 를 그대로 쓰는 GPS waypoint. */
    fun waypoints(routeId: String): List<StopWaypoint> {
        val id = StudentRouteIds.normalizeUiId(routeId)
        return stopInfos(id).mapIndexed { index, stop ->
            StopWaypoint(
                id = stop.id.ifBlank { "${id}_$index" },
                name = stop.name,
                latitude = stop.lat,
                longitude = stop.lng,
            )
        }
    }

    fun hasDbData(): Boolean = infosByUiId.isNotEmpty()

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

    /** DB 미로드 시 빈 설정 (하드코딩 목록 없음). */
    fun emptyConfig(routeId: String): RouteStopConfig {
        val id = StudentRouteIds.normalizeUiId(routeId)
        return RouteStopConfig(
            routeId = id,
            outboundFrom = "",
            outboundTo = "",
            inboundFrom = "",
            inboundTo = "",
            outboundStops = emptyList(),
            inboundStops = emptyList(),
        )
    }
}
