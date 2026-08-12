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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val recordedAt: String? = null,
    val scheduledDepartureTime: String? = null,
    val actualStartedAt: String? = null,
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
    /** Per-vehicle GPS age label, e.g. "마지막 위치 수신 5초 전". */
    val lastLocationLabel: String = "",
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

/**
 * Stop timeline config per route.
 * DB stores one ordered loop per route; inbound list is optional (empty = single direction).
 */
data class RouteStopConfig(
    val routeId: String,
    val outboundFrom: String,
    val outboundTo: String,
    val inboundFrom: String,
    val inboundTo: String,
    val outboundStops: List<String>,
    val inboundStops: List<String>,
) {
    val directions: List<Pair<String, String>>
        get() = buildList {
            if (outboundStops.isNotEmpty()) add(outboundFrom to outboundTo)
            if (inboundStops.isNotEmpty()) add(inboundFrom to inboundTo)
        }

    fun stopNames(directionIndex: Int): List<String> =
        when {
            directionIndex <= 0 || inboundStops.isEmpty() -> outboundStops
            else -> inboundStops
        }
}

fun routeStopConfig(routeId: String): RouteStopConfig =
    com.onda.mju.student.data.route.RouteStopCatalog.config(routeId)

/**
 * Builds timeline nodes for a direction. Progress states are mock placeholders
 * until real stop-arrival tracking exists; scaled to the stop count of this direction.
 */
fun liveStopsForDirection(
    config: RouteStopConfig,
    directionIndex: Int,
): List<LiveStopNode> {
    val names = config.stopNames(directionIndex)
    if (names.isEmpty()) return emptyList()
    val dirKey = if (directionIndex == 0) "out" else "in"
    val last = names.lastIndex
    val currentIndex = when {
        names.size <= 2 -> 0
        else -> (names.size / 2).coerceAtMost(last - 1).coerceAtLeast(0)
    }
    return names.mapIndexed { index, name ->
        val state = when {
            index == last -> StopPassState.Destination
            index < currentIndex -> if (index == 0) StopPassState.Departed else StopPassState.Passed
            index == currentIndex -> StopPassState.Current
            else -> StopPassState.Upcoming
        }
        val statusText = when (state) {
            StopPassState.Departed -> "출발 완료"
            StopPassState.Passed -> "통과 완료"
            StopPassState.Current -> "현재 위치"
            StopPassState.Upcoming -> "도착 예정"
            StopPassState.Destination -> "도착 예정"
        }
        LiveStopNode(
            id = "${config.routeId}_${dirKey}_$index",
            name = name,
            state = state,
            statusText = statusText,
            subText = null,
            alertOn = state == StopPassState.Current,
        )
    }
}

/** Offline / loading skeleton — production overlays live vehicles from operations. */
fun sampleRouteLive(routeId: String = "city_shuttle"): RouteLiveData {
    val config = routeStopConfig(routeId)
    return RouteLiveData(
        routeId = routeId,
        routeName = com.onda.mju.student.data.route.StudentRouteIds.displayName(routeId),
        directions = config.directions,
        runningCount = 0,
        nextDeparture = "-",
        lastUpdateLabel = "마지막 갱신 —",
        locationOk = false,
        vehicles = emptyList(),
        stops = liveStopsForDirection(config, directionIndex = 0),
    )
}

/** Preview/fallback helper. Production UI uses [buildStopLiveData] with live vehicles. */
fun sampleStopLive(stopId: String = "s3"): StopLiveData {
    val resolved = resolveStopSelection(stopId, routeIdHint = null)
    return StopLiveData(
        stopId = resolved?.waypoint?.id ?: stopId,
        stopName = resolved?.waypoint?.name ?: stopId,
        lastUpdateLabel = "마지막 위치 수신 없음",
        arrivals = emptyList(),
    )
}

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
