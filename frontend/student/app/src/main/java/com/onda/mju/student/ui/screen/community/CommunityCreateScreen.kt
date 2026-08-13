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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var route by remember(initialReport?.id) {
        mutableStateOf(initialReport?.routeLabel ?: "기흥역 통학버스")
    }
    var stop by remember(initialReport?.id) {
        mutableStateOf(initialReport?.stopName ?: "명지대역 사거리 정류장")
    }
    var vehicle by remember(initialReport?.id) {
        mutableStateOf(initialReport?.vehicleLabel ?: "2호차")
    }
    var selectedType by remember(initialReport?.id) {
        mutableStateOf(initialReport?.type)
    }
    var note by remember(initialReport?.id) {
        mutableStateOf(initialReport?.body ?: "")
    }

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
            SelectRow(Icons.Filled.DirectionsBus, "노선 선택", route)
            Spacer(modifier = Modifier.height(10.dp))
            SelectRow(Icons.Filled.Place, "정류장 선택", stop)
            Spacer(modifier = Modifier.height(10.dp))
            SelectRow(Icons.Filled.DirectionsBus, "차량 선택 (선택사항)", vehicle)

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
                                Icon(type.icon, null, tint = if (selected) OndaBlue else type.color, modifier = Modifier.size(22.dp))
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
            Button(
                onClick = {
                    val type = selectedType ?: return@Button
                    val base = initialReport
                    onSubmit(
                        createCommunityReport(
                            type = type,
                            routeLabel = route,
                            stopName = stop,
                            body = note.trim(),
                            vehicleLabel = vehicle,
                            id = base?.id ?: "r_${System.currentTimeMillis()}",
                            reporterCount = base?.reporterCount ?: 1,
                            likeCount = base?.likeCount ?: 0,
                            dislikeCount = base?.dislikeCount ?: 0,
                            registeredAt = base?.registeredAt ?: "방금 전",
                            isValid = base?.isValid ?: true,
                        ).copy(
                            directionLabel = base?.directionLabel ?: "명지대 방면",
                            timeLabel = if (isEdit) "수정됨" else "방금 전",
                        ),
                    )
                },
                enabled = selectedType != null,
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
private fun SelectRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
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
            Text(value, color = TitleBlack, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = BodyGray)
    }
}
