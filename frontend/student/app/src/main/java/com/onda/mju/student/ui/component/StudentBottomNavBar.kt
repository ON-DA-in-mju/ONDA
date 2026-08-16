package com.onda.mju.student.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.ui.theme.ONDAStudentTheme

/**
 * Shared bottom navigation for the student app.
 *
 * Fixed visual rules (reuse on every main tab screen):
 * - Icon size: 22.dp
 * - Label: 11.sp / Medium, no extra font padding (한글 하단이 잘리지 않게)
 * - Icon ↔ label gap: 3.dp
 * - Active: [StudentBottomNavColors.Active]
 * - Inactive: [StudentBottomNavColors.Inactive]
 * - Tabs (fixed order): 홈 / 노선 / 커뮤니티 / 공지 / MY
 */
object StudentBottomNavColors {
    val Active = Color(0xFF0041F1)
    val Inactive = Color(0xFF9AA3B2)
    val Divider = Color(0xFFE8EDF2)
    val Background = Color.White
}

enum class StudentBottomTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        label = "홈",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Route(
        label = "노선",
        selectedIcon = Icons.Filled.DirectionsBus,
        unselectedIcon = Icons.Outlined.DirectionsBus,
    ),
    Community(
        label = "커뮤니티",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
    ),
    Notice(
        label = "공지",
        selectedIcon = Icons.Filled.Campaign,
        unselectedIcon = Icons.Outlined.Campaign,
    ),
    My(
        label = "MY",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
}

@Composable
fun StudentBottomNavBar(
    selectedTab: StudentBottomTab,
    onTabSelected: (StudentBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudentBottomNavColors.Background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = StudentBottomNavColors.Divider,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudentBottomTab.entries.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: StudentBottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) {
        StudentBottomNavColors.Active
    } else {
        StudentBottomNavColors.Inactive
    }
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentBottomNavBarPreview() {
    ONDAStudentTheme {
        StudentBottomNavBar(
            selectedTab = StudentBottomTab.Home,
            onTabSelected = {},
        )
    }
}

@Preview(showBackground = true, device = "spec:width=320dp,height=640dp,dpi=320")
@Composable
private fun StudentBottomNavBarCompactPreview() {
    ONDAStudentTheme {
        StudentBottomNavBar(
            selectedTab = StudentBottomTab.Community,
            onTabSelected = {},
        )
    }
}

