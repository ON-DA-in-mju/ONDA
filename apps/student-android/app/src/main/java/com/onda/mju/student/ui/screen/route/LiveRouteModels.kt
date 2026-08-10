package com.onda.mju.student.ui.screen.route

import androidx.compose.ui.graphics.Color

enum class VehicleStatus(val label: String, val color: Color, val bg: Color) {
    Running("운행 중", Color(0xFF0F766E), Color(0xFFD1FAE5)),
    Approaching("정류장 접근", Color(0xFFEA580C), Color(0xFFFFEDD5)),
    Waiting("운행 대기", Color(0xFF6B7280), Color(0xFFF3F4F6)),
    Locating("위치 확인 중", Color(0xFF2563EB), Color(0xFFDBEAFE)),
}

enum class StopPassState {
    Departed,
    Passed,
    Current,
    Upcoming,
    Destination,
}

data class LiveVehicle(
    val id: String,
    val label: String,
    val status: VehicleStatus,
    val etaMinutes: Int? = null,
)

data class LiveStopNode(
    val id: String,
    val name: String,
    val state: StopPassState,
    val statusText: String,
    val subText: String? = null,
    val alertOn: Boolean = false,
)

data class RouteLiveData(
    val routeId: String,
    val routeName: String,
    val directions: List<Pair<String, String>>,
    val runningCount: Int,
    val nextDeparture: String,
    val lastUpdateLabel: String,
    val locationOk: Boolean,
    val vehicles: List<LiveVehicle>,
    val stops: List<LiveStopNode>,
)

data class StopArrival(
    val vehicleId: String,
    val vehicleLabel: String,
    val status: VehicleStatus,
    val etaLabel: String,
    val etaColor: Color,
)

data class StopLiveData(
    val stopId: String,
    val stopName: String,
    val lastUpdateLabel: String,
    val arrivals: List<StopArrival>,
)

data class BusDetailData(
    val vehicleId: String,
    val title: String,
    val status: VehicleStatus,
    val direction: String,
    val currentStatus: String,
    val nextStop: String,
    val etaLabel: String,
    val scheduledDeparture: String,
    val actualDeparture: String,
    val earlyNote: String,
    val lastLocationLabel: String,
    val gpsOk: Boolean,
    val remainingStops: Int,
    val reportFull: Int,
    val reportSeat: Int,
    val reportWait: Int,
    val reportAgo: String,
)

fun sampleRouteLive(routeId: String = "city_shuttle"): RouteLiveData {
    val name = when (routeId) {
        "giheung" -> "기흥역 통학버스"
        "myeongji_station" -> "명지대역 셔틀"
        else -> "시내 셔틀"
    }
    return RouteLiveData(
        routeId = routeId,
        routeName = name,
        directions = listOf(
            "버스관리사무소" to "중앙공영주차장",
            "중앙공영주차장" to "버스관리사무소",
        ),
        runningCount = 3,
        nextDeparture = "17:15",
        lastUpdateLabel = "마지막 갱신 10초 전",
        locationOk = true,
        vehicles = listOf(
            LiveVehicle("v1", "1호차", VehicleStatus.Running, 1),
            LiveVehicle("v2", "2호차", VehicleStatus.Approaching, 3),
            LiveVehicle("v3", "3호차", VehicleStatus.Waiting, null),
        ),
        stops = listOf(
            LiveStopNode("s1", "버스관리사무소", StopPassState.Departed, "출발 완료", "출발 16:45"),
            LiveStopNode("s2", "상공회의소", StopPassState.Passed, "통과 완료", "통과 추정 16:48"),
            LiveStopNode("s3", "진입로(럭스나인 앞)", StopPassState.Current, "현재 위치", alertOn = true),
            LiveStopNode("s4", "동부경찰서 중앙지구대", StopPassState.Upcoming, "약 3분 후 · 17:08 예상"),
            LiveStopNode("s5", "용인 CGV", StopPassState.Upcoming, "약 7분 후 · 17:12 예상"),
            LiveStopNode("s6", "중앙공영주차장", StopPassState.Destination, "약 12분 후 · 17:15 예상"),
        ),
    )
}

fun sampleStopLive(stopId: String = "s3"): StopLiveData = StopLiveData(
    stopId = stopId,
    stopName = "진입로(럭스나인 앞)",
    lastUpdateLabel = "마지막 위치 수신 1분 전",
    arrivals = listOf(
        StopArrival("v1", "1호차", VehicleStatus.Running, "도착 예정 1분", Color(0xFF16A34A)),
        StopArrival("v2", "2호차", VehicleStatus.Approaching, "도착 예정 3분", Color(0xFFEA580C)),
        StopArrival("v3", "3호차", VehicleStatus.Locating, "도착 예정 6분", Color(0xFF2563EB)),
    ),
)

fun sampleBusDetail(vehicleId: String = "v2"): BusDetailData = BusDetailData(
    vehicleId = vehicleId,
    title = when (vehicleId) {
        "v1" -> "시내 셔틀 1호차"
        "v3" -> "시내 셔틀 3호차"
        else -> "시내 셔틀 2호차"
    },
    status = VehicleStatus.Running,
    direction = "버스관리사무소 → 중앙공영주차장",
    currentStatus = "운행중",
    nextStop = "용인 CGV",
    etaLabel = "1분 후",
    scheduledDeparture = "17:15",
    actualDeparture = "17:09",
    earlyNote = "예정 시간보다 6분 일찍 출발",
    lastLocationLabel = "1분 전",
    gpsOk = true,
    remainingStops = 2,
    reportFull = 4,
    reportSeat = 1,
    reportWait = 2,
    reportAgo = "2분 전 기준",
)
