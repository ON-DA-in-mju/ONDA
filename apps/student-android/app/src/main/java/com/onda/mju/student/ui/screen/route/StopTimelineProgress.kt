package com.onda.mju.student.ui.screen.route

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Stop with map coordinates used for GPS progress. */
data class StopWaypoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * GPS-derived timeline progress for one vehicle on one direction.
 * [lastPassedStopIndex] only moves forward (-1 = nothing passed yet).
 */
data class StopTimelineProgress(
    val lastPassedStopIndex: Int,
    /** Segment start index for in-between bus placement; -1 if bus sits on a stop node. */
    val busSegmentFromIndex: Int,
    /** 0f..1f along segment from → to; ignored when [busOnStopIndex] != null. */
    val busSegmentProgress: Float,
    /** When non-null, bus icon is drawn on that stop row. */
    val busOnStopIndex: Int?,
    val nodes: List<LiveStopNode>,
    /**
     * Focus 정류장( [lastPassedStopIndex]+1 ) ARRIVE 반경에 들어온 적 있는지.
     * 기사앱 [StopProgressTracker.hasEnteredFocus] 와 동일.
     */
    val hasEnteredFocus: Boolean,
)

/** Sticky tracker — 기사앱 StopProgressTracker 와 동일 조건. */
data class VehicleStopTracker(
    val lastPassedStopIndex: Int = -1,
    val hasEnteredFocus: Boolean = false,
)

/**
 * 기사앱 StopRouteProgress 와 동일 반경/전진 규칙.
 * - 한 번에 최대 1개 정류장만 전진
 * - ARRIVE 진입 후 EXIT 이탈(또는 다음 정류장으로 분명한 이동) 시에만 지나감
 */
const val STOP_ARRIVE_RADIUS_METERS = 120.0
const val STOP_EXIT_RADIUS_METERS = 140.0
private const val TOWARD_NEXT_MARGIN_METERS = 40.0
/** Floating bus must move this far along the next segment before the left stop shows a check. */
private const val CHECK_AFTER_SEGMENT_PROGRESS = 0.40f

/** Offline fallback coords when DB stops map is empty. */
internal val stopCoordinatesByName: Map<String, Pair<Double, Double>> = mapOf(
    "버스관리사무소" to (37.2245 to 127.1878),
    "채플관 앞" to (37.2245 to 127.1878),
    "기흥역 5번 출구" to (37.2754 to 127.1159),
    "상공회의소" to (37.2301 to 127.1889),
    "이마트" to (37.2304 to 127.1892),
    "진입로(럭스나인 앞)" to (37.2332 to 127.1894),
    "진입로(역북동 주민센터)" to (37.2335 to 127.1895),
    "경전철 명지대역" to (37.2381 to 127.1905),
    "명지대역 사거리 정류장" to (37.2400 to 127.1925),
    "동부경찰서 중앙지구대" to (37.2342 to 127.2005),
    "용인CGV" to (37.2348 to 127.2092),
    "중앙공영주차장" to (37.2340 to 127.2060),
    "명진당" to (37.2228 to 127.1875),
    "제1공학관" to (37.2220 to 127.1870),
    "제3공학관" to (37.2215 to 127.1868),
    "함박관" to (37.2210 to 127.1862),
    "창조관" to (37.2207 to 127.1858),
    "생활관(명현관)" to (37.22015 to 127.18515),
    "정문" to (37.22455 to 127.18875),
)

fun stopWaypointsForDirection(
    config: RouteStopConfig,
    directionIndex: Int,
    stopCoordinates: StopCoordinateMap = emptyMap(),
): List<StopWaypoint> {
    val dirKey = if (directionIndex == 0) "out" else "in"
    return config.stopNames(directionIndex).mapIndexed { index, name ->
        val coords = StopCoordinateResolver.lookup(name, stopCoordinates)
            ?: fallbackCoords(index, stopCoordinates)
        StopWaypoint(
            id = "${config.routeId}_${dirKey}_$index",
            name = name,
            latitude = coords.first,
            longitude = coords.second,
        )
    }
}

private fun fallbackCoords(index: Int, stopCoordinates: StopCoordinateMap): Pair<Double, Double> {
    val base = StopCoordinateResolver.lookup("버스관리사무소", stopCoordinates)
        ?: (37.2245 to 127.1878)
    return base.first + index * 0.0015 to base.second + index * 0.0010
}

