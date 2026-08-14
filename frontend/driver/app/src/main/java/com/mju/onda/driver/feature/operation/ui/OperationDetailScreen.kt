package com.mju.onda.driver.feature.operation.ui

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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Place
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.AppStatusBadge
import com.mju.onda.driver.core.ui.components.DriverBottomNav
import com.mju.onda.driver.core.ui.components.DriverNavTab
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.home.data.OperationStatus
import com.mju.onda.driver.feature.operation.data.MockOperationDetail
import com.mju.onda.driver.feature.operation.data.OperationDetailInfo
import com.mju.onda.driver.feature.operation.viewmodel.OperationDetailEvent
import com.mju.onda.driver.feature.operation.viewmodel.OperationDetailViewModel

/** 홈 화면과 동일한 상태 배지 색 */
@Composable
private fun statusBadgeColors(status: OperationStatus): Pair<Color, Color> = when (status) {
    OperationStatus.Waiting,
    OperationStatus.DepartingSoon,
    -> OndaColors.SuccessSoft to OndaColors.SuccessText
    OperationStatus.Ended,
    OperationStatus.Unavailable,
    -> OndaColors.ErrorSoft to OndaColors.Error
    OperationStatus.InProgress,
    OperationStatus.Scheduled,
    -> OndaColors.PrimarySoft to OndaColors.Primary
}
@Composable
fun OperationDetailScreen(
    operationId: String,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onOpenPreCheck: (operationId: String) -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onContactAdmin: () -> Unit = {},
    viewModel: OperationDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OperationDetailEvent.NavigateBack -> onBack()
                OperationDetailEvent.GoHome -> onGoHome()
                OperationDetailEvent.PrepareConfirmed -> onOpenPreCheck(uiState.info.id)
                OperationDetailEvent.ContactAdmin -> onContactAdmin()
                OperationDetailEvent.OpenHistory -> onOpenHistory()
                OperationDetailEvent.OpenSettings -> onOpenSettings()
            }
        }
    }

    if (uiState.showConfirmDialog) {
        AssignmentConfirmDialog(
            info = uiState.info,
            onDismiss = viewModel::dismissConfirmDialog,
            onInfoDifferent = viewModel::onConfirmInfoDifferent,
            onConfirm = viewModel::onConfirmInfoOk,
        )
    }

    Scaffold(
        containerColor = Color(0xFFF7F9FC),
        topBar = {
            OndaTopBar(
                title = MockOperationDetail.SCREEN_TITLE,
                onBack = viewModel::onBack,
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
                    .padding(bottom = 24.dp),
            ) {
                // 헤더(뒤로가기·제목) 아래 전체 너비 일러스트. 아래 항목은 스크롤로 이어짐.
                Image(
                    painter = painterResource(id = R.drawable.operation_detail_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 163f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailMainCard(info = uiState.info)
                Spacer(modifier = Modifier.height(12.dp))
                DetailInfoTable(info = uiState.info)
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockOperationDetail.PREPARE_LABEL,
                    onClick = viewModel::onPrepare,
                    enabled = uiState.prepareEnabled,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockOperationDetail.CONTACT_ADMIN_LABEL,
                    onClick = viewModel::onContactAdmin,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun AssignmentConfirmDialog(
    info: OperationDetailInfo,
    onDismiss: () -> Unit,
    onInfoDifferent: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OndaColors.Surface, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(OndaColors.PrimarySoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier.size(46.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = MockOperationDetail.CONFIRM_TITLE,
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = MockOperationDetail.CONFIRM_SUBTITLE,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OndaColors.Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, OndaColors.Border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = info.routeName,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = info.vehicleName,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${info.departTime} 출발 예정",
                        style = OndaTypography.bodyLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.Primary,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(OndaColors.SuccessText, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = info.origin,
                        style = OndaTypography.bodySmall.copy(fontSize = 13.sp),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = OndaColors.TextHint,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(16.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = info.destination,
                        style = OndaTypography.bodySmall.copy(fontSize = 13.sp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                val (badgeBg, badgeFg) = statusBadgeColors(info.status)
                AppStatusBadge(
                    label = info.statusLabel,
                    backgroundColor = badgeBg,
                    foregroundColor = badgeFg,
                    borderRadius = 8.dp,
                    fontSizeSp = 11f,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = OndaColors.TextHint,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = MockOperationDetail.CONFIRM_FOOTER,
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        color = OndaColors.TextHint,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OndaOutlinedButton(
                    label = MockOperationDetail.CONFIRM_DIFF_LABEL,
                    onClick = onInfoDifferent,
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                    fillMaxWidth = false,
                    fontSize = 12.sp,
                    contentHorizontalPadding = 6.dp,
                )
                OndaPrimaryButton(
                    label = MockOperationDetail.CONFIRM_OK_LABEL,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                    fillMaxWidth = false,
                    fontSize = 12.sp,
                    contentHorizontalPadding = 6.dp,
                )
            }
        }
    }
}

@Composable
private fun DetailMainCard(info: OperationDetailInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppStatusBadge(
                label = MockOperationDetail.ASSIGNED_BADGE,
                backgroundColor = OndaColors.PrimarySoft,
                foregroundColor = OndaColors.Primary,
                borderRadius = 8.dp,
                fontSizeSp = 11f,
            )
            val (badgeBg, badgeFg) = statusBadgeColors(info.status)
            AppStatusBadge(
                label = info.statusLabel,
                backgroundColor = badgeBg,
                foregroundColor = badgeFg,
                borderRadius = 8.dp,
                fontSizeSp = 11f,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = info.routeName,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${info.vehicleName} | 운행 회차 ${info.round}",
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${info.departTime} 출발 예정",
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.Primary,
                ),
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .width(1.dp)
                    .height(12.dp)
                    .background(OndaColors.Border),
            )
            Text(
                text = "예상 종료 ${info.expectedEndTime}",
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextPrimary,
                ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(OndaColors.SuccessText, CircleShape),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = info.origin,
                style = OndaTypography.bodySmall.copy(fontSize = 13.sp),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = OndaColors.TextHint,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(16.dp),
            )
            Icon(
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = info.destination,
                style = OndaTypography.bodySmall.copy(fontSize = 13.sp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = OndaColors.TextMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = info.dateLabel,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OndaColors.SurfaceMuted, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.TextMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = MockOperationDetail.ADMIN_NOTE,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun DetailInfoTable(info: OperationDetailInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        DetailRow(
            icon = Icons.Outlined.DirectionsBus,
            iconTint = OndaColors.SuccessText,
            label = MockOperationDetail.LABEL_ORIGIN_STOP,
            value = info.origin,
        )
        DetailRow(
            icon = Icons.Outlined.LocationOn,
            iconTint = OndaColors.Primary,
            label = MockOperationDetail.LABEL_DEST_STOP,
            value = info.destination,
        )
        DetailRow(
            icon = Icons.Outlined.DirectionsBus,
            iconTint = OndaColors.SuccessText,
            label = MockOperationDetail.LABEL_VEHICLE,
            value = info.vehicleName,
        )
        DetailRow(
            icon = Icons.Outlined.Notifications,
            iconTint = OndaColors.Primary,
            label = MockOperationDetail.LABEL_STATUS,
            valueContent = {
                val (badgeBg, badgeFg) = statusBadgeColors(info.status)
                AppStatusBadge(
                    label = info.statusLabel,
                    backgroundColor = badgeBg,
                    foregroundColor = badgeFg,
                    borderRadius = 8.dp,
                    fontSizeSp = 11f,
                )
            },
            showDivider = false,
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String? = null,
    valueContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (valueContent != null) {
                valueContent()
            } else if (value != null) {
                Text(
                    text = value,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
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
