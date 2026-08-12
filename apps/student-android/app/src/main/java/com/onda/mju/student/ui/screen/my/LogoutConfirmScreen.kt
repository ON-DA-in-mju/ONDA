package com.onda.mju.student.ui.screen.my

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
private val PageBg = Color(0xFFF5F7FA)
private val SoftBlue = Color(0xFFEDF4FE)

@Composable
fun LogoutConfirmScreen(
    modifier: Modifier = Modifier,
    userName: String = "이다윤",
    userEmail: String = "mju_student@mju.ac.kr",
    autoLoginLabel: String = "사용 안 함",
    onBackClick: () -> Unit = {},
    onConfirmLogout: () -> Unit = {},
    onCancelClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp),
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                "로그아웃",
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
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(id = R.drawable.settings_header_illustration),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(SoftBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = OndaBlue,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "로그아웃하시겠습니까?",
                    color = TitleBlack,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "현재 계정에서 로그아웃되며,\n다음 이용시 다시 로그인해야 합니다.",
                    color = BodyGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                LogoutInfoRow(Icons.Filled.Person, "학생명", userName)
                HorizontalDivider(color = CardBorder)
                LogoutInfoRow(Icons.Filled.Email, "학교 이메일", userEmail)
                HorizontalDivider(color = CardBorder)
                LogoutInfoRow(Icons.Filled.Lock, "자동 로그인", autoLoginLabel)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onConfirmLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OndaBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text("로그아웃", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, OndaBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OndaBlue),
            ) {
                Text("취소", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LogoutInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = OndaBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = BodyGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
