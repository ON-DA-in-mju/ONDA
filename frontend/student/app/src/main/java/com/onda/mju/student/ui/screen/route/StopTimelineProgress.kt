package com.onda.mju.student.ui.screen.route

import com.onda.mju.student.data.remote.dto.OperationStopProgressDto
import com.onda.mju.student.data.route.RouteStopCatalog
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
 * 기사앱 StopRouteProgress 와 동일:
 * - ARRIVE → 도착 + 다음 포커스
 * - EXIT → 지나감 확정
 */
data class StopTimelineProgress(
    val lastPassedStopIndex: Int,
    val lastArrivedStopIndex: Int,
    /** Segment start index for in-between bus placement; -1 if bus sits on a stop node. */
    val busSegmentFromIndex: Int,
    /** 0f..1f along segment from → to; ignored when [busOnStopIndex] != null. */
    val busSegmentProgress: Float,
    /** When non-null, bus icon is drawn on that stop row. */
    val busOnStopIndex: Int?,
    val nodes: List<LiveStopNode>,
)

data class VehicleStopTracker(
    val lastPassedStopIndex: Int = -1,
    val lastArrivedStopIndex: Int = -1,
)

fun VehicleStopTracker.mergeAhead(other: VehicleStopTracker): VehicleStopTracker {
    val passed = maxOf(lastPassedStopIndex, other.lastPassedStopIndex)
    val arrived = maxOf(lastArrivedStopIndex, other.lastArrivedStopIndex, passed)
    return VehicleStopTracker(
        lastPassedStopIndex = passed,
        lastArrivedStopIndex = arrived,
    )
}

fun OperationStopProgressDto.toVehicleStopTracker(
    waypoints: List<StopWaypoint>,
): VehicleStopTracker {
    val last = waypoints.lastIndex
    if (last < 0) {
        return VehicleStopTracker(
            lastPassedStopIndex = lastPassedIndex,
            lastArrivedStopIndex = maxOf(lastArrivedIndex, lastPassedIndex),
        )
    }
    val passed = when {
        lastPassedIndex in 0..last -> lastPassedIndex
        !lastPassedStopId.isNullOrBlank() -> waypoints.indexOfFirst { it.id == lastPassedStopId }
        else -> -1
    }.coerceIn(-1, last)
    val arrived = when {
        lastArrivedIndex in 0..last -> lastArrivedIndex
        !lastArrivedStopId.isNullOrBlank() -> waypoints.indexOfFirst { it.id == lastArrivedStopId }
        else -> -1
    }.coerceIn(-1, last)
    return VehicleStopTracker(
        lastPassedStopIndex = passed,
        lastArrivedStopIndex = maxOf(arrived, passed),
    )
}

fun seedTrackersFromProgress(
    vehicles: List<LiveVehicle>,
    waypoints: List<StopWaypoint>,
    progressByOperation: Map<String, OperationStopProgressDto>,
    current: Map<String, VehicleStopTracker>,
): Map<String, VehicleStopTracker> {
    if (progressByOperation.isEmpty()) return current
    val next = current.toMutableMap()
    for (vehicle in vehicles) {
        val saved = progressByOperation[vehicle.id] ?: continue
        next[vehicle.id] = (next[vehicle.id] ?: VehicleStopTracker())
            .mergeAhead(saved.toVehicleStopTracker(waypoints))
    }
    return next
}

/** 기사앱과 동일 반경 */
const val STOP_ARRIVE_RADIUS_METERS = 60.0
const val STOP_EXIT_RADIUS_METERS = 80.0
private const val CHECK_AFTER_SEGMENT_PROGRESS = 0.40f

/**
 * 노선 waypoint — DB `route_stops`+`stops` lat/lng 우선.
 * catalog 비어 있을 때만 stops 테이블 좌표 맵으로 이름 매칭 (하드코딩 좌표 없음).
 */
fun stopWaypointsForRoute(
    routeId: String,
    directionIndex: Int = 0,
    stopCoordinates: StopCoordinateMap = emptyMap(),
): List<StopWaypoint> {
    val fromCatalog = RouteStopCatalog.waypoints(routeId)
    if (fromCatalog.isNotEmpty()) {
        // DB loop is a single direction; ignore inbound index.
        return fromCatalog
    }
    val config = routeStopConfig(routeId)
    return stopWaypointsForDirection(config, directionIndex, stopCoordinates)
}

fun stopWaypointsForDirection(
    config: RouteStopConfig,
    directionIndex: Int,
    stopCoordinates: StopCoordinateMap = emptyMap(),
): List<StopWaypoint> {
    val dirKey = if (directionIndex == 0) "out" else "in"
    return config.stopNames(directionIndex).mapIndexedNotNull { index, name ->
        val coords = StopCoordinateResolver.lookup(name, stopCoordinates) ?: return@mapIndexedNotNull null
        StopWaypoint(
            id = "${config.routeId}_${dirKey}_$index",
            name = name,
            latitude = coords.first,
            longitude = coords.second,
        )
    }
}

