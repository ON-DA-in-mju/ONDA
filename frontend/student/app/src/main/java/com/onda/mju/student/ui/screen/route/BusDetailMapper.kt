package com.onda.mju.student.ui.screen.route

import com.onda.mju.student.data.remote.dto.OperationDeviceStatusDto
import com.onda.mju.student.data.remote.dto.OperationDto
import com.onda.mju.student.data.remote.dto.OperationStopProgressDto
import com.onda.mju.student.data.remote.dto.VehicleLocationDto
import com.onda.mju.student.data.route.OperationalRouteResolver
import com.onda.mju.student.data.route.RouteStopCatalog
import com.onda.mju.student.data.route.StudentRouteIds
import com.onda.mju.student.ui.screen.community.CommunityReport
import com.onda.mju.student.ui.screen.community.ReportType
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.max

private val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")
private val RegisteredAtPattern: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

private const val REPORT_WINDOW_MINUTES = 10L

/**
 * Builds bus-detail UI from DB operation + live location/progress + recent community reports.
 * [operationId] is the live vehicle id (= operations.id).
 */
fun buildBusDetailData(
    operationId: String,
    operations: List<OperationDto>,
    location: VehicleLocationDto?,
    deviceStatus: OperationDeviceStatusDto?,
    stopProgress: OperationStopProgressDto?,
    reports: List<CommunityReport>,
    nowMillis: Long = System.currentTimeMillis(),
): BusDetailData {
    val operation = operations.firstOrNull { it.id == operationId }
    if (operation == null) {
        return BusDetailData(
            vehicleId = operationId,
            title = "버스 정보 없음",
            status = VehicleStatus.Locating,
            direction = "-",
            currentStatus = "정보 없음",
            nextStop = "-",
            etaLabel = "-",
            scheduledDeparture = "-",
            actualDeparture = "-",
            earlyNote = "정보 없음",
            lastLocationLabel = "-",
            gpsOk = false,
            remainingStops = 0,
            reportFull = 0,
            reportSeat = 0,
            reportWait = 0,
            reportAgo = "제보 없음",
        )
    }

    val route = operation.schedule?.route
    val routeName = route?.routeName.orEmpty()
    val uiRouteId = StudentRouteIds.uiIdForRouteName(routeName)
        ?: StudentRouteIds.CITY_SHUTTLE
    val waypoints = RouteStopCatalog.waypoints(uiRouteId)

    val busLabel = operation.bus?.busName
        ?: operation.bus?.vehicleNumber
        ?: "운행 차량"
    val title = if (routeName.isNotBlank()) {
        "$routeName · $busLabel"
    } else {
        busLabel
    }

    val direction = directionLabel(route?.startLocation, route?.endLocation, waypoints)

    val vehicleStatus = when (operation.status.uppercase()) {
        "IN_PROGRESS" -> VehicleStatus.Running
        "SCHEDULED" -> VehicleStatus.Waiting
        else -> VehicleStatus.Waiting
    }
    val currentStatus = when (operation.status.uppercase()) {
        "IN_PROGRESS" -> "운행중"
        "SCHEDULED" -> "운행 대기"
        "COMPLETED" -> "운행 종료"
        "CANCELLED" -> "운행 취소"
        else -> operation.status.ifBlank { "-" }
    }

    val tracker = stopProgress?.toVehicleStopTracker(waypoints) ?: VehicleStopTracker()
    val lastIndex = waypoints.lastIndex
    val nextIndex = when {
        lastIndex < 0 -> -1
        tracker.lastPassedStopIndex < 0 -> 0
        tracker.lastPassedStopIndex >= lastIndex -> lastIndex
        else -> tracker.lastPassedStopIndex + 1
    }
    val nextStop = waypoints.getOrNull(nextIndex)?.name ?: "-"
    val remainingStops = when {
        lastIndex < 0 -> 0
        tracker.lastPassedStopIndex < 0 -> waypoints.size
        else -> max(0, lastIndex - tracker.lastPassedStopIndex)
    }

    val ageSeconds = timestampAgeSeconds(location?.recordedAt, nowMillis)
    val gpsOk = resolveGpsOk(deviceStatus, ageSeconds)
    val lastLocationLabel = formatRelativeAgo(ageSeconds)

    val etaLabel = estimateNextStopEtaLabel(
        location = location,
        ageSeconds = ageSeconds,
        waypoints = waypoints,
        tracker = tracker,
        nextIndex = nextIndex,
        speed = location?.speed,
    )

    val scheduledDeparture = formatHm(operation.schedule?.departureTime)
    val actualDeparture = formatStartedAtKst(operation.startedAt)
    val earlyNote = earlyDepartureNote(
        scheduledDepartureTime = operation.schedule?.departureTime,
        startedAt = operation.startedAt,
    )

    val recent = recentRouteReports(
        reports = reports,
        routeName = routeName,
        busLabel = busLabel,
        nowMillis = nowMillis,
    )
    val reportFull = recent.count { it.type == ReportType.Full }
    val reportSeat = recent.count { it.type == ReportType.SeatAvailable }
    val reportWait = recent.count {
        it.type == ReportType.LongQueue || it.type == ReportType.ShortQueue
    }
    val reportAgo = recent
        .mapNotNull { parseRegisteredAtMillis(it.registeredAt) }
        .maxOrNull()
        ?.let { formatRelativeAgo((nowMillis - it) / 1_000L) + " 기준" }
        ?: if (recent.isEmpty()) "최근 10분 제보 없음" else "최근 10분 기준"

    return BusDetailData(
        vehicleId = operationId,
        title = title,
        status = vehicleStatus,
        direction = direction,
        currentStatus = currentStatus,
        nextStop = nextStop,
        etaLabel = etaLabel,
        scheduledDeparture = scheduledDeparture,
        actualDeparture = actualDeparture,
        earlyNote = earlyNote,
        lastLocationLabel = lastLocationLabel,
        gpsOk = gpsOk,
        remainingStops = remainingStops,
        reportFull = reportFull,
        reportSeat = reportSeat,
        reportWait = reportWait,
        reportAgo = reportAgo,
    )
}

