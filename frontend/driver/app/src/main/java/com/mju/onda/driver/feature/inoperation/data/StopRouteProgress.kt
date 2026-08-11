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
    /** 해당 정류장 인근 */
    AtStop,
    /** 직전 정류장을 지나 다음으로 이동 중 */
    Between,
    /** 종점 도착 */
    Arrived,
}

data class StopRouteProgressState(
    val routeName: String,
    val vehicleName: String,
    val stops: List<RouteStop>,
    /** 현재 포커스 정류장 인덱스 (0-based) */
    val currentIndex: Int,
    val phase: StopProgressPhase,
    val hasGps: Boolean,
    val distanceToCurrentMeters: Int? = null,
)

object StopRouteProgress {
    /** 정류장 도착으로 인정하는 반경 */
    const val ARRIVE_RADIUS_METERS = 90.0

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
        )

    /**
     * GPS로 진행 인덱스를 갱신한다.
     * [maxReachedIndex] 는 운행 중 한 번 지나간 정류장을 뒤로 되돌리지 않기 위한 값.
     */
    fun resolve(
        routeName: String,
        vehicleName: String,
        stops: List<RouteStop>,
        lat: Double?,
        lng: Double?,
        maxReachedIndex: Int,
    ): Pair<StopRouteProgressState, Int> {
        if (stops.isEmpty()) {
            return initial(routeName, vehicleName, stops) to 0
        }
        if (lat == null || lng == null) {
            return StopRouteProgressState(
                routeName = routeName,
                vehicleName = vehicleName,
                stops = stops,
                currentIndex = maxReachedIndex.coerceIn(0, stops.lastIndex),
                phase = StopProgressPhase.Waiting,
                hasGps = false,
            ) to maxReachedIndex
        }

        val distances = stops.map { distanceMeters(lat, lng, it.lat, it.lng) }
        val nearest = distances.indices.minByOrNull { distances[it] } ?: 0
        val nearestDist = distances[nearest]

        var reached = maxReachedIndex.coerceIn(0, stops.lastIndex)

        // 반경 안이면 해당 정류장까지 진행으로 인정 (뒤로 가지 않음)
        if (nearestDist <= ARRIVE_RADIUS_METERS && nearest >= reached) {
            reached = nearest
        } else if (reached < stops.lastIndex) {
            // 다음 정류장 쪽으로 더 가까워지면 직전 정류장을 지난 것으로 보고 다음을 포커스
            val next = reached + 1
            val distCurrent = distances[reached]
            val distNext = distances[next]
            if (distNext + 25 < distCurrent && nearest >= reached) {
                reached = min(nearest, next).coerceAtLeast(reached)
            }
        }

        val phase = when {
            reached >= stops.lastIndex && nearestDist <= ARRIVE_RADIUS_METERS ->
                StopProgressPhase.Arrived
            nearestDist <= ARRIVE_RADIUS_METERS && nearest == reached ->
                StopProgressPhase.AtStop
            reached > 0 || nearestDist <= ARRIVE_RADIUS_METERS ->
                StopProgressPhase.Between
            else ->
                StopProgressPhase.Waiting
        }

        val focusIndex = when (phase) {
            StopProgressPhase.Between -> {
                // 이동 중이면 "지금 향하는" 다음 정류장을 크게 보여 줌
                min(reached + 1, stops.lastIndex)
            }
            else -> reached
        }

        val state = StopRouteProgressState(
            routeName = routeName,
            vehicleName = vehicleName,
            stops = stops,
            currentIndex = focusIndex,
            phase = phase,
            hasGps = true,
            distanceToCurrentMeters = distances[focusIndex].toInt(),
        )
        return state to reached
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
