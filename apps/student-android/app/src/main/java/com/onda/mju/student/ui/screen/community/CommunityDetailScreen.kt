package com.onda.mju.student.ui.screen.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)

@Composable
fun CommunityDetailScreen(
    report: CommunityReport,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
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
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                text = "제보 상세",
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
            ReportCard(report = report, onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            InfoBanner(text = "학생들의 제보입니다. 실제 상황과 다를 수 있어요.")
            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(title = "제보 내용") {
                Text(report.body, color = TitleBlack, fontSize = 14.sp, lineHeight = 22.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))

            SectionCard(title = "관련 정보") {
                InfoRow("노선", report.routeLabel)
                HorizontalDivider(color = CardBorder)
                InfoRow("방향", report.directionLabel)
                HorizontalDivider(color = CardBorder)
                InfoRow("정류장", report.stopName)
                HorizontalDivider(color = CardBorder)
                InfoRow("제보 유형", report.type.label)
                HorizontalDivider(color = CardBorder)
                InfoRow("최근 반응", "공감 ${report.likeCount} · 비공감 ${report.dislikeCount}")
            }
            Spacer(modifier = Modifier.height(12.dp))

            SectionCard(title = "참고") {
                Text(
                    "제보 내용은 실시간으로 변동될 수 있으며, 최신순 기준으로 표시됩니다.",
                    color = BodyGray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OndaBlue),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = OndaBlue,
                ),
            ) {
                Text("목록으로 돌아가기", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(title, color = TitleBlack, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = BodyGray,
            fontSize = 13.sp,
            modifier = Modifier.width(88.dp),
        )
        Text(value, color = TitleBlack, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
