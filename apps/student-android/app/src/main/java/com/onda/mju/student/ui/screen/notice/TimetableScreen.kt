package com.onda.mju.student.ui.screen.notice

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

@Composable
fun TimetableScreen(
    modifier: Modifier = Modifier,
    initialRouteId: String = "giheung",
    onBackClick: () -> Unit = {},
) {
    BackHandler(onBack = onBackClick)

    val routes = remember { sampleTimetableRoutes() }
    var selectedRouteId by remember {
        mutableStateOf(
            routes.firstOrNull { it.id == initialRouteId }?.id ?: routes.first().id,
        )
    }
    var dayType by remember { mutableStateOf(TimetableDayType.Weekday) }
    var selectedDirectionId by remember {
        mutableStateOf(
            routes.first { it.id == selectedRouteId }
                .directionsFor(dayType)
                .first()
                .id,
        )
    }
    var routeMenuExpanded by remember { mutableStateOf(false) }

    val selectedRoute = remember(routes, selectedRouteId) {
        routes.first { it.id == selectedRouteId }
    }
    val directions = remember(selectedRoute, dayType) {
        selectedRoute.directionsFor(dayType)
    }
    val directionIndex = directions.indexOfFirst { it.id == selectedDirectionId }
        .takeIf { it >= 0 }
        ?: 0
    val activeDirectionId = directions.getOrNull(directionIndex)?.id
        ?: directions.firstOrNull()?.id.orEmpty()

    val schedule = remember(selectedRoute, dayType, activeDirectionId) {
        selectedRoute.findSchedule(dayType, activeDirectionId)
    }
    val rows = remember(schedule) {
        if (schedule == null || !schedule.operates) {
            emptyList()
        } else {
            schedule.departures.toRowUi()
        }
    }

    fun selectRoute(routeId: String) {
        selectedRouteId = routeId
        routeMenuExpanded = false
        val nextDirections = routes.first { it.id == routeId }.directionsFor(dayType)
        selectedDirectionId = nextDirections.first().id
    }

    fun selectDayType(next: TimetableDayType) {
        dayType = next
        val nextDirections = selectedRoute.directionsFor(next)
        if (nextDirections.none { it.id == selectedDirectionId }) {
            selectedDirectionId = nextDirections.first().id
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
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .clickable { routeMenuExpanded = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DirectionsBus, null, tint = OndaBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedRoute.name,
                            color = TitleBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Text(selectedRoute.summary, color = BodyGray, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = BodyGray)
                }
                DropdownMenu(
                    expanded = routeMenuExpanded,
                    onDismissRequest = { routeMenuExpanded = false },
                ) {
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        route.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TitleBlack,
                                        fontSize = 14.sp,
                                    )
                                    Text(route.summary, color = BodyGray, fontSize = 12.sp)
                                }
                            },
                            onClick = { selectRoute(route.id) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (directions.size >= 2) {
                SegmentedTwo(
                    left = directions[0].label,
                    right = directions[1].label,
                    selected = directionIndex.coerceIn(0, 1),
                    onSelect = { index ->
                        selectedDirectionId = directions[index].id
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedTwo(
                left = "학기 중 평일",
                right = "주말 · 방학",
                selected = if (dayType == TimetableDayType.Weekday) 0 else 1,
                onSelect = { index ->
                    selectDayType(
                        if (index == 0) TimetableDayType.Weekday
                        else TimetableDayType.WeekendVacation,
                    )
                },
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
                    HeaderCell("순번", Modifier.weight(0.7f))
                    HeaderCell("출발시간", Modifier.weight(1f))
                    HeaderCell("운행대수", Modifier.weight(1f))
                    HeaderCell("상태", Modifier.weight(1.1f))
                }

                if (schedule == null || !schedule.operates || rows.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (schedule?.operates == false) {
                                "주말·방학에는 운행하지 않습니다."
                            } else {
                                "표시할 시간표가 없습니다."
                            },
                            color = BodyGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = row.sequence.toString(),
                                modifier = Modifier.weight(0.7f),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = row.departureTime,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = row.vehicleCountLabel,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                            )
                            Text(
                                text = row.statusLabel,
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
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = BodyGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
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
