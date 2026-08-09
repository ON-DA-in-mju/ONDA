package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography

@Composable
fun InfoIconCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = OndaColors.AccentSoft,
    iconColor: Color = OndaColors.Accent,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBackgroundColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = description,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                    color = Color(0xFF8A93A0),
                ),
            )
        }
    }
}
