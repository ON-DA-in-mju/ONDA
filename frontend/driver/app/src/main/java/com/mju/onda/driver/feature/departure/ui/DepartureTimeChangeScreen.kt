package com.mju.onda.driver.feature.departure.ui

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.AccessTime
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
import androidx.compose.ui.platform.LocalContext
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
import com.mju.onda.driver.feature.departure.data.DepartureTimeChangeInfo
import com.mju.onda.driver.feature.departure.data.MockDepartureTimeChange
import com.mju.onda.driver.feature.departure.viewmodel.DepartureTimeChangeEvent
import com.mju.onda.driver.feature.departure.viewmodel.DepartureTimeChangeViewModel

@Composable
fun DepartureTimeChangeScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onOpenAlarms: () -> Unit,
    viewModel: DepartureTimeChangeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DepartureTimeChangeEvent.ConfirmAndGoHome -> onConfirm()
                DepartureTimeChangeEvent.NavigateBack -> onBack()
                DepartureTimeChangeEvent.OpenAlarms -> onOpenAlarms()
                DepartureTimeChangeEvent.OpenHistory -> {
                    Toast.makeText(context, "운행 이력 화면은 다음 단계에서 연결합니다.", Toast.LENGTH_SHORT).show()
                }
                DepartureTimeChangeEvent.OpenSettings -> {
                    Toast.makeText(context, "설정 화면은 다음 단계에서 연결합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            DepartureHeader(
                hasUnread = uiState.hasUnreadAlarm,
                onBack = viewModel::onBack,
                onAlarmClick = viewModel::onAlarmClick,
            )
        },
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
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 24.dp),
            ) {
                DepartureSummaryCard()
                Spacer(modifier = Modifier.height(14.dp))
                TimeCompareCard(info = uiState.info)
                Spacer(modifier = Modifier.height(14.dp))
                DepartureDetailCard(info = uiState.info)
                Spacer(modifier = Modifier.height(10.dp))
                ChangeMetaBox(info = uiState.info)
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockDepartureTimeChange.CONFIRM_LABEL,
                    onClick = viewModel::onConfirm,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun DepartureHeader(
    hasUnread: Boolean,
    onBack: () -> Unit,
    onAlarmClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
            .height(52.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "뒤로가기",
            tint = OndaColors.TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack)
                .padding(6.dp)
                .size(24.dp),
        )
        OndaLogo(
            modifier = Modifier.align(Alignment.Center),
            height = 30.dp,
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(
                imageVector = Icons.Rounded.NotificationsNone,
                contentDescription = "운행 알림",
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
}

@Composable
private fun DepartureSummaryCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.PrimarySoft, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFD7E7FF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AccessTime,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(59.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = MockDepartureTimeChange.HEADLINE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockDepartureTimeChange.SUBTITLE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimeCompareCard(info: DepartureTimeChangeInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TimeCompareItem(
            label = MockDepartureTimeChange.BEFORE_LABEL,
            labelColor = OndaColors.TextHint,
            time = info.beforeTime,
            timeColor = OndaColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = OndaColors.TextHint,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(22.dp),
        )
        TimeCompareItem(
            label = MockDepartureTimeChange.AFTER_LABEL,
            labelColor = OndaColors.Primary,
            time = info.afterTime,
            timeColor = OndaColors.Primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimeCompareItem(
    label: String,
    labelColor: Color,
    time: String,
    timeColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = OndaTypography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = time,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = timeColor,
            ),
        )
    }
}

@Composable
private fun DepartureDetailCard(info: DepartureTimeChangeInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        InfoRow(Icons.Outlined.LocationOn, MockDepartureTimeChange.LABEL_ROUTE, info.routeName)
        InfoRow(Icons.Outlined.DirectionsBus, MockDepartureTimeChange.LABEL_VEHICLE, info.vehicleName)
        InfoRow(Icons.Outlined.AccessTime, MockDepartureTimeChange.LABEL_ROUND, info.roundLabel)
        InfoRow(Icons.Outlined.LocationOn, MockDepartureTimeChange.LABEL_ORIGIN, info.origin)
        InfoRow(
            icon = Icons.Outlined.LocationOn,
            label = MockDepartureTimeChange.LABEL_DESTINATION,
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
private fun ChangeMetaBox(info: DepartureTimeChangeInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.PrimarySoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
}
