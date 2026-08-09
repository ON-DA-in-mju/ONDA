package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography

@Composable
fun OndaOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
    fillMaxWidth: Boolean = true,
    fontSize: TextUnit = 14.sp,
    contentHorizontalPadding: Dp = 12.dp,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.2.dp, OndaColors.Primary),
        contentPadding = PaddingValues(horizontal = contentHorizontalPadding),
    ) {
        Text(
            text = label,
            style = OndaTypography.labelLarge.copy(
                color = OndaColors.Primary,
                fontSize = fontSize,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
