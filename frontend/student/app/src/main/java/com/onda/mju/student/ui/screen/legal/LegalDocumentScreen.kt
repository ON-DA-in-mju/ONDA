package com.onda.mju.student.ui.screen.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

enum class LegalType {
    Privacy,
    Terms,
}

@Composable
fun LegalDocumentScreen(
    type: LegalType,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val title = when (type) {
        LegalType.Privacy -> "개인정보 처리방침"
        LegalType.Terms -> "서비스 이용약관"
    }
    val body = when (type) {
        LegalType.Privacy ->
            "ON-DA는 명지대학교 자연캠퍼스 셔틀버스 통합 서비스 제공을 위해 필요한 범위에서 개인정보를 처리합니다.\n\n" +
                "1. 수집 항목: 학교 이메일, 이름, 재학생 인증 정보, 기기 알림 토큰\n" +
                "2. 이용 목적: 로그인, 알림 발송, 서비스 운영\n" +
                "3. 보관 기간: 회원 탈퇴 또는 관련 법령에 따른 기간\n\n" +
                "자세한 내용은 추후 정식 문서로 교체됩니다."
        LegalType.Terms ->
            "본 약관은 ON-DA 학생용 앱 이용과 관련된 기본 조건을 정합니다.\n\n" +
                "1. 서비스는 셔틀 운행 정보 및 커뮤니티 제보를 제공합니다.\n" +
                "2. 이용자는 허위 제보 등 서비스 방해 행위를 해서는 안 됩니다.\n" +
                "3. 실시간 정보는 교통 상황에 따라 달라질 수 있습니다.\n\n" +
                "정식 약관 문서는 추후 교체됩니다."
    }

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.Black,
                )
            }
            Text(
                text = title,
                color = Color(0xFF111827),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = title,
                color = Color(0xFF111827),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Text(
                text = body,
                color = Color(0xFF445066),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
