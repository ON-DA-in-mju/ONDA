package com.mju.onda.driver.feature.inoperation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.AppStatusBadge
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.inoperation.data.InOperationDetailStatusInfo
import com.mju.onda.driver.feature.inoperation.data.MockInOperationDetailStatus
import com.mju.onda.driver.feature.inoperation.viewmodel.InOperationDetailStatusEvent
import com.mju.onda.driver.feature.inoperation.viewmodel.InOperationDetailStatusViewModel

private val SafetyBg = Color(0xFFE8F8F7)
private val StatusIconBg = Color(0xFFE8F8EF)

@Composable
fun InOperationDetailStatusScreen(
    operationId: String,
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onEndOperation: () -> Unit = {},
    onSuspendRequest: () -> Unit = {},
    onOpenStopRoute: () -> Unit = {},
    viewModel: InOperationDetailStatusViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                InOperationDetailStatusEvent.NavigateBack -> onBack()
                InOperationDetailStatusEvent.GoHome -> onHome()
                InOperationDetailStatusEvent.EndOperation -> onEndOperation()
                InOperationDetailStatusEvent.SuspendRequest -> onSuspendRequest()
                InOperationDetailStatusEvent.OpenStopRoute -> onOpenStopRoute()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockInOperationDetailStatus.SCREEN_TITLE,
                onBack = viewModel::onBack,
                actions = {
                    IconButton(onClick = viewModel::onHome) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "홈",
                            tint = OndaColors.TextPrimary,
                        )
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
                    .padding(bottom = 24.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.in_operation_detail_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(350f / 140f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(12.dp))
                MainInfoCard(info = uiState.info)
                Spacer(modifier = Modifier.height(12.dp))
                TransmissionCard(info = uiState.info)
                Spacer(modifier = Modifier.height(12.dp))
                SafetyCard()
                Spacer(modifier = Modifier.height(20.dp))
                OndaOutlinedButton(
                    label = MockInOperationDetailStatus.STOP_ROUTE_LABEL,
                    onClick = viewModel::onOpenStopRoute,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaPrimaryButton(
                    label = MockInOperationDetailStatus.END_OPERATION_LABEL,
                    onClick = viewModel::onEndOperation,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockInOperationDetailStatus.SUSPEND_REQUEST_LABEL,
                    onClick = viewModel::onSuspendRequest,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun MainInfoCard(info: InOperationDetailStatusInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(OndaColors.PrimarySoft, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsBus,
                    contentDescription = null,
                    tint = OndaColors.Primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.routeName,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 16.sp,
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
            }
            AppStatusBadge(
                label = info.statusLabel,
                backgroundColor = OndaColors.SuccessSoft,
                foregroundColor = OndaColors.SuccessText,
                borderRadius = 999.dp,
                fontSizeSp = 11f,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            TimeStat(
                label = MockInOperationDetailStatus.LABEL_ACTUAL_START,
                value = info.actualStartTime,
                modifier = Modifier.weight(1f),
            )
            TimeStat(
                label = MockInOperationDetailStatus.LABEL_ELAPSED,
                value = info.elapsedLabel,
                modifier = Modifier.weight(1f),
            )
            TimeStat(
                label = MockInOperationDetailStatus.LABEL_EXPECTED_END,
                value = info.expectedEndTime,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        DashedDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(OndaColors.SuccessText, CircleShape),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = info.origin, style = OndaTypography.bodySmall.copy(fontSize = 13.sp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = OndaColors.TextHint,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
            )
            Icon(
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = info.destination, style = OndaTypography.bodySmall.copy(fontSize = 13.sp))
        }
    }
}

@Composable
private fun TimeStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = OndaTypography.bodySmall.copy(
                fontSize = 11.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OndaColors.Primary,
            ),
        )
    }
}

@Composable
private fun TransmissionCard(info: InOperationDetailStatusInfo) {
    val ok = info.transmissionOk
    val badgeBg = if (ok) OndaColors.SuccessSoft else Color(0xFFFFF0E8)
    val badgeFg = if (ok) OndaColors.SuccessText else Color(0xFFF07A3A)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = MockInOperationDetailStatus.SECTION_TRANSMISSION,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.weight(1f),
            )
            AppStatusBadge(
                label = info.locationBadge,
                backgroundColor = badgeBg,
                foregroundColor = badgeFg,
                borderRadius = 999.dp,
                fontSizeSp = 10f,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        StatusRow(
            icon = Icons.Rounded.Place,
            label = MockInOperationDetailStatus.LABEL_LAST_TRANSMISSION,
            value = info.lastTransmission,
            valueColor = OndaColors.TextPrimary,
            iconTint = lastTransmissionIconColor(info.lastTransmission, ok),
        )
        Spacer(modifier = Modifier.height(10.dp))
        StatusRow(
            icon = Icons.Rounded.Wifi,
            label = MockInOperationDetailStatus.LABEL_NETWORK,
            value = info.networkStatus,
            valueColor = networkStatusColor(info.networkStatus),
            iconTint = networkStatusColor(info.networkStatus),
        )
        Spacer(modifier = Modifier.height(10.dp))
        StatusRow(
            icon = Icons.Outlined.Dns,
            label = MockInOperationDetailStatus.LABEL_SERVER,
            value = info.serverStatus,
            valueColor = serverStatusColor(info.serverStatus),
            iconTint = serverStatusColor(info.serverStatus),
        )
    }
}

private fun networkStatusColor(status: String): Color =
    if (status == "연결됨") OndaColors.SuccessText else Color(0xFFE05A3C)

private fun serverStatusColor(status: String): Color = when (status) {
    "정상" -> OndaColors.SuccessText
    "대기" -> OndaColors.Warning
    else -> Color(0xFFE05A3C)
}

private fun lastTransmissionIconColor(label: String, sending: Boolean): Color = when {
    label != "없음" -> OndaColors.SuccessText
    sending -> OndaColors.Warning
    else -> Color(0xFFE05A3C)
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    iconTint: Color = OndaColors.SuccessText,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(StatusIconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
        }
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            ),
        )
    }
}

@Composable
private fun SafetyCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafetyBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = OndaColors.Accent,
                modifier = Modifier.size(28.dp),
            )
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = MockInOperationDetailStatus.SAFETY_TITLE,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MockInOperationDetailStatus.SAFETY_BODY,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun DashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = OndaColors.Border,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
        )
    }
}
