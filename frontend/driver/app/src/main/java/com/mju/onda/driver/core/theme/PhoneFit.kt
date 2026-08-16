package com.mju.onda.driver.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * 피그마(약 360dp 폭) 기준으로 좁은 폰에서는 dp/sp를 같이 줄여 글자가 잘리지 않게 한다.
 * 큰 폰에서는 키우지 않고 여백만 둔다. 사용자 접근성 글자 크기는 최대 1.15배로 제한.
 */
@Composable
fun ProvidePhoneFit(
    designWidthDp: Float = 360f,
    minScale: Float = 0.88f,
    maxScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(1)
    val layoutScale = (widthDp / designWidthDp).coerceIn(minScale, maxScale)
    val current = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = current.density * layoutScale,
            fontScale = current.fontScale.coerceIn(0.9f, 1.15f),
        ),
        content = content,
    )
}
