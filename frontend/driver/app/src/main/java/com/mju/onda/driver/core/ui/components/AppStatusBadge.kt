package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppStatusBadge(
    label: String,
    backgroundColor: Color,
    foregroundColor: Color,
    modifier: Modifier = Modifier,
    isFilled: Boolean = false,
    borderRadius: Dp? = null,
    fontSizeSp: Float? = null,
) {
    val radius = borderRadius ?: if (isFilled) 6.dp else 20.dp
    val fontSize = fontSizeSp ?: if (isFilled) 11f else 12f
    val horizontal = if (isFilled) 8.dp else 10.dp
    val vertical = if (isFilled) 4.dp else 5.dp

    Text(
        text = label,
        color = foregroundColor,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = (fontSize * 1.2f).sp,
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(radius))
            .padding(horizontal = horizontal, vertical = vertical),
    )
}
