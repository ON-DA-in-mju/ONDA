package com.mju.onda.driver.feature.inoperation.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class StopProgressPhase {
    /** 아직 출발 전 / GPS 없음 → 첫 정류장 대기 */
    Waiting,
    /** 해당 정류장 인근 (진입 인식) */
    AtStop,
    /** 도착 후 다음으로 이동 중 / 지나감 확정 전 */
    Between,
    /** 종점 도착 */
    Arrived,
}

data class StopRouteProgressState(
    val routeName: String,
    val vehicleName: String,
    val stops: List<RouteStop>,
    /** UI 포커스 정류장 (보통 마지막 도착 정류장, 없으면 다음 대기) */
    val currentIndex: Int,
    val phase: StopProgressPhase,
    val hasGps: Boolean,
    val distanceToCurrentMeters: Int? = null,
    /**
     * 이탈까지 끝나 “지나감” 확정된 마지막 정류장 inclusive.
     * -1 이면 아직 아무 정류장도 지나감 확정 안 됨.
     */
    val lastPassedIndex: Int = -1,
    /**
     * ARRIVE 진입으로 “도착” 인식된 마지막 정류장 inclusive.
     * -1 이면 아직 첫 정류장 미도착.
     */
    val lastArrivedIndex: Int = -1,
)

/**
 * sticky GPS 진행 상태.
 * - 도착(ARRIVE): lastArrived 전진 → 다음 정류장이 포커스
 * - 지나감(EXIT): lastPassed 전진
 */
data class StopProgressTracker(
    val lastPassedIndex: Int = -1,
    val lastArrivedIndex: Int = -1,
)

object StopRouteProgress {
    /** 정류장 도착/진입 반경 — 들어오면 즉시 도착 인식 + 다음 포커스 */
    const val ARRIVE_RADIUS_METERS = 60.0

    /** 정류장 이탈 반경 — 이보다 멀어지면 지나감 확정 */
    const val EXIT_RADIUS_METERS = 80.0

    fun initial(
        routeName: String,
        vehicleName: String,
        stops: List<RouteStop>,
    ): StopRouteProgressState =
        StopRouteProgressState(
            routeName = routeName,
            vehicleName = vehicleName,
            stops = stops,
            currentIndex = 0,
            phase = StopProgressPhase.Waiting,
            hasGps = false,
            lastPassedIndex = -1,
            lastArrivedIndex = -1,
        )

    /**
     * GPS로 진행 인덱스를 갱신한다.
     * - ARRIVE 진입 → 해당 정류장 도착 + 포커스를 다음으로 (한 칸)
     * - EXIT 이탈 → 해당 정류장 지나감 확정 (한 칸)
     * - 종점은 진입만으로 도착·지나감 확정
     */
    fun resolve(
        routeName: String,
        vehicleName: String,
        stops: List<RouteStop>,
        lat: Double?,
        lng: Double?,
        tracker: StopProgressTracker,
    ): Pair<StopRouteProgressState, StopProgressTracker> {
        if (stops.isEmpty()) {
            return initial(routeName, vehicleName, stops) to StopProgressTracker()
        }
        if (lat == null || lng == null) {
            val idx = when {
                tracker.lastArrivedIndex >= 0 -> tracker.lastArrivedIndex
                else -> (tracker.lastPassedIndex + 1).coerceIn(0, stops.lastIndex)
            }
            return StopRouteProgressState(
                routeName = routeName,
                vehicleName = vehicleName,
                stops = stops,
                currentIndex = idx,
                phase = StopProgressPhase.Waiting,
                hasGps = false,
                lastPassedIndex = tracker.lastPassedIndex,
                lastArrivedIndex = tracker.lastArrivedIndex,
            ) to tracker
        }

        val last = stops.lastIndex
        var passed = tracker.lastPassedIndex.coerceIn(-1, last)
        var arrived = tracker.lastArrivedIndex.coerceIn(-1, last)
        if (arrived < passed) arrived = passed

        // 1) 지나감 확정: 도착은 했지만 아직 안 지나간 가장 앞 정류장 1개만 EXIT 체크
        if (arrived > passed) {
            val exitIdx = passed + 1
            val distExit = distanceMeters(lat, lng, stops[exitIdx].lat, stops[exitIdx].lng)
            if (distExit > EXIT_RADIUS_METERS) {
                passed = exitIdx
            }
        }

        // 2) 도착 인식: 다음 포커스 = arrived+1 에 ARRIVE 진입 시 한 칸
        val focusIndex = min(arrived + 1, last)
        if (focusIndex > arrived) {
            val distFocus = distanceMeters(lat, lng, stops[focusIndex].lat, stops[focusIndex].lng)
            if (distFocus <= ARRIVE_RADIUS_METERS) {
                arrived = focusIndex
                // 종점: 진입 = 도착 + 지나감 확정
                if (arrived >= last) {
                    passed = last
                }
            }
        }

        passed = maxOf(passed, tracker.lastPassedIndex).coerceIn(-1, last)
        arrived = maxOf(arrived, tracker.lastArrivedIndex, passed).coerceIn(-1, last)

        val phase: StopProgressPhase
        val currentIndex: Int
        val distCurrent: Double

        when {
            passed >= last || arrived >= last -> {
                phase = StopProgressPhase.Arrived
                currentIndex = last
                distCurrent = distanceMeters(lat, lng, stops[last].lat, stops[last].lng)
            }
            arrived < 0 -> {
                phase = StopProgressPhase.Waiting
                currentIndex = 0
                distCurrent = distanceMeters(lat, lng, stops[0].lat, stops[0].lng)
            }
            else -> {
                currentIndex = arrived
                distCurrent = distanceMeters(lat, lng, stops[arrived].lat, stops[arrived].lng)
                phase = if (distCurrent <= ARRIVE_RADIUS_METERS) {
                    StopProgressPhase.AtStop
                } else {
                    StopProgressPhase.Between
                }
            }
        }

        val state = StopRouteProgressState(
            routeName = routeName,
            vehicleName = vehicleName,
            stops = stops,
            currentIndex = currentIndex,
            phase = phase,
            hasGps = true,
            distanceToCurrentMeters = distCurrent.toInt(),
            lastPassedIndex = passed,
            lastArrivedIndex = arrived,
        )
        return state to StopProgressTracker(
            lastPassedIndex = passed,
            lastArrivedIndex = arrived,
        )
    }

    fun previousName(state: StopRouteProgressState): String? =
        state.stops.getOrNull(state.currentIndex - 1)?.name

    fun currentName(state: StopRouteProgressState): String? =
        state.stops.getOrNull(state.currentIndex)?.name

    fun nextName(state: StopRouteProgressState): String? =
        state.stops.getOrNull(state.currentIndex + 1)?.name

    fun phaseLabel(state: StopRouteProgressState): String = when {
        !state.hasGps -> "GPS 수신 대기 중"
        state.phase == StopProgressPhase.Waiting -> "출발 정류장으로 이동해 주세요"
        state.phase == StopProgressPhase.AtStop -> "현재 정류장"
        state.phase == StopProgressPhase.Between -> "다음 정류장으로 이동 중"
        state.phase == StopProgressPhase.Arrived -> "종점 도착"
        else -> "운행 중"
    }

    /** UI: 지나감 확정(체크) — EXIT 이후 */
    fun isStopPassed(state: StopRouteProgressState, index: Int): Boolean =
        index <= state.lastPassedIndex

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2.0)
        return 2 * earth * asin(sqrt(a))
    }
}
