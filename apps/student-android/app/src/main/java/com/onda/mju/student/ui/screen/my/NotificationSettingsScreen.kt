package com.onda.mju.student.ui.screen.my

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun NotificationSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    var emergency by remember { mutableStateOf(true) }
    var favoriteChange by remember { mutableStateOf(true) }
    var startAlert by remember { mutableStateOf(true) }
    var alight by remember { mutableStateOf(true) }
    var general by remember { mutableStateOf(true) }
    var service by remember { mutableStateOf(true) }

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
                "알림 설정",
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
            Text("알림 수신 설정", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
            ) {
                ToggleRow(Icons.Filled.Notifications, Color(0xFFE11D48), "긴급 공지", "운행 중단, 안전 관련 등 중요한 공지를 받습니다.", emergency) { emergency = it }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.DirectionsBus, OndaBlue, "즐겨찾기 노선 운행 변경", "즐겨찾기한 노선의 운행 변경 알림을 받습니다.", favoriteChange) { favoriteChange = it }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.PlayArrow, Color(0xFF8B5CF6), "운행 시작 알림", "셔틀버스의 운행 시작 알림을 받습니다.", startAlert) { startAlert = it }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.Place, Color(0xFF14B8A6), "하차 알림", "설정한 하차 정류장 알림을 받습니다.", alight) { alight = it }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.Campaign, Color(0xFFF59E0B), "일반 공지", "일반 공지 및 안내 사항을 받습니다.", general) { general = it }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.Info, BodyGray, "서비스 알림", "앱 업데이트, 점검 등 서비스 안내를 받습니다.", service) { service = it }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("알림 시간 설정", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("알림 허용 시간", color = TitleBlack, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("06:00 ~ 23:00", color = OndaBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BodyGray)
            }
            Text(
                "설정한 시간 외에는 알림이 울리지 않습니다.",
                color = BodyGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )

            Spacer(modifier = Modifier.height(18.dp))
            Text("기타 설정", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("진동 함께 사용", color = TitleBlack, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("항상", color = OndaBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BodyGray)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = BodyGray, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = OndaBlue),
        )
    }
}
