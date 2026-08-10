package com.onda.mju.student.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R
import com.onda.mju.student.ui.theme.ONDAStudentTheme

private val OndaBlue = Color(0xFF0041F1)
private val InfoBoxBg = Color(0xFFF0F6FD)
private val InfoPrimaryText = Color(0xFF111827)
private val InfoSecondaryText = Color(0xFF33415E)
private val FooterText = Color(0xFF6B7283)
private val SloganText = Color(0xFF111111)

/**
 * Fractions from Figma STU-00-01A (402 x 874).
 * Device chrome is omitted; positions are relative to the full screen.
 */
private const val LogoTopFraction = 122f / 874f
private const val LogoHeightFraction = 91f / 874f
private const val LogoWidthFraction = 225f / 402f
private const val SloganTopFraction = 236f / 874f
// Slightly lower than Figma raw y so the art sits closer to the login button.
private const val IllustrationTopFraction = 330f / 874f
private const val IllustrationHeightFraction = 365f / 874f
private const val SideInsetFraction = 14f / 402f
private const val BottomClusterBottomPaddingFraction = 28f / 874f
private const val ButtonToInfoGapFraction = 19f / 874f
private const val InfoToFooterGapFraction = 23f / 874f

@Composable
fun LoginStartScreen(
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            val density = LocalDensity.current
            val screenHeight = maxHeight
            val screenWidth = maxWidth

            fun fracH(fraction: Float): Dp =
                with(density) { (screenHeight.toPx() * fraction).toDp() }

            fun fracW(fraction: Float): Dp =
                with(density) { (screenWidth.toPx() * fraction).toDp() }

            val sideInset = fracW(SideInsetFraction).coerceIn(12.dp, 20.dp)
            val logoTop = fracH(LogoTopFraction)
            val logoHeight = fracH(LogoHeightFraction).coerceIn(64.dp, 96.dp)
            val sloganTop = fracH(SloganTopFraction)
            val illustrationTop = fracH(IllustrationTopFraction)
            val illustrationHeight = fracH(IllustrationHeightFraction)
                .coerceIn(240.dp, 400.dp)

            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "ON-DA",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = logoTop)
                    .fillMaxWidth(LogoWidthFraction)
                    .height(logoHeight),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = "언제 어디서나\n셔틀버스가 ON-DA",
                color = SloganText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = sloganTop)
                    .fillMaxWidth(0.7f),
            )

            // Nearly full-bleed like Figma (x≈2, width≈398 on 402).
            Image(
                painter = painterResource(id = R.drawable.login_illustration),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = illustrationTop)
                    .fillMaxWidth()
                    .height(illustrationHeight),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.BottomCenter,
            )

            // Button + info + terms anchored near the bottom (Figma ~75%–93%).
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = sideInset)
                    .padding(bottom = fracH(BottomClusterBottomPaddingFraction).coerceIn(12.dp, 28.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OndaBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "명지대학교 계정으로 로그인",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(
                    modifier = Modifier.height(
                        fracH(ButtonToInfoGapFraction).coerceIn(12.dp, 20.dp),
                    ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(InfoBoxBg)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = OndaBlue,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "명지대학교 재학생 전용 서비스입니다.",
                            color = InfoPrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                        Text(
                            text = "학교 계정 인증 후 셔틀 운행 정보를 확인할 수 있어요.",
                            color = InfoSecondaryText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(
                        fracH(InfoToFooterGapFraction).coerceIn(14.dp, 28.dp),
                    ),
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "개인정보 처리방침",
                        color = FooterText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onPrivacyPolicyClick)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "     |     ",
                        color = FooterText,
                        fontSize = 11.5.sp,
                    )
                    Text(
                        text = "서비스 이용약관",
                        color = FooterText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onTermsClick)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_7")
@Composable
private fun LoginStartScreenPreview() {
    ONDAStudentTheme {
        LoginStartScreen()
    }
}
