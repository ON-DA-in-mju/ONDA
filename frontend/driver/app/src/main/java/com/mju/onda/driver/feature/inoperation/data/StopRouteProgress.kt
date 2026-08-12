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
    /**
     * 완전히 “지나간” 정류장까지  inclusive 인덱스.
     * -1 이면 아직 출발 정류장도 이탈하지 않음.
     */
    val lastPassedIndex: Int = -1,
)

/** 운행 중 sticky GPS 진행 상태 (한 칸씩만 전진). */
data class StopProgressTracker(
    /** 진입 후 이탈까지 끝난 마지막 정류장 인덱스. 없으면 -1 */
    val lastPassedIndex: Int = -1,
    /** [lastPassedIndex]+1 번 정류장 반경(ARRIVE) 안에 들어온 적 있는지 */
    val hasEnteredFocus: Boolean = false,
)

object StopRouteProgress {
    /** 정류장 “도착/진입” 반경 — 학생앱과 동일 계열 */
    const val ARRIVE_RADIUS_METERS = 120.0

    /** 정류장 “이탈” 반경 — 이보다 멀어져야 지나감 확정 */
    const val EXIT_RADIUS_METERS = 140.0

    /** 다음 정류장 쪽으로 분명할 때 (enter 이후) */
    private const val TOWARD_NEXT_MARGIN_METERS = 40.0

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
        )

    /**
     * GPS로 진행 인덱스를 갱신한다.
     * - 한 번에 최대 1개 정류장만 전진
     * - 정류장은 ARRIVE 진입 후 EXIT 이탈(또는 다음 정류장으로 분명한 이동) 시에만 “지나감”
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
            val idx = (tracker.lastPassedIndex + 1).coerceIn(0, stops.lastIndex)
            return StopRouteProgressState(
                routeName = routeName,
                vehicleName = vehicleName,
                stops = stops,
                currentIndex = idx,
                phase = StopProgressPhase.Waiting,
                hasGps = false,
                lastPassedIndex = tracker.lastPassedIndex,
            ) to tracker
        }

        val last = stops.lastIndex
        var passed = tracker.lastPassedIndex.coerceIn(-1, last)
        var enteredFocus = tracker.hasEnteredFocus

        val focusIndex = min(passed + 1, last)
        val focus = stops[focusIndex]
        val distFocus = distanceMeters(lat, lng, focus.lat, focus.lng)

        if (distFocus <= ARRIVE_RADIUS_METERS) {
            enteredFocus = true
        }

        // 종점: 진입만으로 도착 확정 (이탈 불필요)
        if (focusIndex == last && enteredFocus && distFocus <= ARRIVE_RADIUS_METERS) {
            passed = last
            val state = StopRouteProgressState(
                routeName = routeName,
                vehicleName = vehicleName,
                stops = stops,
                currentIndex = last,
                phase = StopProgressPhase.Arrived,
                hasGps = true,
                distanceToCurrentMeters = distFocus.toInt(),
                lastPassedIndex = passed,
            )
            return state to StopProgressTracker(lastPassedIndex = passed, hasEnteredFocus = true)
        }

        // 포커스 정류장 이탈 → 한 칸만 지나감 확정
        if (focusIndex < last && enteredFocus) {
            val next = stops[focusIndex + 1]
            val distNext = distanceMeters(lat, lng, next.lat, next.lng)
            val leftFocus = distFocus > EXIT_RADIUS_METERS
            val clearlyTowardNext =
                distNext + TOWARD_NEXT_MARGIN_METERS < distFocus &&
                    distFocus > ARRIVE_RADIUS_METERS

            if (leftFocus || clearlyTowardNext) {
                passed = focusIndex
                enteredFocus = distNext <= ARRIVE_RADIUS_METERS
            }
        }

        // 절대 뒤로 가지 않음, 점프 금지 (이미 한 칸만 처리)
        passed = maxOf(passed, tracker.lastPassedIndex).coerceIn(-1, last)

        val phase: StopProgressPhase
        val currentIndex: Int
        val distCurrent: Double

        when {
            passed >= last -> {
                phase = StopProgressPhase.Arrived
                currentIndex = last
                distCurrent = distanceMeters(lat, lng, stops[last].lat, stops[last].lng)
            }
            passed < 0 && !enteredFocus -> {
                phase = StopProgressPhase.Waiting
                currentIndex = 0
                distCurrent = distanceMeters(lat, lng, stops[0].lat, stops[0].lng)
            }
            else -> {
                val uiFocus = min(passed + 1, last)
                val d = distanceMeters(lat, lng, stops[uiFocus].lat, stops[uiFocus].lng)
                if (d <= ARRIVE_RADIUS_METERS) {
                    phase = StopProgressPhase.AtStop
                    currentIndex = uiFocus
                    distCurrent = d
                } else if (passed >= 0 || enteredFocus) {
                    phase = StopProgressPhase.Between
                    currentIndex = uiFocus
                    distCurrent = d
                } else {
                    phase = StopProgressPhase.Waiting
                    currentIndex = 0
                    distCurrent = distanceMeters(lat, lng, stops[0].lat, stops[0].lng)
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
        )
        return state to StopProgressTracker(
            lastPassedIndex = passed,
            hasEnteredFocus = enteredFocus,
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

    /** UI: 이 인덱스까지는 지나간(체크) 표시 */
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