private fun directionLabel(
    start: String?,
    end: String?,
    waypoints: List<StopWaypoint>,
): String {
    val from = start?.takeIf { it.isNotBlank() } ?: waypoints.firstOrNull()?.name
    val to = end?.takeIf { it.isNotBlank() } ?: waypoints.lastOrNull()?.name
    return when {
        !from.isNullOrBlank() && !to.isNullOrBlank() -> "$from → $to"
        !from.isNullOrBlank() -> from
        else -> "-"
    }
}

private fun resolveGpsOk(
    deviceStatus: OperationDeviceStatusDto?,
    ageSeconds: Long?,
): Boolean {
    if (ageSeconds == null || ageSeconds > LOCATION_STALE_THRESHOLD_SECONDS) return false
    val flagged = deviceStatus?.gpsOk
    if (flagged == false) return false
    if (deviceStatus?.gpsEnabled == false) return false
    return true
}

private fun estimateNextStopEtaLabel(
    location: VehicleLocationDto?,
    ageSeconds: Long?,
    waypoints: List<StopWaypoint>,
    tracker: VehicleStopTracker,
    nextIndex: Int,
    speed: Double?,
): String {
    if (nextIndex < 0 || waypoints.isEmpty()) return "-"
    if (tracker.lastPassedStopIndex >= waypoints.lastIndex) return "종점 도착"
    val lat = location?.latitude
    val lng = location?.longitude
    if (lat == null || lng == null || ageSeconds == null ||
        ageSeconds > LOCATION_STALE_THRESHOLD_SECONDS
    ) {
        return "위치 확인 중"
    }

    val progress = resolveStopTimelineProgress(
        waypoints = waypoints,
        latitude = lat,
        longitude = lng,
        tracker = tracker,
    )
    val target = waypoints[nextIndex]
    val distToStop = distanceMeters(lat, lng, target.latitude, target.longitude)
    if (distToStop <= STOP_APPROACH_RADIUS_METERS) return "곧 도착"

    val remainingMeters = remainingDistanceToStopMeters(
        waypoints = waypoints,
        lat = lat,
        lng = lng,
        targetIndex = nextIndex,
        progress = progress,
    ) ?: return "위치 확인 중"

    val minutes = estimateEtaMinutes(remainingMeters, speed)
    return when {
        minutes <= 0 -> "곧 도착"
        else -> "${minutes}분 후"
    }
}

