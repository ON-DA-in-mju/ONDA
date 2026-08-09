package com.mju.onda.driver.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mju.onda.driver.core.theme.OndaColors

enum class DriverNavTab {
    Today,
    History,
    Settings,
}

@Composable
fun DriverBottomNav(
    current: DriverNavTab,
    onTabSelected: (DriverNavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = OndaColors.Border, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp),
        ) {
            NavItem(
                icon = Icons.Rounded.DirectionsBus,
                label = "오늘의 운행",
                selected = current == DriverNavTab.Today,
                onClick = { onTabSelected(DriverNavTab.Today) },
                modifier = Modifier.weight(1f),
            )
            NavItem(
                icon = Icons.Rounded.AccessTime,
                label = "운행 이력",
                selected = current == DriverNavTab.History,
                onClick = { onTabSelected(DriverNavTab.History) },
                modifier = Modifier.weight(1f),
            )
            NavItem(
                icon = Icons.Rounded.Settings,
                label = "설정",
                selected = current == DriverNavTab.Settings,
                onClick = { onTabSelected(DriverNavTab.Settings) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) OndaColors.Primary else OndaColors.TextMuted

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
