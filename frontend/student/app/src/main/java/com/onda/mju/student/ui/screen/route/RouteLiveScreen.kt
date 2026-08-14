package com.onda.mju.student.ui.screen.route

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.onda.mju.student.data.route.RouteStopCatalog
import com.onda.mju.student.data.remote.dto.OperationDeviceStatusDto
import com.onda.mju.student.data.remote.dto.OperationStopProgressDto
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import java.time.ZoneId

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val Teal = Color(0xFF14B8A6)

@Composable
fun RouteLiveScreen(
    routeId: String,
    modifier: Modifier = Modifier,
    liveData: RouteLiveData? = null,
    deviceStatuses: Map<String, OperationDeviceStatusDto> = emptyMap(),
    stopProgress: Map<String, OperationStopProgressDto> = emptyMap(),
    stopCoordinates: StopCoordinateMap = emptyMap(),
    routeCatalogRevision: Int = 0,
    onBackClick: () -> Unit = {},
    onStopClick: (String) -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    onTimetableClick: () -> Unit = {},
) {
    val data = liveData ?: remember(routeId) { sampleRouteLive(routeId) }
    val catalogRevision = maxOf(routeCatalogRevision, RouteStopCatalog.revision())
    val stopConfig = remember(data.routeId, stopCoordinates, catalogRevision) {
        routeStopConfig(data.routeId)
    }
    val directions = remember(stopConfig) { stopConfig.directions }
    var directionIndex by remember(routeId) { mutableIntStateOf(0) }
    val waypoints = remember(data.routeId, directionIndex, stopCoordinates, catalogRevision) {
        stopWaypointsForRoute(data.routeId, directionIndex, stopCoordinates)
    }
    var selectedVehicle by remember(routeId) {
        mutableStateOf(data.vehicles.firstOrNull()?.id.orEmpty())
    }

    // If the selected operation ended / left the list, move selection to a remaining vehicle.
    val vehicleIds = remember(data.vehicles) { data.vehicles.map { it.id } }
    LaunchedEffect(vehicleIds) {
        if (selectedVehicle.isNotEmpty() && selectedVehicle !in vehicleIds) {
            selectedVehicle = vehicleIds.firstOrNull().orEmpty()
            Log.d(
                "ONDA_SUPABASE",
                "selected vehicle reset to=${selectedVehicle.ifEmpty { "<none>" }}",
            )
        } else if (selectedVehicle.isEmpty() && vehicleIds.isNotEmpty()) {
            selectedVehicle = vehicleIds.first()
        }
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val selectedLiveVehicle = data.vehicles.firstOrNull { it.id == selectedVehicle }
    val selectedDeviceStatus = selectedLiveVehicle?.let { deviceStatuses[it.id] }

    // Per-vehicle tracker. DB 진행을 바닥값으로 두고 GPS로만 전진한다.
    var trackerByVehicle by remember(routeId, directionIndex) {
        mutableStateOf<Map<String, VehicleStopTracker>>(emptyMap())
    }

    val effectiveLat = selectedLiveVehicle?.latitude
    val effectiveLng = selectedLiveVehicle?.longitude
    val trackerKey = selectedLiveVehicle?.id ?: "none"
    val dbTracker = remember(waypoints, trackerKey, stopProgress[trackerKey]) {
        stopProgress[trackerKey]?.toVehicleStopTracker(waypoints) ?: VehicleStopTracker()
    }
    val tracker = (trackerByVehicle[trackerKey] ?: VehicleStopTracker()).mergeAhead(dbTracker)

    val timelineProgress = remember(
        waypoints,
        trackerKey,
        effectiveLat,
        effectiveLng,
        tracker,
    ) {
        resolveStopTimelineProgress(
            waypoints = waypoints,
            latitude = effectiveLat,
            longitude = effectiveLng,
            tracker = tracker,
        )
    }
    LaunchedEffect(trackerKey, dbTracker) {
        val prev = trackerByVehicle[trackerKey] ?: VehicleStopTracker()
        val merged = prev.mergeAhead(dbTracker)
        if (prev != merged) {
            trackerByVehicle = trackerByVehicle + (trackerKey to merged)
        }
    }
    LaunchedEffect(
        trackerKey,
        timelineProgress.lastPassedStopIndex,
        timelineProgress.lastArrivedStopIndex,
        directionIndex,
    ) {
        val next = VehicleStopTracker(
            lastPassedStopIndex = timelineProgress.lastPassedStopIndex,
            lastArrivedStopIndex = timelineProgress.lastArrivedStopIndex,
        )
        val prev = trackerByVehicle[trackerKey]
        if (prev != next) {
            trackerByVehicle = trackerByVehicle + (trackerKey to next)
        }
    }

    val locationAgeSeconds = remember(selectedLiveVehicle?.recordedAt, nowMillis) {
        timestampAgeSeconds(selectedLiveVehicle?.recordedAt, nowMillis)
    }
    val lastUpdatedText = remember(locationAgeSeconds) {
        lastUpdatedLabel(locationAgeSeconds)
    }
    val connectionStatus = remember(
        selectedDeviceStatus,
        selectedLiveVehicle?.recordedAt,
        nowMillis,
    ) {
        resolveConnectionStatus(
            deviceStatus = selectedDeviceStatus,
            locationRecordedAt = selectedLiveVehicle?.recordedAt,
            nowMillis = nowMillis,
        )
    }

    var lastLoggedConnection by remember {
        mutableStateOf<Pair<String, String>?>(null) // operationId to label
    }
    LaunchedEffect(selectedLiveVehicle?.id, connectionStatus.label) {
        val vehicle = selectedLiveVehicle ?: return@LaunchedEffect
        val previous = lastLoggedConnection
        val oldStatus = previous?.takeIf { it.first == vehicle.id }?.second
        if (previous?.first == vehicle.id && previous.second == connectionStatus.label) {
            return@LaunchedEffect
        }
        Log.d(
            "ONDA_SUPABASE",
            "connection status changed operationId=${vehicle.id}, " +
                "oldStatus=${oldStatus ?: "-"}, newStatus=${connectionStatus.label}, " +
                "gpsEnabled=${selectedDeviceStatus?.gpsEnabled}, " +
                "heartbeatAge=${connectionStatus.heartbeatAgeSeconds}, " +
                "locationAge=${connectionStatus.locationAgeSeconds}",
        )
        lastLoggedConnection = vehicle.id to connectionStatus.label
    }

    // Temporary: verify live vehicles passed from shell.
    LaunchedEffect(data.vehicles) {
        data.vehicles.forEach { vehicle ->
            Log.d(
                "ONDA_SUPABASE",
                "route vehicle operationId=${vehicle.id}, label=${vehicle.label}, " +
                    "latitude=${vehicle.latitude}, longitude=${vehicle.longitude}, " +
                    "recordedAt=${vehicle.recordedAt}",
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                data.routeName,
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                "시간표",
                color = OndaBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .clickable(onClick = onTimetableClick),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                directions.forEachIndexed { index, pair ->
                    val selected = directionIndex == index
                    Text(
                        text = "${pair.first} → ${pair.second}",
                        color = if (selected) Color.White else BodyGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) OndaBlue else Color.Transparent)
                            .clickable { directionIndex = index }
                            .padding(horizontal = 6.dp, vertical = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SoftBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.DirectionsBus, null, tint = OndaBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        buildAnnotatedString {
                            append("현재 ")
                            withStyle(SpanStyle(color = OndaBlue, fontWeight = FontWeight.Bold)) {
                                append("${data.runningCount}대")
                            }
                            append(" 운행 중")
                        },
                        color = TitleBlack,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        buildAnnotatedString {
                            append("다음 출발 ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(data.nextDeparture)
                            }
                        },
                        color = BodyGray,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Refresh, null, tint = BodyGray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(lastUpdatedText, color = BodyGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Spacer(
                            modifier = Modifier
                                .size(6.dp)
                                .background(connectionStatus.color, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(connectionStatus.label, color = connectionStatus.color, fontSize = 11.sp)
                    }
                }
                val isRunning = data.runningCount > 0
                Text(
                    if (isRunning) "운행 중" else "운행 예정",
                    color = if (isRunning) Color(0xFF0F766E) else Color(0xFF1D4ED8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (isRunning) Color(0xFFD1FAE5) else Color(0xFFDBEAFE),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                data.vehicles.forEach { vehicle ->
                    val selected = selectedVehicle == vehicle.id
                    Column(
                        modifier = Modifier
                            .width(108.dp)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) OndaBlue else CardBorder,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedVehicle = vehicle.id
                                onVehicleClick(vehicle.id)
                            }
                            .padding(12.dp),
                    ) {
                        Icon(Icons.Filled.DirectionsBus, null, tint = OndaBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(vehicle.label, color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(vehicle.status.label, color = if (selected) OndaBlue else BodyGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "예정 출발 ${formatScheduledTime(vehicle.scheduledDepartureTime)}",
                            color = BodyGray,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                        Text(
                            "실제 출발 ${formatStartedAtKst(vehicle.actualStartedAt)}",
                            color = BodyGray,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            StopTimeline(
                progress = timelineProgress,
                onStopClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftBlue, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, null, tint = OndaBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("• 예상 도착시간은 교통상황에 따라 변경될 수 있어요.", color = OndaBlue, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")

private val TimelineRowHeight = 64.dp
private val TimelineRailWidth = 28.dp

@Composable
private fun StopTimeline(
    progress: StopTimelineProgress,
    onStopClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stops = progress.nodes
    if (stops.isEmpty()) return

    val density = LocalDensity.current
    val rowHeightPx = with(density) { TimelineRowHeight.toPx() }
    val railWidthPx = with(density) { TimelineRailWidth.toPx() }
    val lastPassed = progress.lastPassedStopIndex
    val showFloatingBus = progress.busOnStopIndex == null &&
        progress.busSegmentFromIndex >= 0 &&
        progress.busSegmentFromIndex < stops.lastIndex

    val busIconPx = with(density) { 28.dp.toPx() }
    val busX = ((railWidthPx - busIconPx) / 2f).roundToInt()

    Box(modifier = modifier) {
        // Continuous rail behind icons.
        Box(
            modifier = Modifier
                .width(TimelineRailWidth)
                .height(TimelineRowHeight * stops.size)
                .align(Alignment.TopStart)
                .zIndex(0f),
        ) {
            val topPad = TimelineRowHeight / 2
            val lineHeight = TimelineRowHeight * (stops.size - 1).coerceAtLeast(0)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topPad)
                    .width(2.dp)
                    .height(lineHeight)
                    .background(CardBorder),
            )
            val activeEndIndex: Float = when {
                progress.busOnStopIndex != null -> progress.busOnStopIndex!!.toFloat()
                showFloatingBus -> progress.busSegmentFromIndex + progress.busSegmentProgress
                lastPassed >= 0 -> lastPassed.toFloat()
                else -> 0f
            }
            val maxSegment = (stops.size - 1).coerceAtLeast(0).toFloat()
            val clampedEnd = activeEndIndex.coerceIn(0f, maxSegment)
            val activeHeight = TimelineRowHeight * clampedEnd
            if (clampedEnd > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topPad)
                        .width(2.dp)
                        .height(activeHeight)
                        .background(Teal),
                )
            }
        }

        // Stop markers / checks / flags (under bus).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f),
        ) {
            stops.forEachIndexed { index, stop ->
                val isCurrent = stop.state == StopPassState.Current || progress.busOnStopIndex == index
                val arrivedDestination =
                    index == stops.lastIndex && lastPassed >= stops.lastIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TimelineRowHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(TimelineRailWidth),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Placeholder when bus sits here — bus is drawn in the top overlay.
                        // At terminus keep the flag under the bus so arrival reads as "on the flag".
                        TimelineStopIcon(
                            stop = stop,
                            hideForBusOverlay = (progress.busOnStopIndex == index ||
                                stop.state == StopPassState.Current) && !arrivedDestination,
                            arrivedDestination = arrivedDestination,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onStopClick(stop.id) }
                            .padding(vertical = 2.dp),
                    ) {
                        Text(
                            stop.name,
                            color = if (isCurrent) OndaBlue else TitleBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        when {
                            stop.state == StopPassState.Departed || stop.state == StopPassState.Passed ||
                                (arrivedDestination && stop.statusText == "도착 완료") -> {
                                Text(
                                    stop.statusText,
                                    color = Teal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFFCCFBF1), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            else -> Text(
                                stop.statusText,
                                color = if (isCurrent) OndaBlue else BodyGray,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        // Bus always on top of rail / checks / flags.
        Box(
            modifier = Modifier
                .width(TimelineRailWidth)
                .height(TimelineRowHeight * stops.size)
                .align(Alignment.TopStart)
                .zIndex(2f),
        ) {
            val busRowIndex = progress.busOnStopIndex
            if (busRowIndex != null) {
                val busY = rowHeightPx * busRowIndex + rowHeightPx / 2f - busIconPx / 2f
                TimelineBusIcon(
                    modifier = Modifier.offset {
                        IntOffset(busX, busY.roundToInt())
                    },
                )
            } else if (showFloatingBus) {
                val busY = rowHeightPx * (progress.busSegmentFromIndex + progress.busSegmentProgress) +
                    rowHeightPx / 2f - busIconPx / 2f
                TimelineBusIcon(
                    modifier = Modifier.offset {
                        IntOffset(busX, busY.roundToInt())
                    },
                )
            }
        }
    }
}

@Composable
private fun TimelineBusIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(OndaBlue, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.DirectionsBus,
            contentDescription = "현재 버스 위치",
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TimelineStopIcon(
    stop: LiveStopNode,
    hideForBusOverlay: Boolean,
    arrivedDestination: Boolean,
) {
    when {
        arrivedDestination -> {
            Icon(Icons.Filled.Flag, null, tint = Teal, modifier = Modifier.size(20.dp))
        }
        hideForBusOverlay -> {
            // Keep layout slot; bus is painted in the zIndex overlay above.
            Spacer(modifier = Modifier.size(28.dp))
        }
        stop.state == StopPassState.Departed || stop.state == StopPassState.Passed -> {
            Box(
                modifier = Modifier.size(22.dp).background(Teal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        stop.state == StopPassState.Destination -> {
            Icon(Icons.Filled.Flag, null, tint = BodyGray, modifier = Modifier.size(20.dp))
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(2.dp, CardBorder, CircleShape)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

private fun lastUpdatedLabel(ageSeconds: Long?): String = formatLastUpdatedShortLabel(ageSeconds)

private data class ConnectionStatusUi(
    val label: String,
    val color: Color,
    val heartbeatAgeSeconds: Long?,
    val locationAgeSeconds: Long?,
)

private fun resolveConnectionStatus(
    deviceStatus: OperationDeviceStatusDto?,
    locationRecordedAt: String?,
    nowMillis: Long,
): ConnectionStatusUi {
    val heartbeatAge = timestampAgeSeconds(deviceStatus?.updatedAt, nowMillis)
    // Age is based only on latest vehicle_locations.recorded_at — same lat/lng is OK if row is fresh.
    val locationAge = timestampAgeSeconds(locationRecordedAt, nowMillis)

    return when {
        deviceStatus == null -> ConnectionStatusUi(
            label = "상태 확인 중",
            color = Color(0xFF64748B),
            heartbeatAgeSeconds = heartbeatAge,
            locationAgeSeconds = locationAge,
        )
        heartbeatAge == null || heartbeatAge > HEARTBEAT_STALE_THRESHOLD_SECONDS -> ConnectionStatusUi(
            label = "연결 확인 불가",
            color = Color(0xFFDC2626),
            heartbeatAgeSeconds = heartbeatAge,
            locationAgeSeconds = locationAge,
        )
        deviceStatus.gpsEnabled == false -> ConnectionStatusUi(
            label = "GPS 꺼짐",
            color = Color(0xFFEA580C),
            heartbeatAgeSeconds = heartbeatAge,
            locationAgeSeconds = locationAge,
        )
        locationAge == null || locationAge > LOCATION_STALE_THRESHOLD_SECONDS -> ConnectionStatusUi(
            label = "위치 확인 불가",
            color = Color(0xFFEA580C),
            heartbeatAgeSeconds = heartbeatAge,
            locationAgeSeconds = locationAge,
        )
        else -> ConnectionStatusUi(
            label = "위치 정상",
            color = Color(0xFF16A34A),
            heartbeatAgeSeconds = heartbeatAge,
            locationAgeSeconds = locationAge,
        )
    }
}

private fun formatScheduledTime(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val parts = value.trim().split(':')
    return if (parts.size >= 2) {
        val hour = parts[0].padStart(2, '0')
        val minute = parts[1].padStart(2, '0')
        "$hour:$minute"
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
