package com.onda.mju.student

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.onda.mju.student.ui.theme.ONDAStudentTheme

private val SplashBackground = Color.White
private val SplashSloganColor = Color(0xFF051228)

/**
 * Fractions derived from the Figma frame (400 x 845), using the phone content
 * width (360) for horizontal sizing. Device chrome from the mockup is omitted.
 */
private const val LogoTopFraction = 220f / 845f
private const val LogoWidthFraction = 241f / 360f
private const val SloganTopFraction = 345f / 845f
private const val IllustrationHeightFraction = 304f / 845f

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    SplashContent(modifier = modifier)
}

@Composable
private fun SplashContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
                // Figma inserts blank lines between phrases; keep a close readable gap.
                lineHeight = 40.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = sloganTop)
                    .fillMaxWidth(LogoWidthFraction),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_7")
@Composable
private fun SplashScreenPreview() {
    ONDAStudentTheme {
        SplashContent()
    }
}
