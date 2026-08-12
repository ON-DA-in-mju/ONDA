package com.onda.mju.student.ui.screen.route

import kotlin.math.atan2
import kotlin.math.cos
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
 * [lastPassedStopIndex] only moves forward (-1 = nothing passed yet / still at origin).
 */
data class StopTimelineProgress(
    val lastPassedStopIndex: Int,
    /** Segment start index for in-between bus placement; -1 if bus sits on a stop node. */
    val busSegmentFromIndex: Int,
    /** 0f..1f along segment from → to; ignored when [busOnStopIndex] != null. */
    val busSegmentProgress: Float,
    /** When non-null, bus icon is drawn on that stop row (start / at stop / arrived). */
    val busOnStopIndex: Int?,
    val nodes: List<LiveStopNode>,
    /** Whether GPS has entered the start-stop radius at least once (monotonic). */
    val hasEnteredStart: Boolean,
)

/** Mutable tracker kept per vehicle + direction. */
data class VehicleStopTracker(
    val lastPassedStopIndex: Int = -1,
    val hasEnteredStart: Boolean = false,
)

/**
 * Arrive / exit radii for stop passage.
 * Hardcoded campus stop coords can be tens of meters off real GPS, so arrive is generous;
 * exit requires leaving that zone after having entered (enter-then-exit).
 */
const val STOP_ARRIVE_RADIUS_METERS = 120.0
const val STOP_EXIT_RADIUS_METERS = 140.0
private const val SEGMENT_SNAP_METERS = 150.0
/** Floating bus must move this far along the next segment before the left stop shows a check. */
private const val CHECK_AFTER_SEGMENT_PROGRESS = 0.40f

