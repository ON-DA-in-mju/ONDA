package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
fun OndaPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 48.dp,
    cornerRadius: Dp = 12.dp,
    fontSize: TextUnit = 14.sp,
    contentHorizontalPadding: Dp = 16.dp,
    fillMaxWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height),
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = OndaColors.Primary,
            contentColor = OndaColors.TextOnPrimary,
            disabledContainerColor = OndaColors.Primary.copy(alpha = 0.45f),
            disabledContentColor = OndaColors.TextOnPrimary,
        ),
        contentPadding = PaddingValues(horizontal = contentHorizontalPadding),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = OndaColors.TextOnPrimary,
                strokeWidth = 2.4.dp,
            )
        } else {
            Text(
                text = label,
                style = OndaTypography.labelLarge.copy(fontSize = fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
