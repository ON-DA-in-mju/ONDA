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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

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
    onBackClick: () -> Unit = {},
    onStopClick: (String) -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    onTimetableClick: () -> Unit = {},
) {
    val data = remember(routeId, liveData) { liveData ?: sampleRouteLive(routeId) }
    var directionIndex by remember { mutableIntStateOf(0) }
    var selectedVehicle by remember(routeId, data.vehicles.map { it.id }) {
        mutableStateOf(data.vehicles.firstOrNull()?.id.orEmpty())
    }
    var alertStops by remember {
        mutableStateOf(data.stops.filter { it.alertOn }.map { it.id }.toSet())
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val selectedLiveVehicle = data.vehicles.firstOrNull { it.id == selectedVehicle }
    val ageSeconds = remember(selectedLiveVehicle?.recordedAt, nowMillis) {
        locationAgeSeconds(selectedLiveVehicle?.recordedAt, nowMillis)
    }
    val lastUpdatedText = remember(ageSeconds) {
        lastUpdatedLabel(ageSeconds)
    }
    val locationStatusText = remember(ageSeconds) {
        locationStatusLabel(ageSeconds)
    }
    val statusColor = remember(ageSeconds) {
        locationStatusColor(ageSeconds)
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
                data.directions.forEachIndexed { index, pair ->
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
                                .background(statusColor, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(locationStatusText, color = statusColor, fontSize = 11.sp)
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                data.stops.forEachIndexed { index, stop ->
                    val isCurrent = stop.state == StopPassState.Current
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(28.dp),
                        ) {
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(10.dp)
                                        .background(
                                            if (data.stops[index - 1].state == StopPassState.Upcoming) {
                                                CardBorder
                                            } else {
                                                Teal
                                            },
                                        ),
                                )
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            when (stop.state) {
                                StopPassState.Departed, StopPassState.Passed -> {
                                    Box(
                                        modifier = Modifier.size(22.dp).background(Teal, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                StopPassState.Current -> {
                                    Box(
                                        modifier = Modifier.size(28.dp).background(OndaBlue, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.DirectionsBus, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                StopPassState.Destination -> {
                                    Icon(Icons.Filled.Flag, null, tint = BodyGray, modifier = Modifier.size(20.dp))
                                }
                                StopPassState.Upcoming -> {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(2.dp, CardBorder, CircleShape)
                                            .background(Color.White, CircleShape),
                                    )
                                }
                            }
                            if (index < data.stops.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(28.dp)
                                        .background(
                                            if (stop.state == StopPassState.Upcoming || stop.state == StopPassState.Destination) {
                                                CardBorder
                                            } else {
                                                Teal
                                            },
                                        ),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onStopClick(stop.id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                stop.name,
                                color = if (isCurrent) OndaBlue else TitleBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            when (stop.state) {
                                StopPassState.Departed, StopPassState.Passed -> {
                                    Text(
                                        stop.statusText,
                                        color = Teal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFFCCFBF1), RoundedCornerShape(999.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                    stop.subText?.let {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(it, color = BodyGray, fontSize = 11.sp)
                                    }
                                }
                                else -> Text(stop.statusText, color = if (isCurrent) OndaBlue else BodyGray, fontSize = 12.sp)
                            }
                        }
                        IconButton(
                            onClick = {
                                alertStops = if (stop.id in alertStops) alertStops - stop.id else alertStops + stop.id
                            },
                        ) {
                            Icon(
                                imageVector = if (stop.id in alertStops) {
                                    Icons.Filled.Notifications
                                } else {
                                    Icons.Filled.NotificationsOff
                                },
                                contentDescription = "하차 알림",
                                tint = if (stop.id in alertStops) OndaBlue else BodyGray,
                            )
                        }
                    }
                }
            }

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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• 하차 알림은 선택한 정류장 기준으로 설정됩니다.", color = OndaBlue, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")

private fun parseRecordedAtInstant(recordedAt: String): Instant? {
    val trimmed = recordedAt.trim()
    if (trimmed.isEmpty()) return null
    return try {
        Instant.parse(trimmed)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(trimmed).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                ZonedDateTime.parse(trimmed).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}

private fun locationAgeSeconds(recordedAt: String?, nowMillis: Long): Long? {
    if (recordedAt.isNullOrBlank()) return null
    val recordedInstant = parseRecordedAtInstant(recordedAt) ?: return null
    // Compare using epoch millis; Asia/Seoul is used for display context consistency.
    val nowInstant = Instant.ofEpochMilli(nowMillis).atZone(SeoulZone).toInstant()
    val ageMillis = (nowInstant.toEpochMilli() - recordedInstant.toEpochMilli()).coerceAtLeast(0L)
    return ageMillis / 1_000L
}

private fun lastUpdatedLabel(ageSeconds: Long?): String {
    if (ageSeconds == null) return "위치 정보 없음"
    return when {
        ageSeconds < 60L -> "마지막 갱신 ${ageSeconds}초 전"
        ageSeconds < 3_600L -> "마지막 갱신 ${ageSeconds / 60L}분 전"
        else -> "마지막 갱신 1시간 이상 전"
    }
}

private fun locationStatusLabel(ageSeconds: Long?): String {
    if (ageSeconds == null) return "위치 확인 중"
    return when {
        ageSeconds <= 30L -> "위치 정상"
        ageSeconds <= 60L -> "위치 불안정"
        else -> "위치 수신 지연"
    }
}

private fun locationStatusColor(ageSeconds: Long?): Color {
    if (ageSeconds == null) return Color(0xFF64748B)
    return when {
        ageSeconds <= 30L -> Color(0xFF16A34A)
        ageSeconds <= 60L -> Color(0xFFEA580C)
        else -> Color(0xFFDC2626)
    }
}
