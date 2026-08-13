package com.onda.mju.student.ui.screen.notice

import androidx.annotation.DrawableRes
import com.onda.mju.student.data.route.StudentRouteIds

data class StopGuideRouteInfo(
    val id: String,
    val title: String,
    val description: String,
    @param:DrawableRes val thumbRes: Int,
)

data class StopGuideItem(
    val id: String,
    val routeId: String,
    val name: String,
    val address: String,
    val locationGuide: String,
    val landmarks: List<String>,
    val availableRoutes: List<String>,
    @param:DrawableRes val thumbRes: Int,
)

/** Offline placeholder when DB catalog is empty. */
fun emptyStopGuideRoutes(): List<StopGuideRouteInfo> =
    StudentRouteIds.orderedUiIds.map { uiId ->
        StopGuideRouteInfo(
            id = StudentRouteIds.guideUiId(uiId),
            title = StudentRouteIds.displayName(uiId),
            description = when (uiId) {
                StudentRouteIds.GIHEUNG -> "명지대학교와 기흥역을 연결하는 셔틀버스 노선입니다."
                StudentRouteIds.MYEONGJI_STATION -> "명지대학교와 명지대역을 연결하는 셔틀버스 노선입니다."
                else -> "학교와 용인 시내 주요 정류장을 연결하는 시내 셔틀버스 노선입니다."
            },
            thumbRes = StudentRouteIds.imageRes(uiId),
        )
    }

fun emptyStopGuideItems(): List<StopGuideItem> = emptyList()

@Deprecated("Use DB-backed stop guide", ReplaceWith("emptyStopGuideRoutes()"))
fun sampleStopGuideRoutes(): List<StopGuideRouteInfo> = emptyStopGuideRoutes()

@Deprecated("Use DB-backed stop guide", ReplaceWith("emptyStopGuideItems()"))
fun sampleStopGuideItems(): List<StopGuideItem> = emptyStopGuideItems()

fun List<StopGuideItem>.forRoute(routeId: String): List<StopGuideItem> =
    filter { it.routeId == routeId }
