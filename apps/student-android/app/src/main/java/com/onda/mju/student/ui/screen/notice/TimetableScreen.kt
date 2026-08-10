package com.onda.mju.student.ui.screen.notice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private data class TimetableRow(
    val time: String,
    val count: String,
    val duration: String = "약 35분",
    val status: String,
    val statusColor: Color,
    val statusBg: Color,
)

@Composable
fun TimetableScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    var directionIndex by remember { mutableStateOf(0) }
    var scheduleIndex by remember { mutableStateOf(0) }

    val rows = listOf(
        TimetableRow("08:00", "1대", status = "운행 예정", statusColor = OndaBlue, statusBg = SoftBlue),
        TimetableRow("09:05", "1대", status = "운행 예정", statusColor = OndaBlue, statusBg = SoftBlue),
        TimetableRow("10:00", "2대", status = "운행 중", statusColor = Color(0xFF0F766E), statusBg = Color(0xFFD1FAE5)),
        TimetableRow("11:10", "1대", status = "운행 예정", statusColor = OndaBlue, statusBg = SoftBlue),
        TimetableRow("12:20", "2대", status = "운행 예정", statusColor = OndaBlue, statusBg = SoftBlue),
        TimetableRow("14:00", "1대", status = "운행 예정", statusColor = OndaBlue, statusBg = SoftBlue),
        TimetableRow("16:00", "2대", status = "운행 예정", statusColor = OndaBlue, statusBg = SoftBlue),
        TimetableRow("17:15", "3대", status = "운행 예정", statusColor = OndaBlue, statusBg = SoftBlue),
        TimetableRow("18:15", "1대", status = "운행 취소", statusColor = Color(0xFFDC2626), statusBg = Color(0xFFFEE2E2)),
        TimetableRow("19:15", "1대", status = "운행 종료", statusColor = BodyGray, statusBg = Color(0xFFF3F4F6)),
    )

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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                    Text("시간표", color = TitleBlack, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("선택한 노선의 운행 시간표를 확인하세요", color = BodyGray, fontSize = 13.sp)
                }
                Image(
                    painter = painterResource(id = R.drawable.route_list_header_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(120.dp)
                        .aspectRatio(1.4f),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.DirectionsBus, null, tint = OndaBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("기흥역 통학버스", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("버스관리사무소 ⇄ 기흥역 5번 출구", color = BodyGray, fontSize = 12.sp)
                }
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = BodyGray)
            }

            Spacer(modifier = Modifier.height(12.dp))
            SegmentedTwo(
                left = "버스관리사무소 → 기흥역 5번 출구",
                right = "기흥역 5번 출구 → 버스관리사무소",
                selected = directionIndex,
                onSelect = { directionIndex = it },
            )
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedTwo(
                left = "학기 중 평일",
                right = "주말 · 방학",
                selected = scheduleIndex,
                onSelect = { scheduleIndex = it },
            )

            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    HeaderCell("출발시간", Modifier.weight(1f))
                    HeaderCell("운행대수", Modifier.weight(1f))
                    HeaderCell("소요시간", Modifier.weight(1f))
                    HeaderCell("상태", Modifier.weight(1.1f))
                }
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(row.time, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(row.count, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp)
                        Text(row.duration, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = BodyGray)
                        Text(
                            row.status,
                            color = row.statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(row.statusBg)
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftBlue, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Icon(Icons.Filled.Info, null, tint = OndaBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "만차 또는 현장 상황에 따라 실제 출발시간이 달라질 수 있습니다. 시간표는 교통상황에 따라 변동될 수 있습니다.",
                    color = OndaBlue,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, textAlign = TextAlign.Center, color = BodyGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SegmentedTwo(
    left: String,
    right: String,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(left, right).forEachIndexed { index, label ->
            val active = selected == index
            Text(
                text = label,
                color = if (active) Color.White else BodyGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) OndaBlue else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
            )
        }
    }
}
