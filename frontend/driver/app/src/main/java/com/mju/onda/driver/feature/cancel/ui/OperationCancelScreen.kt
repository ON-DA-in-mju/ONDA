package com.mju.onda.driver.feature.cancel.ui

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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Close
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
import com.mju.onda.driver.feature.cancel.data.MockOperationCancel
import com.mju.onda.driver.feature.cancel.data.OperationCancelInfo
import com.mju.onda.driver.feature.cancel.viewmodel.OperationCancelEvent
import com.mju.onda.driver.feature.cancel.viewmodel.OperationCancelViewModel

private val CancelSoft = Color(0xFFFFEBEE)
private val CancelIconBg = Color(0xFFEF5350)

@Composable
fun OperationCancelScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: OperationCancelViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OperationCancelEvent.ConfirmAndGoHome -> onConfirm()
                OperationCancelEvent.NavigateBack -> onBack()
                OperationCancelEvent.OpenAlarms -> onOpenAlarms()
                OperationCancelEvent.OpenHistory -> onOpenHistory()
                OperationCancelEvent.OpenSettings -> onOpenSettings()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            CancelHeader(
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
                CancelSummaryCard()
                Spacer(modifier = Modifier.height(14.dp))
                CancelDetailCard(info = uiState.info)
                Spacer(modifier = Modifier.height(10.dp))
                CancelMetaBox(info = uiState.info)
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockOperationCancel.CONFIRM_LABEL,
                    onClick = viewModel::onConfirm,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun CancelHeader(
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
private fun CancelSummaryCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CancelSoft, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(CancelIconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(59.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = MockOperationCancel.HEADLINE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockOperationCancel.SUBTITLE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CancelDetailCard(info: OperationCancelInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        InfoRow(Icons.Outlined.LocationOn, MockOperationCancel.LABEL_ROUTE, info.routeName)
        InfoRow(Icons.Outlined.DirectionsCar, MockOperationCancel.LABEL_VEHICLE, info.vehicleName)
        InfoRow(Icons.Outlined.AccessTime, MockOperationCancel.LABEL_ROUND, info.roundLabel)
        InfoRow(Icons.Outlined.AccessTime, MockOperationCancel.LABEL_DEPART_TIME, info.departTime)
        InfoRow(Icons.Outlined.LocationOn, MockOperationCancel.LABEL_ORIGIN, info.origin)
        InfoRow(
            icon = Icons.Outlined.Flag,
            label = MockOperationCancel.LABEL_DESTINATION,
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
private fun CancelMetaBox(info: OperationCancelInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CancelSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetaLine(
            icon = Icons.Outlined.ErrorOutline,
            label = "취소 사유",
            value = info.cancelReason,
        )
        MetaLine(
            icon = Icons.Outlined.AccessTime,
            label = "취소 시간",
            value = info.cancelTime,
        )
    }
}

@Composable
private fun MetaLine(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OndaColors.Error,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = OndaColors.Error,
                lineHeight = 18.sp,
            ),
        )
        Text(
            text = value,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 18.sp,
            ),
        )
    }
}
