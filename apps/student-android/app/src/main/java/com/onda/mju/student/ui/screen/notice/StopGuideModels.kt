package com.onda.mju.student.ui.screen.notice

import androidx.annotation.DrawableRes
import com.onda.mju.student.R

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

fun sampleStopGuideRoutes(): List<StopGuideRouteInfo> = listOf(
    StopGuideRouteInfo(
        id = "giheung",
        title = "기흥역 통학버스",
        description = "명지대학교와 기흥역을 연결하는 셔틀버스 노선입니다.",
        thumbRes = R.drawable.route_thumb_giheung,
    ),
    StopGuideRouteInfo(
        id = "myeongji",
        title = "명지대역 셔틀",
        description = "명지대학교와 명지대역을 연결하는 셔틀버스 노선입니다.",
        thumbRes = R.drawable.route_thumb_myeongji,
    ),
    StopGuideRouteInfo(
        id = "city",
        title = "시내 셔틀",
        description = "학교와 용인 시내 주요 정류장을 연결하는 시내 셔틀버스 노선입니다.",
        thumbRes = R.drawable.route_thumb_city,
    ),
)

fun sampleStopGuideItems(): List<StopGuideItem> = listOf(
    StopGuideItem(
        id = "bus_office",
        routeId = "city",
        name = "버스관리사무소",
        address = "명지대학교 버스관리사무소 앞",
        locationGuide = "명지대학교 버스관리사무소 정문 앞에서 탑승할 수 있습니다.",
        landmarks = listOf("버스관리사무소", "정문", "도로변 버스정류장"),
        availableRoutes = listOf("시내 셔틀"),
        thumbRes = R.drawable.route_thumb_city,
    ),
    StopGuideItem(
        id = "chamber",
        routeId = "city",
        name = "상공회의소",
        address = "용인상공회의소 앞",
        locationGuide = "용인상공회의소 건물 앞 도로변 정류장에서 대기해 주세요.",
        landmarks = listOf("상공회의소", "횡단보도", "도로변 버스정류장"),
        availableRoutes = listOf("시내 셔틀"),
        thumbRes = R.drawable.route_thumb_city,
    ),
    StopGuideItem(
        id = "luxnine",
        routeId = "city",
        name = "진입로(럭스나인 앞)",
        address = "럭스나인 건물 앞",
        locationGuide = "럭스나인 건물 앞 진입로 정류장에서 탑승할 수 있습니다.",
        landmarks = listOf("럭스나인", "진입로", "도로변 버스정류장"),
        availableRoutes = listOf("시내 셔틀"),
        thumbRes = R.drawable.route_thumb_city,
    ),
    StopGuideItem(
        id = "myeongji_stn",
        routeId = "city",
        name = "경전철 명지대역",
        address = "경전철 명지대역 1번 출구 앞",
        locationGuide = "경전철 명지대역 1번 출구로 나와 정류장 표지판을 따라가면 됩니다.",
        landmarks = listOf("명지대역", "1번 출구", "횡단보도"),
        availableRoutes = listOf("시내 셔틀", "명지대역 셔틀"),
        thumbRes = R.drawable.route_thumb_myeongji,
    ),
    StopGuideItem(
        id = "myeongji_cross",
        routeId = "myeongji",
        name = "명지대역 사거리 정류장",
        address = "명지대역 사거리 인근",
        locationGuide = "명지대역 1번 출구로 나와 횡단보도를 건넌 뒤, 사거리 인근 " +
            "‘47352 역북6동 방면’ 정류장 표지판이 보이는 곳에서 대기하면 됩니다.",
        landmarks = listOf("명지대역", "횡단보도", "도로변 버스정류장"),
        availableRoutes = listOf("명지대역 셔틀"),
        thumbRes = R.drawable.route_thumb_myeongji,
    ),
    StopGuideItem(
        id = "giheung_exit5",
        routeId = "giheung",
        name = "기흥역 5번 출구",
        address = "기흥역 5번 출구 앞",
        locationGuide = "기흥역 5번 출구로 나와 바로 앞 통학버스 대기 장소에서 탑승해 주세요.",
        landmarks = listOf("기흥역", "5번 출구", "횡단보도"),
        availableRoutes = listOf("기흥역 통학버스"),
        thumbRes = R.drawable.route_thumb_giheung,
    ),
    StopGuideItem(
        id = "giheung_campus",
        routeId = "giheung",
        name = "버스관리사무소",
        address = "명지대학교 버스관리사무소 앞",
        locationGuide = "명지대학교 버스관리사무소 앞에서 기흥역 방면 버스를 이용할 수 있습니다.",
        landmarks = listOf("버스관리사무소", "정문"),
        availableRoutes = listOf("기흥역 통학버스", "시내 셔틀"),
        thumbRes = R.drawable.route_thumb_giheung,
    ),
    StopGuideItem(
        id = "myeongji_campus",
        routeId = "myeongji",
        name = "명진당",
        address = "명지대학교 명진당 앞",
        locationGuide = "명진당 앞 정류장에서 명지대역 셔틀을 이용할 수 있습니다.",
        landmarks = listOf("명진당", "중앙도서관"),
        availableRoutes = listOf("명지대역 셔틀"),
        thumbRes = R.drawable.route_thumb_myeongji,
    ),
)

fun List<StopGuideItem>.forRoute(routeId: String): List<StopGuideItem> =
    filter { it.routeId == routeId }
