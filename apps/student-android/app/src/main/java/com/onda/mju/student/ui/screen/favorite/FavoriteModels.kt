package com.onda.mju.student.ui.screen.favorite

import androidx.compose.ui.graphics.Color

enum class FavoriteTab {
    Route,
    Stop,
}

data class FavoriteRoute(
    val id: String,
    val name: String,
    val status: String,
    val nextDeparture: String,
    val operatingCount: String,
    val iconBg: Color,
)

data class FavoriteStop(
    val id: String,
    val name: String,
    val routes: List<RouteTag>,
    val iconBg: Color,
)

data class RouteTag(
    val label: String,
    val bg: Color,
    val fg: Color,
)

private val SoftBlue = Color(0xFFDBEAFE)
private val SoftTeal = Color(0xFFCCFBF1)
private val SoftPurple = Color(0xFFEDE9FE)
private val BlueFg = Color(0xFF1D4ED8)
private val TealFg = Color(0xFF0F766E)
private val PurpleFg = Color(0xFF6D28D9)

fun sampleFavoriteRoutes(): List<FavoriteRoute> = listOf(
    FavoriteRoute(
        id = "giheung",
        name = "기흥역 통학버스",
        status = "운행 중",
        nextDeparture = "17:15",
        operatingCount = "3대",
        iconBg = SoftBlue,
    ),
    FavoriteRoute(
        id = "myeongji_station",
        name = "명지대역 셔틀",
        status = "운행 중",
        nextDeparture = "16:50",
        operatingCount = "4대",
        iconBg = SoftTeal,
    ),
)

fun sampleFavoriteStops(): List<FavoriteStop> = listOf(
    FavoriteStop(
        id = "myeongji_station_intersection",
        name = "명지대역 사거리 정류장",
        iconBg = SoftBlue,
        routes = listOf(RouteTag("명지대역 셔틀", SoftTeal, TealFg)),
    ),
    FavoriteStop(
        id = "bus_management_office",
        name = "버스관리사무소",
        iconBg = SoftTeal,
        routes = listOf(
            RouteTag("기흥역 통학버스", SoftBlue, BlueFg),
            RouteTag("명지대역 셔틀", SoftTeal, TealFg),
            RouteTag("시내 셔틀", SoftPurple, PurpleFg),
        ),
    ),
    FavoriteStop(
        id = "giheung_exit_5",
        name = "기흥역 5번 출구",
        iconBg = SoftPurple,
        routes = listOf(RouteTag("기흥역 통학버스", SoftBlue, BlueFg)),
    ),
    FavoriteStop(
        id = "entrance_luxnine",
        name = "진입로(럭스나인 앞)",
        iconBg = SoftBlue,
        routes = listOf(
            RouteTag("명지대역 셔틀", SoftTeal, TealFg),
            RouteTag("시내 셔틀", SoftPurple, PurpleFg),
        ),
    ),
)
