package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography

@Composable
fun StatusIconRow(
    icon: ImageVector,
    label: String,
    statusText: String,
    modifier: Modifier = Modifier,
    statusColor: Color = OndaColors.Primary,
    iconBackgroundColor: Color = OndaColors.PrimarySoft,
    iconColor: Color = OndaColors.Primary,
    showDivider: Boolean = true,
    iconCircleSize: Dp = 40.dp,
    iconSize: Dp = 33.dp,
    labelStyle: TextStyle = OndaTypography.bodyLarge.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    ),
    statusStyle: TextStyle = OndaTypography.bodyLarge.copy(
        color = statusColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = if (showDivider) 12.dp else 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(iconCircleSize)
                    .background(iconBackgroundColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(iconSize),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = labelStyle,
                modifier = Modifier.weight(1f),
            )
            Text(text = statusText, style = statusStyle)
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = OndaColors.Border,
            )
        }
    }
}
