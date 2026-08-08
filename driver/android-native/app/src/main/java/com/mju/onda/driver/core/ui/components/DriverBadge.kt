package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mju.onda.driver.core.theme.OndaColors

@Composable
fun DriverBadge(
    label: String = "기사님용",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(OndaColors.DriverBadgeBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = OndaColors.DriverBadgeFg,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            color = OndaColors.DriverBadgeFg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