private fun recentRouteReports(
    reports: List<CommunityReport>,
    routeName: String,
    busLabel: String,
    nowMillis: Long,
): List<CommunityReport> {
    val canonicalRoute = OperationalRouteResolver.canonicalRouteName(routeName)
    val windowStart = nowMillis - REPORT_WINDOW_MINUTES * 60_000L
    return reports.filter { report ->
        val created = parseRegisteredAtMillis(report.registeredAt) ?: return@filter false
        if (created < windowStart) return@filter false
        val reportRoute = OperationalRouteResolver.canonicalRouteName(report.routeLabel)
        val sameRoute = canonicalRoute.isNotBlank() &&
            (reportRoute == canonicalRoute ||
                report.routeLabel.contains(routeName, ignoreCase = true) ||
                routeName.contains(report.routeLabel, ignoreCase = true))
        if (!sameRoute) return@filter false
        // Prefer same vehicle when the report named one; otherwise keep route-level reports.
        val reportVehicle = report.vehicleLabel.trim()
        if (reportVehicle.isBlank() || reportVehicle == "-" || reportVehicle == "미상") {
            true
        } else {
            reportVehicle.contains(busLabel, ignoreCase = true) ||
                busLabel.contains(reportVehicle, ignoreCase = true)
        }
    }
}

private fun parseRegisteredAtMillis(registeredAt: String): Long? {
    val trimmed = registeredAt.trim()
    if (trimmed.isEmpty() || trimmed == "-" || trimmed == "방금 전") {
        return if (trimmed == "방금 전") System.currentTimeMillis() else null
    }
    return try {
        java.time.LocalDateTime.parse(trimmed, RegisteredAtPattern)
            .atZone(SeoulZone)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun formatRelativeAgo(ageSeconds: Long?): String {
    if (ageSeconds == null) return "-"
    return when {
        ageSeconds < 3L -> "방금 전"
        ageSeconds < 60L -> "${ageSeconds}초 전"
        ageSeconds < 3_600L -> "${ageSeconds / 60L}분 전"
        else -> "1시간 이상 전"
    }
}

private fun formatHm(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val parts = value.trim().split(':')
    return if (parts.size >= 2) {
        "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
    } else {
        value
    }
}

private fun formatStartedAtKst(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val instant = parseRecordedAtInstant(value) ?: return "-"
    val local = instant.atZone(SeoulZone).toLocalTime()
    return "%02d:%02d".format(local.hour, local.minute)
}

private fun earlyDepartureNote(
    scheduledDepartureTime: String?,
    startedAt: String?,
): String {
    val scheduled = parseDepartureLocalTime(scheduledDepartureTime) ?: return "정보 없음"
    val startedInstant = startedAt?.let { parseRecordedAtInstant(it) } ?: return "정보 없음"
    val actual = startedInstant.atZone(SeoulZone).toLocalTime()
    val diffMinutes = Duration.between(scheduled, actual).toMinutes()
    return when {
        diffMinutes <= -1L -> "예정 시간보다 ${-diffMinutes}분 일찍 출발"
        diffMinutes >= 1L -> "예정 시간보다 ${diffMinutes}분 늦게 출발"
        else -> "정시 출발"
    }
}

private fun parseDepartureLocalTime(value: String?): LocalTime? {
    if (value.isNullOrBlank()) return null
    val trimmed = value.trim()
    return try {
        when {
            trimmed.length >= 8 -> LocalTime.parse(trimmed.take(8))
            trimmed.length >= 5 -> LocalTime.parse(trimmed.take(5))
            else -> null
        }
    } catch (_: DateTimeParseException) {
        null
    }
}