/**
 * Builds forward-only progress from GPS — driver-aligned enter→exit, one stop at a time.
 */
fun resolveStopTimelineProgress(
    waypoints: List<StopWaypoint>,
    latitude: Double?,
    longitude: Double?,
    tracker: VehicleStopTracker,
): StopTimelineProgress {
    if (waypoints.isEmpty()) {
        return StopTimelineProgress(
            lastPassedStopIndex = -1,
            busSegmentFromIndex = -1,
            busSegmentProgress = 0f,
            busOnStopIndex = null,
            nodes = emptyList(),
            hasEnteredFocus = false,
        )
    }

    val last = waypoints.lastIndex
    if (latitude == null || longitude == null) {
        val focus = min(tracker.lastPassedStopIndex + 1, last).coerceAtLeast(0)
        return buildProgress(
            waypoints = waypoints,
            passed = tracker.lastPassedStopIndex.coerceIn(-1, last),
            busOnStop = focus,
            segmentFrom = -1,
            segmentProgress = 0f,
            hasEnteredFocus = tracker.hasEnteredFocus,
        )
    }

    var passed = tracker.lastPassedStopIndex.coerceIn(-1, last)
    var enteredFocus = tracker.hasEnteredFocus

    val focusIndex = min(passed + 1, last)
    val focus = waypoints[focusIndex]
    val distFocus = distanceMeters(latitude, longitude, focus.latitude, focus.longitude)

    if (distFocus <= STOP_ARRIVE_RADIUS_METERS) {
        enteredFocus = true
    }

    // 종점: 진입만으로 도착 확정 (이탈 불필요)
    if (focusIndex == last && enteredFocus && distFocus <= STOP_ARRIVE_RADIUS_METERS) {
        passed = last
        return buildProgress(
            waypoints = waypoints,
            passed = last,
            busOnStop = last,
            segmentFrom = -1,
            segmentProgress = 1f,
            hasEnteredFocus = true,
        )
    }

    // 포커스 정류장 이탈 → 한 칸만 지나감 (점프 금지)
    if (focusIndex < last && enteredFocus) {
        val next = waypoints[focusIndex + 1]
        val distNext = distanceMeters(latitude, longitude, next.latitude, next.longitude)
        val leftFocus = distFocus > STOP_EXIT_RADIUS_METERS
        val clearlyTowardNext =
            distNext + TOWARD_NEXT_MARGIN_METERS < distFocus &&
                distFocus > STOP_ARRIVE_RADIUS_METERS

        if (leftFocus || clearlyTowardNext) {
            passed = focusIndex
            enteredFocus = distNext <= STOP_ARRIVE_RADIUS_METERS
        }
    }

    passed = maxOf(passed, tracker.lastPassedStopIndex).coerceIn(-1, last)

    return when {
        passed >= last -> buildProgress(
            waypoints = waypoints,
            passed = last,
            busOnStop = last,
            segmentFrom = -1,
            segmentProgress = 1f,
            hasEnteredFocus = enteredFocus,
        )
        passed < 0 && !enteredFocus -> buildProgress(
            waypoints = waypoints,
            passed = -1,
            busOnStop = 0,
            segmentFrom = -1,
            segmentProgress = 0f,
            hasEnteredFocus = false,
        )
        else -> {
            val uiFocus = min(passed + 1, last)
            val focusStop = waypoints[uiFocus]
            val d = distanceMeters(
                latitude, longitude, focusStop.latitude, focusStop.longitude,
            )
            if (d <= STOP_ARRIVE_RADIUS_METERS) {
                buildProgress(
                    waypoints = waypoints,
                    passed = passed,
                    busOnStop = uiFocus,
                    segmentFrom = if (passed >= 0) passed else -1,
                    segmentProgress = if (passed >= 0) 1f else 0f,
                    hasEnteredFocus = true,
                )
            } else if (passed >= 0 || enteredFocus) {
                val fromIdx = if (passed >= 0) passed else 0
                val from = waypoints[fromIdx]
                val to = waypoints[uiFocus]
                val proj = projectOnSegment(
                    latitude, longitude,
                    from.latitude, from.longitude,
                    to.latitude, to.longitude,
                )
                val t = proj.t.coerceIn(0.08f, 0.92f)
                buildProgress(
                    waypoints = waypoints,
                    passed = passed,
                    busOnStop = null,
                    segmentFrom = fromIdx,
                    segmentProgress = t,
                    hasEnteredFocus = enteredFocus,
                )
            } else {
                buildProgress(
                    waypoints = waypoints,
                    passed = -1,
                    busOnStop = 0,
                    segmentFrom = -1,
                    segmentProgress = 0f,
                    hasEnteredFocus = false,
                )
            }
        }
    }
}

