package com.mju.onda.driver.feature.assignment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.DriverBottomNav
import com.mju.onda.driver.core.ui.components.DriverNavTab
import com.mju.onda.driver.core.ui.components.OndaLogo
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.feature.assignment.data.AssignmentChangeInfo
import com.mju.onda.driver.feature.assignment.data.MockAssignmentChange
import com.mju.onda.driver.feature.assignment.viewmodel.AssignmentChangeEvent
import com.mju.onda.driver.feature.assignment.viewmodel.AssignmentChangeViewModel

@Composable
fun AssignmentChangeScreen(
    onConfirm: (String) -> Unit,
    onGoHome: () -> Unit,
    onBack: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: AssignmentChangeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AssignmentChangeEvent.NavigateToVehicleChange -> onConfirm(event.operationId)
                AssignmentChangeEvent.NavigateToHome -> onGoHome()
                AssignmentChangeEvent.NavigateBack -> onBack()
                AssignmentChangeEvent.OpenAlarms -> onOpenAlarms()
                AssignmentChangeEvent.OpenHistory -> onOpenHistory()
                AssignmentChangeEvent.OpenSettings -> onOpenSettings()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        bottomBar = {
            DriverBottomNav(
                current = DriverNavTab.Today,
                onTabSelected = { tab ->
                    when (tab) {
                        DriverNavTab.Today -> viewModel.onTodayClick()
                        DriverNavTab.History -> viewModel.onHistoryClick()
                        DriverNavTab.Settings -> viewModel.onSettingsClick()
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                AssignmentHeader(
                    hasUnread = uiState.hasUnreadAlarm,
                    onBack = viewModel::onBack,
                    onAlarmClick = viewModel::onAlarmClick,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ChangeSummaryCard()
                Spacer(modifier = Modifier.height(14.dp))
                ChangedOperationCard(info = uiState.info)
                Spacer(modifier = Modifier.height(10.dp))
                ChangeMetaBox(info = uiState.info)
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockAssignmentChange.CONFIRM_LABEL,
                    onClick = viewModel::onConfirm,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun AssignmentHeader(
    hasUnread: Boolean,
    onBack: () -> Unit,
    onAlarmClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "뒤로가기",
                tint = OndaColors.TextPrimary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(6.dp)
                    .size(24.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Icon(
                    imageVector = Icons.Rounded.NotificationsNone,
                    contentDescription = "알림",
                    tint = OndaColors.TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onAlarmClick)
                        .padding(6.dp)
                        .size(24.dp),
                )
                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(7.dp)
                            .background(OndaColors.Primary, CircleShape),
                    )
                }
            }
        }
        OndaLogo(
            modifier = Modifier.align(Alignment.Center),
            height = 28.dp,
        )
    }
}

@Composable
private fun ChangeSummaryCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF8F8), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(Color(0xFFD9F3F0), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Autorenew,
                contentDescription = null,
                tint = OndaColors.Accent,
                modifier = Modifier.size(69.dp),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = MockAssignmentChange.HEADLINE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockAssignmentChange.SUBTITLE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChangedOperationCard(info: AssignmentChangeInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = MockAssignmentChange.SECTION_TITLE,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(14.dp))

        InfoRow(
            icon = Icons.Outlined.Route,
            label = MockAssignmentChange.LABEL_ROUTE,
            value = info.routeName,
        )
        InfoRow(
            icon = Icons.Outlined.DirectionsCar,
            label = MockAssignmentChange.LABEL_VEHICLE,
            value = info.vehicleName,
        )
        InfoRow(
            icon = Icons.Outlined.AccessTime,
            label = MockAssignmentChange.LABEL_ROUND,
            value = info.roundLabel,
        )
        InfoRow(
            icon = Icons.Outlined.AccessTime,
            label = MockAssignmentChange.LABEL_DEPART_TIME,
            value = info.departTime,
            showChangedBadge = info.isDepartTimeChanged,
        )
        InfoRow(
            icon = Icons.Outlined.LocationOn,
            label = MockAssignmentChange.LABEL_ORIGIN,
            value = info.origin,
        )
        InfoRow(
            icon = Icons.Outlined.LocationOn,
            label = MockAssignmentChange.LABEL_DESTINATION,
            value = info.destination,
            showDivider = false,
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    showChangedBadge: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            if (showChangedBadge) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = MockAssignmentChange.CHANGED_BADGE,
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OndaColors.TextOnPrimary,
                    ),
                    modifier = Modifier
                        .background(OndaColors.Primary, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OndaColors.Border.copy(alpha = 0.7f)),
            )
        }
    }
}

@Composable
private fun ChangeMetaBox(info: AssignmentChangeInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.PrimarySoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "변경 사유: ${info.changeReason}",
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.Primary,
                lineHeight = 18.sp,
            ),
        )
        Text(
            text = "변경 시간: ${info.changeTime}",
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.Primary,
                lineHeight = 18.sp,
            ),
        )
    }
}
