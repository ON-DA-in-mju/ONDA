package com.onda.mju.student.ui.screen.route

import androidx.compose.ui.graphics.Color
import kotlin.math.ceil

/** Selected stop resolved from navigation stopId (+ optional routeId). */
data class ResolvedStopSelection(
    val routeId: String,
    val directionIndex: Int,
    val waypoint: StopWaypoint,
    val waypoints: List<StopWaypoint>,
)

private val EtaGreen = Color(0xFF16A34A)
private val EtaOrange = Color(0xFFEA580C)
private val EtaBlue = Color(0xFF2563EB)
private val EtaGray = Color(0xFF6B7280)

/** Fallback cruise speed when GPS speed is missing/too low (~25 km/h). */
private const val DEFAULT_SPEED_MPS = 6.94

/**
 * Approach radius for "정류장 접근" — reuse stop arrive radius from timeline GPS logic.
 */
val STOP_APPROACH_RADIUS_METERS: Double = STOP_ARRIVE_RADIUS_METERS

private val favoriteOrGuideStopNames: Map<String, String> = mapOf(
    "myeongji_station_intersection" to "명지대역 사거리 정류장",
    "bus_management_office" to "버스관리사무소",
    "bus_office" to "버스관리사무소",
    "giheung_exit_5" to "기흥역 5번 출구",
    "chamber" to "상공회의소",
    "luxnine" to "진입로(럭스나인 앞)",
    "myeongji_stn" to "경전철 명지대역",
    "s3" to "진입로(럭스나인 앞)",
)

fun resolveStopSelection(
    stopId: String,
    routeIdHint: String?,
    stopCoordinates: StopCoordinateMap = emptyMap(),
): ResolvedStopSelection? {
    val routeCandidates = buildList {
        routeIdHint?.takeIf { it.isNotBlank() }?.let {
            add(com.onda.mju.student.data.route.StudentRouteIds.normalizeUiId(it))
        }
        addAll(com.onda.mju.student.data.route.StudentRouteIds.routeListUiIds)
    }.distinct()

    for (routeId in routeCandidates) {
        val waypoints = stopWaypointsForRoute(routeId, directionIndex = 0, stopCoordinates)
        if (waypoints.isEmpty()) continue
        val byId = waypoints.firstOrNull { it.id == stopId }
        if (byId != null) {
            return ResolvedStopSelection(routeId, 0, byId, waypoints)
        }
    }

    val stopName = favoriteOrGuideStopNames[stopId] ?: stopId
    for (routeId in routeCandidates) {
        val waypoints = stopWaypointsForRoute(routeId, directionIndex = 0, stopCoordinates)
        if (waypoints.isEmpty()) continue
        val infos = com.onda.mju.student.data.route.RouteStopCatalog.stopInfos(routeId)
        val matchedName = infos.firstOrNull { it.id == stopId }?.name ?: stopName
        val byName = waypoints.firstOrNull {
            it.name == matchedName || it.name == stopName
        }
        if (byName != null) {
            return ResolvedStopSelection(routeId, 0, byName, waypoints)
        }
    }
    return null
}

/**
 * Builds per-vehicle arrivals for the stop live / “노선 시간표” stop sheet.
 * Uses each vehicle's own lat/lng/recorded_at and route-direction remaining distance.
 */
