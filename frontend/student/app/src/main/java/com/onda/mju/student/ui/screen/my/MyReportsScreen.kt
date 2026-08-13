package com.onda.mju.student.ui.screen.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.onda.mju.student.ui.screen.community.CommunityReport
import com.onda.mju.student.ui.screen.community.ReportCard
import com.onda.mju.student.ui.screen.community.sampleCommunityReports

private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)

@Composable
fun MyReportsScreen(
    modifier: Modifier = Modifier,
    reports: List<CommunityReport> = sampleCommunityReports().take(2),
    onBackClick: () -> Unit = {},
    onReportClick: (String) -> Unit = {},
) {
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
                "내 제보 내역",
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
            Text("내가 등록한 제보 ${reports.size}건", color = BodyGray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            reports.forEach { report ->
                ReportCard(report = report, onClick = { onReportClick(report.id) })
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
