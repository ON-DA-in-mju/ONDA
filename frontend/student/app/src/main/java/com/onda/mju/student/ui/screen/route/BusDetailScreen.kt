package com.onda.mju.student.ui.screen.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val Purple = Color(0xFF7C3AED)
private val Danger = Color(0xFFE11D48)
private val OkGreen = Color(0xFF16A34A)
private val WarnAmber = Color(0xFFD97706)

@Composable
fun BusDetailScreen(
    data: BusDetailData,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onMoreReportsClick: () -> Unit = {},
) {
    val statusBadgeColor = when (data.status) {
        VehicleStatus.Running -> Color(0xFF22C55E)
        VehicleStatus.Approaching -> Color(0xFFEA580C)
        VehicleStatus.Waiting -> Color(0xFF6B7280)
        VehicleStatus.Locating -> Color(0xFF2563EB)
    }
    val currentStatusColor = when (data.status) {
        VehicleStatus.Running -> OkGreen
        VehicleStatus.Approaching -> Color(0xFFEA580C)
        VehicleStatus.Waiting -> BodyGray
        VehicleStatus.Locating -> OndaBlue
    }
    val earlyNoteColor = when {
        data.earlyNote.contains("일찍") -> OkGreen
        data.earlyNote.contains("늦게") -> WarnAmber
        data.earlyNote.contains("정시") -> OkGreen
        else -> BodyGray
    }
    val reportHeadline = when {
        data.reportFull + data.reportSeat + data.reportWait == 0 -> "최근 10분 제보 없음"
        data.reportFull > data.reportSeat -> "만석 가능성 높음"
        data.reportSeat > 0 -> "좌석 여유 제보 있음"
        data.reportWait > 0 -> "대기줄 제보 있음"
        else -> "학생 제보 요약"
    }
    val reportHeadlineColor = when {
        data.reportFull + data.reportSeat + data.reportWait == 0 -> BodyGray
        data.reportFull > data.reportSeat -> Danger
        else -> Purple
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
                "버스 상세정보",
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
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
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bus_detail_hero),
                    contentDescription = null,
                    modifier = Modifier
                        .width(120.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftBlue),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(data.title, color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        data.status.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(statusBadgeColor, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(data.direction, color = BodyGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            ) {
                DetailRow(Icons.Filled.SignalCellularAlt, "현재 상태", data.currentStatus, currentStatusColor)
                HorizontalDivider(color = CardBorder)
                DetailRow(Icons.Filled.Place, "다음 정류장", data.nextStop)
                HorizontalDivider(color = CardBorder)
                DetailRow(Icons.Filled.AccessTime, "예상 도착시간", data.etaLabel, OndaBlue)
                HorizontalDivider(color = CardBorder)
                DetailRow(Icons.Filled.Event, "예정 출발", data.scheduledDeparture, OndaBlue)
                HorizontalDivider(color = CardBorder)
                DetailRow(Icons.Filled.AccessTime, "실제 출발", data.actualDeparture, OndaBlue)
                HorizontalDivider(color = CardBorder)
                DetailRow(Icons.Filled.DirectionsBus, "조기 출발 여부", data.earlyNote, earlyNoteColor)
                HorizontalDivider(color = CardBorder)
                DetailRow(Icons.Filled.MyLocation, "마지막 위치 수신", data.lastLocationLabel)
                HorizontalDivider(color = CardBorder)
                DetailRow(
                    Icons.Filled.MyLocation,
                    "GPS 상태",
                    if (data.gpsOk) "정상" else "불안정",
                    if (data.gpsOk) OkGreen else Danger,
                )
                HorizontalDivider(color = CardBorder)
                DetailRow(Icons.Filled.Flag, "남은 정류장 수", "${data.remainingStops}개", OndaBlue)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Campaign, null, tint = Purple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "학생 제보 (최근 10분)",
                        color = TitleBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier.clickable(onClick = onMoreReportsClick),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("자세히 보기", color = Purple, fontSize = 12.sp)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Purple, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (data.reportFull + data.reportSeat + data.reportWait > 0) {
                            Icon(Icons.Filled.Warning, null, tint = reportHeadlineColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(reportHeadline, color = reportHeadlineColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "만석 제보 ${data.reportFull}명 | 좌석 여유 ${data.reportSeat}명 | 대기중 있음 ${data.reportWait}명",
                        color = BodyGray,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(data.reportAgo, color = BodyGray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OndaBlue, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.Notifications, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("현장 제보하기", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TitleBlack,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(SoftBlue, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = OndaBlue, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = BodyGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}
