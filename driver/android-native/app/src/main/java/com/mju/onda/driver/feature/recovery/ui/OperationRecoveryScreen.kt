package com.mju.onda.driver.feature.recovery.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.AppStatusBadge
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.recovery.data.MockOperationRecovery
import com.mju.onda.driver.feature.recovery.data.OperationRecoveryInfo
import com.mju.onda.driver.feature.recovery.viewmodel.OperationRecoveryEvent
import com.mju.onda.driver.feature.recovery.viewmodel.OperationRecoveryViewModel

private val InfoBannerBg = Color(0xFFF3F7FD)
private val HeadlineBlue = Color(0xFF0A2A5C)

@Composable
fun OperationRecoveryScreen(
    onBack: () -> Unit,
    onGoToOperation: (String) -> Unit,
    onGoToToday: () -> Unit,
    onOpenBackgroundGuide: () -> Unit = {},
    onOpenBatteryWarning: () -> Unit = {},
    viewModel: OperationRecoveryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val info = uiState.info
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBannerVisibility()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OperationRecoveryEvent.NavigateBack -> onBack()
                is OperationRecoveryEvent.GoToOperation -> onGoToOperation(event.operationId)
                OperationRecoveryEvent.GoToTodayOperation -> onGoToToday()
                OperationRecoveryEvent.OpenBackgroundGuide -> onOpenBackgroundGuide()
                OperationRecoveryEvent.OpenBatteryWarning -> onOpenBatteryWarning()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockOperationRecovery.SCREEN_TITLE,
                onBack = viewModel::onBack,
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
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.operation_recovery_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(350f / 145f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = MockOperationRecovery.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineBlue,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = MockOperationRecovery.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                if (info != null) {
                    RecoveryInfoCard(info = info)
                }
                if (uiState.showBackgroundInfoBanner) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BackgroundInfoBanner(onClick = viewModel::onBackgroundInfoClick)
                }
                if (uiState.showBatteryWarningBanner) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BatteryWarningBanner(onClick = viewModel::onBatteryWarningClick)
                }
                Spacer(modifier = Modifier.height(24.dp))
                OndaPrimaryButton(
                    label = MockOperationRecovery.GO_OPERATION_LABEL,
                    onClick = viewModel::onGoToOperation,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockOperationRecovery.GO_TODAY_LABEL,
                    onClick = viewModel::onGoToToday,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun RecoveryInfoCard(info: OperationRecoveryInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppStatusBadge(
                label = MockOperationRecovery.BADGE_IN_PROGRESS,
                backgroundColor = OndaColors.SuccessSoft,
                foregroundColor = OndaColors.SuccessText,
                borderRadius = 999.dp,
                fontSizeSp = 11f,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.SignalCellularAlt,
                contentDescription = null,
                tint = if (info.transmissionOk) OndaColors.SuccessText else Color(0xFFF07A3A),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = info.transmissionLabel,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (info.transmissionOk) OndaColors.SuccessText else Color(0xFFF07A3A),
                ),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = info.routeName,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = info.vehicleName,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        RecoveryRow(
            icon = Icons.Outlined.AccessTime,
            label = MockOperationRecovery.LABEL_ACTUAL_START,
            value = info.actualStartTime,
        )
        DividerLine()
        RecoveryRow(
            icon = Icons.Outlined.NearMe,
            label = MockOperationRecovery.LABEL_LAST_TRANSMISSION,
            value = info.lastTransmission,
        )
        DividerLine()
        RecoveryRow(
            icon = Icons.Rounded.Place,
            label = MockOperationRecovery.LABEL_ROUTE_SECTION,
            value = info.routeSection,
        )
    }
}

@Composable
private fun RecoveryRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OndaColors.Border.copy(alpha = 0.7f)),
    )
}

@Composable
private fun BackgroundInfoBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoBannerBg, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(OndaColors.PrimarySoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = MockOperationRecovery.INFO_TITLE,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MockOperationRecovery.INFO_BODY,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(id = R.drawable.operation_recovery_phone),
            contentDescription = null,
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(91f / 69f),
            contentScale = ContentScale.Fit,
        )
    }
}

/** DRI-01-03H 배터리 및 충전 경고 진입 블록 */
@Composable
private fun BatteryWarningBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.WarningSoft, RoundedCornerShape(14.dp))
            .border(1.dp, OndaColors.WarningBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.BottomStart) {
            Icon(
                imageVector = Icons.Outlined.BatteryAlert,
                contentDescription = null,
                tint = OndaColors.Warning,
                modifier = Modifier.size(36.dp),
            )
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = OndaColors.Warning,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = MockOperationRecovery.BATTERY_WARNING_TITLE,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.WarningText,
                    lineHeight = 18.sp,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MockOperationRecovery.BATTERY_WARNING_BODY,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}
