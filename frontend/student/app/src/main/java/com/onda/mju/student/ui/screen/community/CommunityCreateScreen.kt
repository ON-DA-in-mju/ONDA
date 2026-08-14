package com.onda.mju.student.ui.screen.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.data.remote.dto.BusDto
import com.onda.mju.student.data.remote.dto.RouteDetailDto
import com.onda.mju.student.data.remote.repository.BusRepository
import com.onda.mju.student.data.remote.repository.RouteRepository
import com.onda.mju.student.data.remote.repository.RouteStopInfo
import com.onda.mju.student.data.remote.repository.RouteStopsRepository
import com.onda.mju.student.data.remote.repository.displayLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val WarnBg = Color(0xFFFFF4E5)
private val WarnText = Color(0xFFEA580C)

@Composable
fun CommunityCreateScreen(
    modifier: Modifier = Modifier,
    initialReport: CommunityReport? = null,
    onBackClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSubmit: (CommunityReport) -> Unit = {},
) {
    val isEdit = initialReport != null
    val routeRepository = remember { RouteRepository() }
    val routeStopsRepository = remember { RouteStopsRepository() }
    val busRepository = remember { BusRepository() }

    var routes by remember { mutableStateOf<List<RouteDetailDto>>(emptyList()) }
    var stops by remember { mutableStateOf<List<RouteStopInfo>>(emptyList()) }
    var buses by remember { mutableStateOf<List<BusDto>>(emptyList()) }
    var loadingOptions by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var stopsLoading by remember { mutableStateOf(false) }

    var selectedRoute by remember(initialReport?.id) { mutableStateOf<RouteDetailDto?>(null) }
    var selectedStop by remember(initialReport?.id) { mutableStateOf<RouteStopInfo?>(null) }
    var selectedBus by remember(initialReport?.id) { mutableStateOf<BusDto?>(null) }
    var selectedType by remember(initialReport?.id) { mutableStateOf(initialReport?.type) }
    var note by remember(initialReport?.id) { mutableStateOf(initialReport?.body ?: "") }

    var routeMenuOpen by remember { mutableStateOf(false) }
    var stopMenuOpen by remember { mutableStateOf(false) }
    var vehicleMenuOpen by remember { mutableStateOf(false) }

    val situationOptions = listOf(
        ReportType.Arrival,
        ReportType.Full,
        ReportType.SeatAvailable,
        ReportType.LongQueue,
        ReportType.ShortQueue,
        ReportType.TrafficJam,
        ReportType.Passed,
        ReportType.Other,
    )

    LaunchedEffect(Unit) {
        loadingOptions = true
        loadError = null
        try {
            val routeList = withContext(Dispatchers.IO) {
                runCatching { routeRepository.getActiveRoutes() }
                    .onFailure { android.util.Log.e("ONDA_REPORTS", "routes load failed: ${it.message}", it) }
                    .getOrDefault(emptyList())
            }
            val busList = withContext(Dispatchers.IO) {
                runCatching { busRepository.getActiveBuses() }
                    .onFailure { android.util.Log.e("ONDA_REPORTS", "buses load failed: ${it.message}", it) }
                    .getOrDefault(emptyList())
            }
            routes = routeList
            buses = busList
            android.util.Log.d("ONDA_REPORTS", "create options routes=${routeList.size}, buses=${busList.size}")

            if (routeList.isEmpty() && busList.isEmpty()) {
                loadError = "노선·차량 정보를 불러오지 못했습니다. 네트워크와 로그인을 확인해 주세요."
            } else if (routeList.isEmpty()) {
                loadError = "노선 정보를 불러오지 못했습니다."
            }

            val initialRouteName = initialReport?.routeLabel
            selectedRoute = routeList.firstOrNull { it.routeName == initialRouteName }
                ?: routeList.firstOrNull()

            selectedBus = busList.firstOrNull { bus ->
                val label = bus.displayLabel()
                label == initialReport?.vehicleLabel ||
                    bus.busName == initialReport?.vehicleLabel ||
                    bus.vehicleNumber == initialReport?.vehicleLabel
            }
        } catch (e: Exception) {
            loadError = e.message ?: "선택 항목을 불러오지 못했습니다."
            routes = emptyList()
            buses = emptyList()
        } finally {
            loadingOptions = false
        }
    }

    LaunchedEffect(selectedRoute?.id, selectedRoute?.routeName) {
        val routeName = selectedRoute?.routeName?.trim().orEmpty()
        if (routeName.isBlank()) {
            stops = emptyList()
            selectedStop = null
            return@LaunchedEffect
        }
        stopsLoading = true
        try {
            val stopList = withContext(Dispatchers.IO) {
                runCatching { routeStopsRepository.getStopsForRouteName(routeName) }
                    .getOrDefault(emptyList())
            }
            stops = stopList
            selectedStop = stopList.firstOrNull { it.name == initialReport?.stopName }
                ?: stopList.firstOrNull()
        } catch (_: Exception) {
            stops = emptyList()
            selectedStop = null
        } finally {
            stopsLoading = false
        }
    }

    val routeLabel = selectedRoute?.routeName ?: "노선을 선택하세요"
    val stopLabel = when {
        selectedRoute == null -> "노선을 먼저 선택하세요"
        stopsLoading -> "정류장 불러오는 중…"
        selectedStop != null -> selectedStop!!.name
        stops.isEmpty() -> "정류장이 없습니다"
        else -> "정류장을 선택하세요"
    }
    val vehicleLabel = selectedBus?.displayLabel() ?: "선택 안 함"
    val directionLabel = selectedRoute?.direction?.takeIf { it.isNotBlank() } ?: "-"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isEdit) "제보 수정" else "간편 제보",
                    color = TitleBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (!isEdit) {
                IconButton(onClick = onNotificationClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = OndaBlue)
                }
            }
        }
        Text(
            text = if (isEdit) "제보 내용을 수정해주세요" else "현재 상황을 빠르게 공유해주세요",
            color = BodyGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftBlue)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, null, tint = OndaBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "이름과 학번은 다른 사용자에게 표시되지 않습니다.",
                    color = OndaBlue,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (loadingOptions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = OndaBlue,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("노선·정류장·차량 불러오는 중…", color = BodyGray, fontSize = 12.sp)
                }
            }
            loadError?.let {
                Text(it, color = WarnText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            ExpandableSelectField(
                icon = Icons.Filled.DirectionsBus,
                label = "노선 선택",
                value = routeLabel,
                expanded = routeMenuOpen,
                enabled = !loadingOptions,
                emptyHint = if (routes.isEmpty()) "불러온 노선이 없습니다" else null,
                onExpandedChange = { open ->
                    routeMenuOpen = open
                    if (open) {
                        stopMenuOpen = false
                        vehicleMenuOpen = false
                    }
                },
            ) {
                routes.forEach { route ->
                    SelectOptionRow(
                        text = buildString {
                            append(route.routeName)
                            route.direction?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
                        },
                        selected = selectedRoute?.id == route.id,
                        onClick = {
                            selectedRoute = route
                            routeMenuOpen = false
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ExpandableSelectField(
                icon = Icons.Filled.Place,
                label = "정류장 선택",
                value = stopLabel,
                expanded = stopMenuOpen,
                enabled = !loadingOptions && !stopsLoading && selectedRoute != null,
                emptyHint = when {
                    selectedRoute == null -> "노선을 먼저 선택하세요"
                    stopsLoading -> "정류장 불러오는 중…"
                    stops.isEmpty() -> "이 노선에 정류장이 없습니다"
                    else -> null
                },
                onExpandedChange = { open ->
                    stopMenuOpen = open
                    if (open) {
                        routeMenuOpen = false
                        vehicleMenuOpen = false
                    }
                },
            ) {
                stops.forEach { stop ->
                    SelectOptionRow(
                        text = stop.name,
                        selected = selectedStop?.id == stop.id,
                        onClick = {
                            selectedStop = stop
                            stopMenuOpen = false
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ExpandableSelectField(
                icon = Icons.Filled.DirectionsBus,
                label = "차량 선택 (선택사항)",
                value = vehicleLabel,
                expanded = vehicleMenuOpen,
                enabled = !loadingOptions,
                emptyHint = null,
                onExpandedChange = { open ->
                    vehicleMenuOpen = open
                    if (open) {
                        routeMenuOpen = false
                        stopMenuOpen = false
                    }
                },
            ) {
                SelectOptionRow(
                    text = "선택 안 함",
                    selected = selectedBus == null,
                    onClick = {
                        selectedBus = null
                        vehicleMenuOpen = false
                    },
                )
                if (buses.isEmpty()) {
                    Text(
                        "등록된 차량이 없습니다",
                        color = BodyGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                } else {
                    buses.forEach { bus ->
                        SelectOptionRow(
                            text = bus.displayLabel(),
                            selected = selectedBus?.id == bus.id,
                            onClick = {
                                selectedBus = bus
                                vehicleMenuOpen = false
                            },
                        )
                    }
                }
            }

            if (directionLabel.isNotBlank() && directionLabel != "-") {
                Text(
                    "방향: $directionLabel",
                    color = BodyGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("상황 선택", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                situationOptions.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { type ->
                            val selected = selectedType == type
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(78.dp)
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) OndaBlue else CardBorder,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedType = type }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    type.icon,
                                    null,
                                    tint = if (selected) OndaBlue else type.color,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    type.label,
                                    color = TitleBlack,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("추가 설명 (선택사항)", color = TitleBlack, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                placeholder = {
                    Text("추가적으로 설명이 필요한 내용을 입력해주세요.", color = BodyGray, fontSize = 13.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OndaBlue,
                    unfocusedBorderColor = CardBorder,
                ),
            )
            Text(
                "${note.length} / 200",
                color = BodyGray,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.End,
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarnBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, null, tint = WarnText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("학생 제보이며 실제 상황과 다를 수 있습니다.", color = WarnText, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            val canSubmit = selectedType != null &&
                selectedRoute != null &&
                selectedStop != null &&
                !loadingOptions
            Button(
                onClick = {
                    val type = selectedType ?: return@Button
                    val route = selectedRoute ?: return@Button
                    val stop = selectedStop ?: return@Button
                    val base = initialReport
                    onSubmit(
                        createCommunityReport(
                            type = type,
                            routeLabel = route.routeName,
                            stopName = stop.name,
                            body = note.trim(),
                            directionLabel = route.direction?.takeIf { it.isNotBlank() } ?: "-",
                            vehicleLabel = selectedBus?.displayLabel() ?: "선택 안 함",
                            id = base?.id ?: "r_${System.currentTimeMillis()}",
                            reporterCount = base?.reporterCount ?: 1,
                            likeCount = base?.likeCount ?: 0,
                            dislikeCount = base?.dislikeCount ?: 0,
                            registeredAt = base?.registeredAt ?: "방금 전",
                            isValid = base?.isValid ?: true,
                        ).copy(
                            timeLabel = if (isEdit) "수정됨" else "방금 전",
                        ),
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OndaBlue,
                    contentColor = Color.White,
                    disabledContainerColor = OndaBlue.copy(alpha = 0.4f),
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isEdit) "수정 완료" else "제보 등록하기",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExpandableSelectField(
    icon: ImageVector,
    label: String,
    value: String,
    expanded: Boolean,
    enabled: Boolean,
    emptyHint: String?,
    onExpandedChange: (Boolean) -> Unit,
    optionsContent: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (expanded) 1.5.dp else 1.dp,
                    color = if (expanded) OndaBlue else CardBorder,
                    shape = RoundedCornerShape(14.dp),
                )
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) {
                    onExpandedChange(!expanded)
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SoftBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = OndaBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = BodyGray, fontSize = 11.sp)
                Text(
                    value,
                    color = if (enabled) TitleBlack else BodyGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                null,
                tint = if (expanded) OndaBlue else BodyGray,
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
            ) {
                if (emptyHint != null) {
                    Text(
                        emptyHint,
                        color = BodyGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                } else {
                    optionsContent()
                }
            }
        }
    }
}

@Composable
private fun SelectOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) SoftBlue else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = if (selected) OndaBlue else TitleBlack,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