fun buildStopLiveData(
    stopId: String,
    routeIdHint: String?,
    vehicles: List<LiveVehicle>,
    nowMillis: Long,
    trackerByVehicle: Map<String, VehicleStopTracker> = emptyMap(),
    stopCoordinates: StopCoordinateMap = emptyMap(),
): Pair<StopLiveData, Map<String, VehicleStopTracker>> {
    val resolved = resolveStopSelection(stopId, routeIdHint, stopCoordinates)
    val stopName = resolved?.waypoint?.name
        ?: favoriteOrGuideStopNames[stopId]
        ?: stopId

    if (resolved == null) {
        val arrivals = vehicles.map { vehicle ->
            locatingArrival(vehicle, nowMillis)
        }
        val headerAge = vehicles.mapNotNull { timestampAgeSeconds(it.recordedAt, nowMillis) }.minOrNull()
        return StopLiveData(
            stopId = stopId,
            stopName = stopName,
            lastUpdateLabel = formatLastLocationReceivedLabel(headerAge),
            arrivals = arrivals,
        ) to trackerByVehicle
    }

    val targetIndex = resolved.waypoints.indexOfFirst { it.id == resolved.waypoint.id }
        .takeIf { it >= 0 }
        ?: resolved.waypoints.indexOfFirst { it.name == resolved.waypoint.name }

    val nextTrackers = trackerByVehicle.toMutableMap()
    val arrivals = vehicles.map { vehicle ->
        val tracker = nextTrackers[vehicle.id] ?: VehicleStopTracker()
        val arrival = buildVehicleArrival(
            vehicle = vehicle,
            waypoints = resolved.waypoints,
            targetIndex = targetIndex,
            tracker = tracker,
            nowMillis = nowMillis,
        )
        nextTrackers[vehicle.id] = arrival.second
        arrival.first
    }

    val headerAge = vehicles.mapNotNull { timestampAgeSeconds(it.recordedAt, nowMillis) }.minOrNull()
    return StopLiveData(
        stopId = resolved.waypoint.id,
        stopName = resolved.waypoint.name,
        lastUpdateLabel = formatLastLocationReceivedLabel(headerAge),
        arrivals = arrivals,
    ) to nextTrackers
}

private fun locatingArrival(vehicle: LiveVehicle, nowMillis: Long): StopArrival {
    val age = timestampAgeSeconds(vehicle.recordedAt, nowMillis)
    return StopArrival(
        vehicleId = vehicle.id,
        vehicleLabel = vehicle.label,
        status = VehicleStatus.Locating,
        etaLabel = "위치 확인 중",
        etaColor = EtaBlue,
        lastLocationLabel = formatLastLocationReceivedLabel(age),
    )
}

private fun buildVehicleArrival(
    vehicle: LiveVehicle,
    waypoints: List<StopWaypoint>,
    targetIndex: Int,
    tracker: VehicleStopTracker,
    nowMillis: Long,
): Pair<StopArrival, VehicleStopTracker> {
    val age = timestampAgeSeconds(vehicle.recordedAt, nowMillis)
    val lat = vehicle.latitude
    val lng = vehicle.longitude

    if (lat == null || lng == null || age == null || age > LOCATION_STALE_THRESHOLD_SECONDS) {
        return locatingArrival(vehicle, nowMillis) to tracker
    }

    if (targetIndex < 0 || waypoints.isEmpty()) {
        return locatingArrival(vehicle, nowMillis) to tracker
    }

    val progress = resolveStopTimelineProgress(
        waypoints = waypoints,
        latitude = lat,
        longitude = lng,
        tracker = tracker,
    )
    val nextTracker = VehicleStopTracker(
        lastPassedStopIndex = progress.lastPassedStopIndex,
        lastArrivedStopIndex = progress.lastArrivedStopIndex,
    )

    val target = waypoints[targetIndex]
    val distToStop = distanceMeters(lat, lng, target.latitude, target.longitude)
    val alreadyPassed = hasPassedSelectedStop(progress, targetIndex)

    if (alreadyPassed) {
        return StopArrival(
            vehicleId = vehicle.id,
            vehicleLabel = vehicle.label,
            status = VehicleStatus.Running,
            etaLabel = "이번 회차 통과 완료",
            etaColor = EtaGray,
            lastLocationLabel = formatLastLocationReceivedLabel(age),
        ) to nextTracker
    }

    val approaching = distToStop <= STOP_APPROACH_RADIUS_METERS
    val remainingMeters = remainingDistanceToStopMeters(
        waypoints = waypoints,
        lat = lat,
        lng = lng,
        targetIndex = targetIndex,
        progress = progress,
    )

    val status = when {
        approaching -> VehicleStatus.Approaching
        else -> VehicleStatus.Running
    }

    val etaMinutes = when {
        approaching || (remainingMeters != null && remainingMeters <= STOP_APPROACH_RADIUS_METERS) -> 0
        remainingMeters == null -> null
        else -> estimateEtaMinutes(remainingMeters, vehicle.speed)
    }

    val (etaLabel, etaColor) = when {
        etaMinutes == null -> "위치 확인 중" to EtaBlue
        etaMinutes <= 0 -> "곧 도착" to EtaOrange
        else -> "도착 예정 ${etaMinutes}분" to if (etaMinutes <= 3) EtaOrange else EtaGreen
    }

    return StopArrival(
        vehicleId = vehicle.id,
        vehicleLabel = vehicle.label,
        status = status,
        etaLabel = etaLabel,
        etaColor = etaColor,
        lastLocationLabel = formatLastLocationReceivedLabel(age),
    ) to nextTracker
}