/**
 * Check / "통과 완료" only after the bus has visually left that stop.
 */
private fun isStopVisuallyCleared(
    index: Int,
    passed: Int,
    busOnStop: Int?,
    segmentFrom: Int,
    segmentProgress: Float,
): Boolean {
    if (busOnStop != null) {
        return index < busOnStop && index <= passed
    }
    if (segmentFrom < 0) return false
    if (index < segmentFrom) return index <= passed
    if (index == segmentFrom) {
        return index <= passed && segmentProgress >= CHECK_AFTER_SEGMENT_PROGRESS
    }
    return false
}

private fun buildProgress(
    waypoints: List<StopWaypoint>,
    passed: Int,
    busOnStop: Int?,
    segmentFrom: Int,
    segmentProgress: Float,
    hasEnteredFocus: Boolean,
): StopTimelineProgress {
    val last = waypoints.lastIndex
    val arrivedDestination = passed >= last
    val nodes = waypoints.mapIndexed { index, wp ->
        val cleared = isStopVisuallyCleared(
            index = index,
            passed = passed,
            busOnStop = busOnStop,
            segmentFrom = segmentFrom,
            segmentProgress = segmentProgress,
        )
        val justLeftNotYetChecked = !cleared &&
            busOnStop == null &&
            segmentFrom == index &&
            index <= passed

        val state = when {
            index == last && arrivedDestination -> StopPassState.Destination
            index == last -> StopPassState.Destination
            busOnStop == index -> StopPassState.Current
            cleared && index == 0 -> StopPassState.Departed
            cleared -> StopPassState.Passed
            else -> StopPassState.Upcoming
        }
        val statusText = when {
            index == last && arrivedDestination -> "도착 완료"
            state == StopPassState.Departed -> "출발 완료"
            state == StopPassState.Passed -> "통과 완료"
            state == StopPassState.Current -> "현재 위치"
            justLeftNotYetChecked -> "이동 중"
            state == StopPassState.Destination -> "도착 예정"
            else -> "도착 예정"
        }
        LiveStopNode(
            id = wp.id,
            name = wp.name,
            state = state,
            statusText = statusText,
            subText = null,
            alertOn = false,
        )
    }
    return StopTimelineProgress(
        lastPassedStopIndex = passed,
        busSegmentFromIndex = segmentFrom,
        busSegmentProgress = segmentProgress,
        busOnStopIndex = busOnStop,
        nodes = nodes,
        hasEnteredFocus = hasEnteredFocus,
    )
}

private data class SegmentProjection(
    val t: Float,
    val distanceMeters: Double,
)

private fun projectOnSegment(
    lat: Double,
    lng: Double,
    aLat: Double,
    aLng: Double,
    bLat: Double,
    bLng: Double,
): SegmentProjection {
    val midLat = (aLat + bLat) / 2.0
    val metersPerDegLat = 111_320.0
    val metersPerDegLng = 111_320.0 * cos(Math.toRadians(midLat))
    val bx = (bLng - aLng) * metersPerDegLng
    val by = (bLat - aLat) * metersPerDegLat
    val px = (lng - aLng) * metersPerDegLng
    val py = (lat - aLat) * metersPerDegLat
    val ab2 = bx * bx + by * by
    val t = if (ab2 <= 1e-6) 0.0 else ((px * bx + py * by) / ab2).coerceIn(0.0, 1.0)
    val cx = t * bx
    val cy = t * by
    val dx = px - cx
    val dy = py - cy
    return SegmentProjection(t = t.toFloat(), distanceMeters = sqrt(dx * dx + dy * dy))
}

fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