/** Approximate campus / route stop coordinates (local until Supabase stops are wired). */
internal val stopCoordinatesByName: Map<String, Pair<Double, Double>> = mapOf(
    "버스관리사무소" to (37.2245 to 127.1878),
    "기흥역 5번 출구" to (37.2754 to 127.1159),
    "상공회의소" to (37.2301 to 127.1889),
    "이마트" to (37.2304 to 127.1892),
    "진입로(럭스나인 앞)" to (37.2332 to 127.1894),
    "진입로(역북동 주민센터)" to (37.2335 to 127.1895),
    "경전철 명지대역" to (37.2381 to 127.1905),
    // Far enough from 경전철(~250m+) so enter/exit radii can distinguish the terminus.
    "명지대역 사거리 정류장" to (37.2400 to 127.1925),
    "동부경찰서 중앙지구대" to (37.2342 to 127.2005),
    "용인CGV" to (37.2348 to 127.2092),
    "중앙공영주차장" to (37.2340 to 127.2060),
    "명진당" to (37.2228 to 127.1875),
    "제1공학관" to (37.2220 to 127.1870),
    "제3공학관" to (37.2215 to 127.1868),
    "함박관" to (37.2210 to 127.1862),
    "창조관" to (37.2207 to 127.1858),
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
 * Builds forward-only progress from GPS.
 *
 * Start stop is NOT marked departed just because GPS is far from the hardcoded origin.
 * Departure requires enter-then-exit (or clear motion toward the next stop after enter).
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
            hasEnteredStart = tracker.hasEnteredStart,
        )
    }

    val last = waypoints.lastIndex
    if (latitude == null || longitude == null) {
        return buildProgress(
            waypoints = waypoints,
            passed = -1,
            busOnStop = 0,
            segmentFrom = -1,
            segmentProgress = 0f,
            hasEnteredStart = tracker.hasEnteredStart,
        )
    }

    var passed = tracker.lastPassedStopIndex.coerceIn(-1, last)
    var enteredStart = tracker.hasEnteredStart
    val start = waypoints[0]
    val d0 = distanceMeters(latitude, longitude, start.latitude, start.longitude)

    if (d0 <= STOP_ARRIVE_RADIUS_METERS) {
        enteredStart = true
    }

    // Origin: stay CURRENT until enter-then-exit (or clear progress to next after enter).
    if (passed < 0) {
        val leftStart = enteredStart && d0 > STOP_EXIT_RADIUS_METERS
        var clearlyTowardNext = false
        if (enteredStart && last >= 1) {
            val next = waypoints[1]
            val d1 = distanceMeters(latitude, longitude, next.latitude, next.longitude)
            val proj = projectOnSegment(
                latitude, longitude,
                start.latitude, start.longitude,
                next.latitude, next.longitude,
            )
            clearlyTowardNext = (d1 + 30.0 < d0 && d0 > STOP_ARRIVE_RADIUS_METERS) ||
                (proj.distanceMeters <= SEGMENT_SNAP_METERS && proj.t >= 0.25 && d0 > STOP_ARRIVE_RADIUS_METERS)
        }
        if (leftStart || clearlyTowardNext) {
            passed = 0
        } else {
            // Still at (or treated as) origin — bus icon on first stop.
            return buildProgress(
                waypoints = waypoints,
                passed = -1,
                busOnStop = 0,
                segmentFrom = -1,
                segmentProgress = 0f,
                hasEnteredStart = enteredStart,
            )
        }
    }

    val dest = waypoints[last]
    val distDest = distanceMeters(latitude, longitude, dest.latitude, dest.longitude)

    // Clear a stop only after leaving its exit radius toward the following stop.
    // Penultimate↔terminus can be closer than EXIT radius — special-cased below.
    var guard = 0
    while (passed < last && guard < waypoints.size + 2) {
        guard++
        val candidate = passed + 1
        val stop = waypoints[candidate]
        val distStop = distanceMeters(latitude, longitude, stop.latitude, stop.longitude)

        if (candidate == last) {
            if (distStop <= STOP_ARRIVE_RADIUS_METERS) {
                passed = last
            }
            break
        }

        val following = waypoints[candidate + 1]
        val followingIsDest = candidate + 1 == last
        val distFollowing = distanceMeters(
            latitude, longitude, following.latitude, following.longitude,
        )
        val proj = projectOnSegment(
            latitude, longitude,
            stop.latitude, stop.longitude,
            following.latitude, following.longitude,
        )

        // Final approach: if GPS is at/near terminus, clear penultimate even when still
        // inside its exit radius (common when last two stops are < EXIT apart).
        if (followingIsDest && distDest <= STOP_ARRIVE_RADIUS_METERS) {
            passed = candidate
            continue
        }
        if (followingIsDest && distDest + 30.0 < distStop && distDest <= STOP_EXIT_RADIUS_METERS) {
            passed = candidate
            continue
        }

        // Still at / approaching this stop — do not clear it yet.
        if (distStop <= STOP_EXIT_RADIUS_METERS) {
            break
        }

        val clearlyPastTowardNext =
            (proj.distanceMeters <= SEGMENT_SNAP_METERS && proj.t >= 0.25f) ||
                (distFollowing + 50.0 < distStop)

        if (clearlyPastTowardNext) {
            passed = candidate
            continue
        }
        break
    }

    // Hard snap onto terminus once nearby and route progress is near the end.
    if (passed < last && distDest <= STOP_ARRIVE_RADIUS_METERS && passed >= (last - 2).coerceAtLeast(0)) {
        passed = last
    }

    passed = maxOf(passed, tracker.lastPassedStopIndex.coerceAtLeast(-1)).coerceIn(-1, last)

    return when {
        passed < 0 -> buildProgress(
            waypoints = waypoints,
            passed = -1,
            busOnStop = 0,
            segmentFrom = -1,
            segmentProgress = 0f,
            hasEnteredStart = enteredStart,
        )
        passed >= last -> buildProgress(
            waypoints = waypoints,
            passed = last,
            busOnStop = last,
            segmentFrom = -1,
            segmentProgress = 1f,
            hasEnteredStart = enteredStart,
        )
        else -> {
            val nextIdx = passed + 1
            val next = waypoints[nextIdx]
            val distNext = distanceMeters(latitude, longitude, next.latitude, next.longitude)
            val approachingDest = nextIdx == last
            if (distNext <= STOP_ARRIVE_RADIUS_METERS ||
                (approachingDest && distDest <= STOP_ARRIVE_RADIUS_METERS)
            ) {
                val atDest = nextIdx == last
                buildProgress(
                    waypoints = waypoints,
                    passed = if (atDest) last else passed,
                    busOnStop = nextIdx,
                    segmentFrom = passed,
                    segmentProgress = 1f,
                    hasEnteredStart = enteredStart,
                )
            } else {
                val from = waypoints[passed]
                val proj = projectOnSegment(
                    latitude, longitude,
                    from.latitude, from.longitude,
                    next.latitude, next.longitude,
                )
                val t = proj.t.coerceIn(0f, 1f)
                // On the final segment, let the bus reach the terminus visually (no 0.92 cap).
                val cappedT = if (approachingDest) {
                    t.coerceAtLeast(0.08f).coerceAtMost(1f)
                } else {
                    t.coerceAtLeast(0.08f).coerceAtMost(0.92f)
                }
                // Near end of final segment → sit on destination flag.
                if (approachingDest && cappedT >= 0.90f &&
                    proj.distanceMeters <= SEGMENT_SNAP_METERS
                ) {
                    buildProgress(
                        waypoints = waypoints,
                        passed = last,
                        busOnStop = last,
                        segmentFrom = -1,
                        segmentProgress = 1f,
                        hasEnteredStart = enteredStart,
                    )
                } else {
                    buildProgress(
                        waypoints = waypoints,
                        passed = passed,
                        busOnStop = null,
                        segmentFrom = passed,
                        segmentProgress = cappedT,
                        hasEnteredStart = enteredStart,
                    )
                }
            }
        }
    }
}

/**
 * Check / "통과 완료" only after the bus has visually left that stop:
 * - stops strictly before the bus-on-stop index
 * - or the segment-from stop once floating progress clears [CHECK_AFTER_SEGMENT_PROGRESS]
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
    hasEnteredStart: Boolean,
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
        hasEnteredStart = hasEnteredStart,
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