private fun hasPassedSelectedStop(
    progress: StopTimelineProgress,
    targetIndex: Int,
): Boolean {
    // Already arrived at a later stop → this one is behind us.
    if (progress.lastArrivedStopIndex > targetIndex) return true
    if (progress.busOnStopIndex == targetIndex) return false
    if (progress.lastPassedStopIndex < targetIndex) return false
    if (progress.busOnStopIndex != null && progress.busOnStopIndex > targetIndex) return true
    if (progress.busSegmentFromIndex > targetIndex) return true
    if (progress.busSegmentFromIndex == targetIndex &&
        progress.busOnStopIndex == null &&
        progress.busSegmentProgress >= 0.40f
    ) {
        return true
    }
    return progress.lastPassedStopIndex >= targetIndex &&
        progress.busOnStopIndex == null &&
        progress.busSegmentFromIndex >= targetIndex
}

/**
 * Remaining distance along the route polyline to [targetIndex] (not straight-line only).
 * Returns null when the stop was already passed this trip.
 */
fun remainingDistanceToStopMeters(
    waypoints: List<StopWaypoint>,
    lat: Double,
    lng: Double,
    targetIndex: Int,
    progress: StopTimelineProgress,
): Double? {
    if (targetIndex !in waypoints.indices) return null
    if (hasPassedSelectedStop(progress, targetIndex)) return null

    val onStop = progress.busOnStopIndex
    if (onStop != null) {
        if (onStop == targetIndex) return 0.0
        if (onStop > targetIndex) return null
        var sum = 0.0
        for (i in onStop until targetIndex) {
            val a = waypoints[i]
            val b = waypoints[i + 1]
            sum += distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return sum
    }

    val from = progress.busSegmentFromIndex
    if (from < 0) {
        // Still treated as at origin.
        var sum = distanceMeters(
            lat, lng,
            waypoints[0].latitude, waypoints[0].longitude,
        )
        for (i in 0 until targetIndex) {
            val a = waypoints[i]
            val b = waypoints[i + 1]
            sum += distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return sum
    }
    if (from > targetIndex) return null
    if (from == targetIndex) {
        // Left the selected stop toward the next — treat as passed.
        return null
    }

    val next = (from + 1).coerceAtMost(waypoints.lastIndex)
    val a = waypoints[from]
    val b = waypoints[next]
    val segLen = distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
    val t = progress.busSegmentProgress.coerceIn(0f, 1f)
    var sum = segLen * (1.0 - t)
    for (i in next until targetIndex) {
        val s = waypoints[i]
        val e = waypoints[i + 1]
        sum += distanceMeters(s.latitude, s.longitude, e.latitude, e.longitude)
    }
    return sum
}

fun estimateEtaMinutes(remainingMeters: Double, speedMps: Double?): Int {
    val speed = if (speedMps != null && speedMps > 1.0) speedMps else DEFAULT_SPEED_MPS
    if (remainingMeters <= 0) return 0
    val minutes = ceil(remainingMeters / speed / 60.0).toInt()
    return minutes.coerceAtLeast(1)
}
