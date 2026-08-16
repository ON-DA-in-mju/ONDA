package com.mju.onda.driver.feature.splash.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mju.onda.driver.R
import kotlinx.coroutines.delay

private val SplashBackground = Color.White
private val SplashSloganColor = Color(0xFF051228)

/** 학생 앱 스플래시와 동일한 Figma 비율 (400 x 845) */
private const val LogoTopFraction = 220f / 845f
private const val LogoWidthFraction = 241f / 360f
private const val SloganTopFraction = 345f / 845f
private const val IllustrationHeightFraction = 304f / 845f

/** 브랜드가 보이도록 학생 앱보다 조금 더 길게 */
private const val SplashDurationMillis = 2800L

/**
 * DRI-00-00 Splash Screen — 학생용과 동일 레이아웃.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(SplashDurationMillis)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val logoTop = with(density) { (maxHeight.toPx() * LogoTopFraction).toDp() }
            val sloganTop = with(density) { (maxHeight.toPx() * SloganTopFraction).toDp() }

            Image(
                painter = painterResource(id = R.drawable.splash_illustration),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(IllustrationHeightFraction),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.BottomCenter,
            )

            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "ON-DA",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = logoTop)
                    .fillMaxWidth(LogoWidthFraction),
                contentScale = ContentScale.FillWidth,
            )

            Text(
                text = "언제 어디서나\n셔틀버스가 ON-DA",
                color = SplashSloganColor,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = sloganTop)
                    .fillMaxWidth(LogoWidthFraction),
            )
        }
    }
}
