package com.onda.mju.student.data.mapper

import com.onda.mju.student.data.remote.dto.RouteDetailDto
import com.onda.mju.student.data.remote.dto.StopDto
import com.onda.mju.student.data.remote.repository.RouteStopInfo
import com.onda.mju.student.data.route.OperationalRouteResolver
import com.onda.mju.student.data.route.StudentRouteIds
import com.onda.mju.student.ui.screen.notice.StopGuideItem
import com.onda.mju.student.ui.screen.notice.StopGuideRouteInfo
import com.onda.mju.student.ui.screen.notice.StopPhotoResources

fun buildStopGuideRoutes(routes: List<RouteDetailDto>): List<StopGuideRouteInfo> {
    val byCanonical = routes.groupBy {
        OperationalRouteResolver.canonicalRouteName(it.routeName)
    }
    return StudentRouteIds.routeListUiIds.map { uiId ->
        val dbName = StudentRouteIds.dbNameForUiId(uiId)
        val meta = byCanonical[dbName]?.firstOrNull()
        StopGuideRouteInfo(
            id = StudentRouteIds.guideUiId(uiId),
            title = StudentRouteIds.displayName(uiId),
            description = meta?.description?.takeIf { it.isNotBlank() }
                ?: defaultGuideDescription(uiId),
            thumbRes = StudentRouteIds.imageRes(uiId),
        )
    }
}

/**
 * 정류장 안내 목록.
 * - `route_stops`에 연결된 정류장만 사용
 * - 시내 평일 / 시내 주말·방학은 각각 분리
 */
fun buildStopGuideItems(
    stops: List<StopDto>,
    routeStops: List<RouteStopInfo>,
): List<StopGuideItem> {
    if (routeStops.isEmpty()) return emptyList()

    val stopByName = stops.associateBy { it.stopName.trim() }
    val stopById = stops.associateBy { it.id }

    data class GuideStopKey(val guideRouteId: String, val stopKey: String, val name: String)

    return routeStops
        .mapNotNull { row ->
            val name = row.name.trim()
            if (name.isBlank()) return@mapNotNull null
            val uiId = StudentRouteIds.uiIdForRouteName(row.routeName) ?: return@mapNotNull null
            val guideId = StudentRouteIds.guideUiId(uiId)
            val stopKey = row.id.ifBlank { name }
            GuideStopKey(guideId, stopKey, name) to row
        }
        .groupBy({ it.first }, { it.second })
        .map { (key, rows) ->
            val meta = stopById[key.stopKey] ?: stopByName[key.name]
            val stopId = meta?.id?.takeIf { it.isNotBlank() }
                ?: key.stopKey.ifBlank { "stop-${key.name}" }
            val lat = meta?.latitude ?: rows.first().lat
            val lng = meta?.longitude ?: rows.first().lng

            val available = rows
                .mapNotNull { row ->
                    StudentRouteIds.uiIdForRouteName(row.routeName)?.let { StudentRouteIds.displayName(it) }
                }
                .distinct()

            val thumb = StopPhotoResources.forStopName(key.name)

            StopGuideItem(
                id = "${stopId}__${key.guideRouteId}",
                routeId = key.guideRouteId,
                name = key.name,
                address = formatStopAddress(key.name, lat, lng),
                locationGuide = "${key.name} 정류장에서 탑승할 수 있습니다.",
                landmarks = listOf(key.name),
                availableRoutes = available.ifEmpty {
                    listOf(
                        StudentRouteIds.displayName(
                            StudentRouteIds.uiIdFromGuideId(key.guideRouteId),
                        ),
                    )
                },
                thumbRes = thumb,
            )
        }
        .sortedWith(compareBy({ it.routeId }, { it.name }))
}

private fun formatStopAddress(name: String, lat: Double, lng: Double): String {
    if (lat == 0.0 && lng == 0.0) return "$name 인근"
    return "$name · %.5f, %.5f".format(lat, lng)
}

private fun defaultGuideDescription(uiId: String): String = when (uiId) {
    StudentRouteIds.GIHEUNG -> "명지대학교와 기흥역을 연결하는 셔틀버스 노선입니다."
    StudentRouteIds.MYEONGJI_STATION -> "명지대학교와 명지대역을 연결하는 셔틀버스 노선입니다."
    StudentRouteIds.CITY_SHUTTLE_VACATION ->
        "공휴일·주말·방학 중 운행하는 시내 셔틀버스 노선입니다."
    else -> "학기 중 평일에 운행하는 시내 셔틀버스 노선입니다."
}
