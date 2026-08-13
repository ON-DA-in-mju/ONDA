package com.mju.onda.driver.feature.vehicle.ui

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.DriverBottomNav
import com.mju.onda.driver.core.ui.components.DriverNavTab
import com.mju.onda.driver.core.ui.components.OndaLogo
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.feature.vehicle.data.MockVehicleChange
import com.mju.onda.driver.feature.vehicle.data.VehicleChangeInfo
import com.mju.onda.driver.feature.vehicle.viewmodel.VehicleChangeEvent
import com.mju.onda.driver.feature.vehicle.viewmodel.VehicleChangeViewModel

@Composable
fun VehicleChangeScreen(
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onOpenAlarms: () -> Unit,
    viewModel: VehicleChangeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                VehicleChangeEvent.ConfirmAndGoHome -> onConfirm()
                VehicleChangeEvent.NavigateBack -> onBack()
                VehicleChangeEvent.OpenAlarms -> onOpenAlarms()
                VehicleChangeEvent.OpenHistory -> {
                    Toast.makeText(context, "운행 이력 화면은 다음 단계에서 연결합니다.", Toast.LENGTH_SHORT).show()
                }
                VehicleChangeEvent.OpenSettings -> {
                    Toast.makeText(context, "설정 화면은 다음 단계에서 연결합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            VehicleHeader(
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
                VehicleSummaryCard()
                Spacer(modifier = Modifier.height(14.dp))
                VehicleCompareCard(info = uiState.info)
                Spacer(modifier = Modifier.height(14.dp))
                VehicleDetailCard(info = uiState.info)
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockVehicleChange.CONFIRM_LABEL,
                    onClick = viewModel::onConfirm,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun VehicleHeader(
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
        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
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
private fun VehicleSummaryCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.PrimarySoft, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.vehicle_change_bus_face),
            contentDescription = null,
            modifier = Modifier.size(88.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = MockVehicleChange.HEADLINE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockVehicleChange.SUBTITLE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VehicleCompareCard(info: VehicleChangeInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        VehicleCompareItem(
            label = MockVehicleChange.BEFORE_LABEL,
            labelColor = OndaColors.TextHint,
            vehicleName = info.beforeVehicle,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(22.dp),
        )
        VehicleCompareItem(
            label = MockVehicleChange.AFTER_LABEL,
            labelColor = OndaColors.Primary,
            vehicleName = info.afterVehicle,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VehicleCompareItem(
    label: String,
    labelColor: Color,
    vehicleName: String,
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = vehicleName,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        // 라벨/호차 텍스트는 Compose Text만 사용. 버스 이미지에는 글자가 포함되지 않음.
        Image(
            painter = painterResource(id = R.drawable.vehicle_change_bus_side),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(103f / 56f),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun VehicleDetailCard(info: VehicleChangeInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        InfoRow(Icons.Outlined.LocationOn, MockVehicleChange.LABEL_ROUTE, info.routeName)
        InfoRow(Icons.Outlined.DirectionsCar, MockVehicleChange.LABEL_ROUND, info.roundLabel)
        InfoRow(Icons.Outlined.AccessTime, MockVehicleChange.LABEL_SCHEDULED_TIME, info.scheduledTime)
        InfoRow(Icons.Outlined.LocationOn, MockVehicleChange.LABEL_ORIGIN, info.origin)
        InfoRow(
            icon = Icons.Outlined.LocationOn,
            label = MockVehicleChange.LABEL_DESTINATION,
            value = info.destination,
            showDivider = false,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ChangeMetaBox(info = info)
        Spacer(modifier = Modifier.height(6.dp))
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
private fun ChangeMetaBox(info: VehicleChangeInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.PrimarySoft, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFD7E7FF), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MetaLine(label = "변경 사유", value = info.changeReason)
        MetaLine(label = "변경 시간", value = info.changeTime)
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.Primary,
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
