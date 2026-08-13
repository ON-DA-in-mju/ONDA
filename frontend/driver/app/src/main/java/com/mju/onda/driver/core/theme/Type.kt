package com.mju.onda.driver.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val CaptionColor = Color(0xFFABB0BA)

val OndaTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = OndaColors.TextPrimary,
        lineHeight = 28.8.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = OndaColors.TextPrimary,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = OndaColors.TextPrimary,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = OndaColors.TextSecondary,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = OndaColors.Subtitle,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp,
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = OndaColors.TextOnPrimary,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = OndaColors.Primary,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = CaptionColor,
        lineHeight = 15.6.sp,
    ),
)