fun resolveStopTimelineProgress(
    waypoints: List<StopWaypoint>,
    latitude: Double?,
    longitude: Double?,
    tracker: VehicleStopTracker,
): StopTimelineProgress {
    if (waypoints.isEmpty()) {
        return StopTimelineProgress(
            lastPassedStopIndex = -1,
            lastArrivedStopIndex = -1,
            busSegmentFromIndex = -1,
            busSegmentProgress = 0f,
            busOnStopIndex = null,
            nodes = emptyList(),
        )
    }

    val last = waypoints.lastIndex
    if (latitude == null || longitude == null) {
        val focus = when {
            tracker.lastArrivedStopIndex >= 0 -> tracker.lastArrivedStopIndex
            else -> (tracker.lastPassedStopIndex + 1).coerceIn(0, last)
        }
        return buildProgress(
            waypoints = waypoints,
            passed = tracker.lastPassedStopIndex.coerceIn(-1, last),
            arrived = tracker.lastArrivedStopIndex.coerceIn(-1, last),
            busOnStop = focus,
            segmentFrom = -1,
            segmentProgress = 0f,
        )
    }

    var passed = tracker.lastPassedStopIndex.coerceIn(-1, last)
    var arrived = tracker.lastArrivedStopIndex.coerceIn(-1, last)
    if (arrived < passed) arrived = passed

    // 1) 지나감 확정 (EXIT) — 한 칸
    if (arrived > passed) {
        val exitIdx = passed + 1
        val distExit = distanceMeters(
            latitude, longitude,
            waypoints[exitIdx].latitude, waypoints[exitIdx].longitude,
        )
        if (distExit > STOP_EXIT_RADIUS_METERS) {
            passed = exitIdx
        }
    }

    // 2) 도착 인식 (ARRIVE) — 한 칸, 다음 포커스
    val focusIndex = min(arrived + 1, last)
    if (focusIndex > arrived) {
        val distFocus = distanceMeters(
            latitude, longitude,
            waypoints[focusIndex].latitude, waypoints[focusIndex].longitude,
        )
        if (distFocus <= STOP_ARRIVE_RADIUS_METERS) {
            arrived = focusIndex
            if (arrived >= last) {
                passed = last
            }
        }
    }

    passed = maxOf(passed, tracker.lastPassedStopIndex).coerceIn(-1, last)
    arrived = maxOf(arrived, tracker.lastArrivedStopIndex, passed).coerceIn(-1, last)

    return when {
        passed >= last || arrived >= last -> buildProgress(
            waypoints = waypoints,
            passed = last,
            arrived = last,
            busOnStop = last,
            segmentFrom = -1,
            segmentProgress = 1f,
        )
        arrived < 0 -> buildProgress(
            waypoints = waypoints,
            passed = -1,
            arrived = -1,
            busOnStop = 0,
            segmentFrom = -1,
            segmentProgress = 0f,
        )
        else -> {
            val dArrived = distanceMeters(
                latitude, longitude,
                waypoints[arrived].latitude, waypoints[arrived].longitude,
            )
            if (dArrived <= STOP_ARRIVE_RADIUS_METERS) {
                buildProgress(
                    waypoints = waypoints,
                    passed = passed,
                    arrived = arrived,
                    busOnStop = arrived,
                    segmentFrom = if (passed >= 0) passed else -1,
                    segmentProgress = if (passed >= 0) 1f else 0f,
                )
            } else {
                val nextIdx = min(arrived + 1, last)
                val fromIdx = arrived
                val from = waypoints[fromIdx]
                val to = waypoints[nextIdx]
                val proj = projectOnSegment(
                    latitude, longitude,
                    from.latitude, from.longitude,
                    to.latitude, to.longitude,
                )
                val t = proj.t.coerceIn(0.08f, 0.92f)
                buildProgress(
                    waypoints = waypoints,
                    passed = passed,
                    arrived = arrived,
                    busOnStop = null,
                    segmentFrom = fromIdx,
                    segmentProgress = t,
                )
            }
        }
    }
}

private fun isStopVisuallyCleared(
    index: Int,
    passed: Int,
    busOnStop: Int?,
    segmentFrom: Int,
    segmentProgress: Float,
): Boolean {
    if (index <= passed) return true
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
    arrived: Int,
    busOnStop: Int?,
    segmentFrom: Int,
    segmentProgress: Float,
): StopTimelineProgress {
    val last = waypoints.lastIndex
    val arrivedDestination = passed >= last || arrived >= last
    val nodes = waypoints.mapIndexed { index, wp ->
        val cleared = isStopVisuallyCleared(
            index = index,
            passed = passed,
            busOnStop = busOnStop,
            segmentFrom = segmentFrom,
            segmentProgress = segmentProgress,
        )
        val isCurrent = busOnStop == index ||
            (busOnStop == null && arrived == index && index > passed)

        val state = when {
            index == last && arrivedDestination -> StopPassState.Destination
            index == last -> StopPassState.Destination
            isCurrent -> StopPassState.Current
            cleared && index == 0 -> StopPassState.Departed
            cleared -> StopPassState.Passed
            else -> StopPassState.Upcoming
        }
        val statusText = when {
            index == last && arrivedDestination -> "도착 완료"
            state == StopPassState.Departed -> "출발 완료"
            state == StopPassState.Passed -> "통과 완료"
            state == StopPassState.Current -> "현재 위치"
            arrived == index && index > passed -> "현재 위치"
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
        lastArrivedStopIndex = arrived,
        busSegmentFromIndex = segmentFrom,
        busSegmentProgress = segmentProgress,
        busOnStopIndex = busOnStop,
        nodes = nodes,
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
