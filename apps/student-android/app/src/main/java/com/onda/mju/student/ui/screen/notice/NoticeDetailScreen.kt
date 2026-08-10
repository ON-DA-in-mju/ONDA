package com.onda.mju.student.ui.screen.notice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)
private val UrgentBg = Color(0xFFFFF1F2)
private val UrgentRed = Color(0xFFE11D48)

@Composable
fun NoticeDetailScreen(
    item: NoticeItem,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onAttachmentClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                "공지사항",
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(onClick = onNotificationClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = OndaBlue)
            }
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
                    .background(UrgentBg, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(72.dp)
                        .background(UrgentRed, RoundedCornerShape(999.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, null, tint = UrgentRed, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.badge.label, color = UrgentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.title, color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(item.datetime.replace(" / ", " · "), color = BodyGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val highlight = "오늘 18시 이후 모든 셔틀 운행을 종료합니다."
            Text(
                text = buildAnnotatedString {
                    val body = item.body
                    val idx = body.indexOf(highlight)
                    if (idx >= 0) {
                        append(body.substring(0, idx))
                        withStyle(SpanStyle(color = UrgentRed, fontWeight = FontWeight.Bold)) {
                            append(highlight)
                        }
                        append(body.substring(idx + highlight.length))
                    } else {
                        append(body)
                    }
                },
                color = TitleBlack,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )

            if (item.relatedRoutes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))
                Text("관련 노선", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.relatedRoutes.forEach { route ->
                        Row(
                            modifier = Modifier
                                .background(SoftBlue, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.DirectionsBus, null, tint = OndaBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(route, color = OndaBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (item.attachmentName != null) {
                Spacer(modifier = Modifier.height(22.dp))
                Text("첨부 안내", color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PictureAsPdf, null, tint = OndaBlue, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.attachmentName, color = TitleBlack, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.attachmentMeta.orEmpty(), color = BodyGray, fontSize = 11.sp)
                    }
                    IconButton(onClick = onAttachmentClick) {
                        Icon(Icons.Filled.Download, null, tint = OndaBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OndaBlue),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = OndaBlue),
            ) {
                Text("< 목록으로 돌아가기", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
