package com.onda.mju.student.data.mapper

import com.onda.mju.student.data.remote.dto.RouteDetailDto
import com.onda.mju.student.data.remote.dto.StopDto
import com.onda.mju.student.data.remote.repository.RouteStopInfo
import com.onda.mju.student.data.route.OperationalRouteResolver
import com.onda.mju.student.data.route.StudentRouteIds
import com.onda.mju.student.ui.screen.notice.StopGuideItem
import com.onda.mju.student.ui.screen.notice.StopGuideRouteInfo

fun buildStopGuideRoutes(routes: List<RouteDetailDto>): List<StopGuideRouteInfo> {
    val byFamily = routes.groupBy { OperationalRouteResolver.baseRouteFamily(it.routeName) }
    return StudentRouteIds.orderedUiIds.map { uiId ->
        val base = StudentRouteIds.dbNameForUiId(uiId)
        val meta = byFamily[base]?.firstOrNull {
            !it.routeName.contains("주말") && !it.routeName.contains("18시")
        } ?: byFamily[base]?.firstOrNull()
        StopGuideRouteInfo(
            id = StudentRouteIds.guideUiId(uiId),
            title = StudentRouteIds.displayName(uiId),
            description = meta?.description?.takeIf { it.isNotBlank() }
                ?: defaultGuideDescription(uiId),
            thumbRes = StudentRouteIds.imageRes(uiId),
        )
    }
}

fun buildStopGuideItems(
    stops: List<StopDto>,
    routeStops: List<RouteStopInfo>,
): List<StopGuideItem> {
    if (stops.isEmpty() && routeStops.isEmpty()) return emptyList()

    val routesByStopName = routeStops
        .groupBy { it.name }
        .mapValues { (_, rows) ->
            rows.map { OperationalRouteResolver.baseRouteFamily(it.routeName) }
                .distinct()
                .mapNotNull { family ->
                    StudentRouteIds.uiIdForRouteName(family)?.let { StudentRouteIds.displayName(it) }
                }
                .distinct()
        }

    val primaryRouteIdByStop = routeStops
        .groupBy { it.name }
        .mapValues { (_, rows) ->
            val family = OperationalRouteResolver.baseRouteFamily(rows.first().routeName)
            StudentRouteIds.guideUiId(
                StudentRouteIds.uiIdForRouteName(family) ?: StudentRouteIds.CITY_SHUTTLE,
            )
        }

    val source = if (stops.isNotEmpty()) {
        stops
    } else {
        routeStops
            .distinctBy { it.name }
            .map {
                StopDto(id = it.id, stopName = it.name, latitude = it.lat, longitude = it.lng)
            }
    }

    return source.map { stop ->
        val available = routesByStopName[stop.stopName].orEmpty()
        val guideRouteId = primaryRouteIdByStop[stop.stopName]
            ?: StudentRouteIds.guideUiId(StudentRouteIds.CITY_SHUTTLE)
        val thumb = when (guideRouteId) {
            "giheung" -> StudentRouteIds.imageRes(StudentRouteIds.GIHEUNG)
            "myeongji" -> StudentRouteIds.imageRes(StudentRouteIds.MYEONGJI_STATION)
            else -> StudentRouteIds.imageRes(StudentRouteIds.CITY_SHUTTLE)
        }
        StopGuideItem(
            id = stop.id,
            routeId = guideRouteId,
            name = stop.stopName,
            address = "${stop.stopName} 인근",
            locationGuide = "${stop.stopName} 정류장에서 탑승할 수 있습니다.",
            landmarks = listOf(stop.stopName),
            availableRoutes = available.ifEmpty {
                listOf(StudentRouteIds.displayName(StudentRouteIds.CITY_SHUTTLE))
            },
            thumbRes = thumb,
        )
    }.sortedBy { it.name }
}

private fun defaultGuideDescription(uiId: String): String = when (uiId) {
    StudentRouteIds.GIHEUNG -> "명지대학교와 기흥역을 연결하는 셔틀버스 노선입니다."
    StudentRouteIds.MYEONGJI_STATION -> "명지대학교와 명지대역을 연결하는 셔틀버스 노선입니다."
    else -> "학교와 용인 시내 주요 정류장을 연결하는 시내 셔틀버스 노선입니다."
}
