package com.onda.mju.student.ui.screen.my

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R

private val OndaBlue = Color(0xFF0041F1)
private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE8EDF2)
private val SoftBlue = Color(0xFFEDF4FE)

/** Intrinsic ratio of bus_detail_hero.png (159×128). */
private const val MyHeaderIllustAspect = 159f / 128f

@Composable
fun MyHomeScreen(
    modifier: Modifier = Modifier,
    userName: String = "김명지",
    userEmail: String = "mju_student@mju.ac.kr",
    onAccountClick: () -> Unit = {},
    onFavoriteManageClick: () -> Unit = {},
    onNotificationSettingClick: () -> Unit = {},
    onMyReportsClick: () -> Unit = {},
    onMyPostsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 8.dp, bottom = 4.dp),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.55f),
            ) {
                Text(
                    text = "MY",
                    color = TitleBlack,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "계정과 설정을 관리하세요",
                    color = BodyGray,
                    fontSize = 13.sp,
                )
            }
            Image(
                painter = painterResource(id = R.drawable.bus_detail_hero),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(148.dp)
                    .aspectRatio(MyHeaderIllustAspect),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(SoftBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DirectionsBus, null, tint = OndaBlue, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(userName, color = TitleBlack, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(userEmail, color = BodyGray, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        ) {
            MenuRow(Icons.Filled.Person, "계정 정보", onAccountClick)
            HorizontalDivider(color = CardBorder)
            MenuRow(Icons.Filled.StarBorder, "즐겨찾기 관리", onFavoriteManageClick)
            HorizontalDivider(color = CardBorder)
            MenuRow(Icons.Filled.Notifications, "알림 설정", onNotificationSettingClick)
            HorizontalDivider(color = CardBorder)
            MenuRow(Icons.Filled.Campaign, "내 제보 내역", onMyReportsClick)
            HorizontalDivider(color = CardBorder)
            MenuRow(Icons.Filled.EditNote, "내가 쓴 글", onMyPostsClick)
            HorizontalDivider(color = CardBorder)
            MenuRow(Icons.Filled.Policy, "개인정보 처리방침", onPrivacyClick)
            HorizontalDivider(color = CardBorder)
            MenuRow(Icons.Filled.Description, "서비스 이용약관", onTermsClick)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, OndaBlue),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = OndaBlue),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("로그아웃", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = OndaBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = TitleBlack, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BodyGray)
    }
}
