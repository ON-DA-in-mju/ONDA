package com.mju.onda.driver.feature.home.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.mju.onda.driver.feature.home.data.AssignedOperation
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationStatus
import com.mju.onda.driver.feature.home.viewmodel.DepartureHomeAlert
import com.mju.onda.driver.feature.home.viewmodel.TodayOperationEvent
import com.mju.onda.driver.feature.home.viewmodel.TodayOperationViewModel
import com.mju.onda.driver.feature.permission.data.MockPermissionGuide

@Composable
fun TodayOperationHomeScreen(
    onOpenAlarms: () -> Unit = {},
    onOpenOperationDetail: (String) -> Unit = {},
    onOpenInOperation: (String) -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onResetToLogin: () -> Unit = {},
    viewModel: TodayOperationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 운행 시작 후 홈 복귀 시 상태 배지 동기화 (곧 출발 → 운행 중)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncRuntimeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TodayOperationEvent.ShowAlarmPending -> onOpenAlarms()
                is TodayOperationEvent.OpenDetail -> {
                    onOpenOperationDetail(event.operationId)
                }
                is TodayOperationEvent.OpenInOperation -> {
                    onOpenInOperation(event.operationId)
                }
                TodayOperationEvent.OpenHistory -> onOpenHistory()
                TodayOperationEvent.OpenSettings -> onOpenSettings()
                TodayOperationEvent.ContactAdmin -> {
                    Toast.makeText(context, MockTodayOperations.CONTACT_ADMIN_TOAST, Toast.LENGTH_SHORT).show()
                }
                TodayOperationEvent.OpenAppPermissionSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                TodayOperationEvent.ShowResetDone -> {
                    Toast.makeText(
                        context,
                        MockTodayOperations.DEMO_RESET_TOAST,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                TodayOperationEvent.NavigateToLoginAfterReset -> onResetToLogin()
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
                        DriverNavTab.Today -> Unit
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
                    .padding(bottom = 28.dp),
            ) {
                HomeHeader(
                    unreadCount = uiState.unreadAlarmCount,
                    onAlarmClick = viewModel::onAlarmClick,
                )

                if (uiState.departureAlert != DepartureHomeAlert.None) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DepartureAlertBanner(alert = uiState.departureAlert)
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                GreetingHeader()
                Spacer(modifier = Modifier.height(18.dp))

                if (uiState.hasAssignments) {
                    AssignedContent(
                        uiState = uiState,
                        onDetail = { operationId ->
                            viewModel.onDetailClick(context, operationId)
                        },
                    )
                } else {
                    EmptyAssignmentContent(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::onRefresh,
                        onContactAdmin = viewModel::onContactAdminClick,
                    )
                }

                HomeActionSection(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::onRefresh,
                    onReset = viewModel::onResetDemoState,
                )
            }
        }
    }

    if (uiState.showPermissionRequiredDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPermissionRequiredDialog,
            title = { Text(text = MockPermissionGuide.REQUIRED_DIALOG_TITLE) },
            text = { Text(text = MockPermissionGuide.REQUIRED_DIALOG_MESSAGE) },
            confirmButton = {
                TextButton(onClick = viewModel::onOpenPermissionSettings) {
                    Text(MockPermissionGuide.REQUIRED_DIALOG_SETTINGS)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPermissionRequiredDialog) {
                    Text(MockPermissionGuide.REQUIRED_DIALOG_CLOSE)
                }
            },
        )
    }
}

@Composable
private fun EmptyAssignmentContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onContactAdmin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.today_empty_illustration),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .padding(bottom = 18.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = MockTodayOperations.EMPTY_TITLE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockTodayOperations.EMPTY_SUBTITLE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = OndaColors.TextSecondary,
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OndaColors.PrimarySoft, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = MockTodayOperations.EMPTY_INFO,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = OndaColors.TextPrimary,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    androidx.compose.material3.Button(
        onClick = onRefresh,
        enabled = !isRefreshing,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OndaColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = OndaColors.Primary.copy(alpha = 0.45f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = MockTodayOperations.REFRESH_LABEL,
                style = OndaTypography.labelLarge,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.Autorenew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OndaOutlinedButton(
        label = "${MockTodayOperations.CONTACT_ADMIN_LABEL}  >",
        onClick = onContactAdmin,
        enabled = !isRefreshing,
        height = 48.dp,
    )
}

@Composable
private fun AssignedContent(
    uiState: com.mju.onda.driver.feature.home.viewmodel.TodayOperationUiState,
    onDetail: (String) -> Unit,
) {
    SummaryRow(
        assignedCount = uiState.assignedCount,
        unreadCount = uiState.unreadAlarmCount,
    )
    Spacer(modifier = Modifier.height(16.dp))

    uiState.activeOperation?.let { active ->
        CurrentInProgressSection(
            operation = active,
            onClick = { onDetail(active.id) },
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    uiState.nextOperation?.let { next ->
        NextOperationCard(
            operation = next,
            onDetail = { onDetail(next.id) },
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = MockTodayOperations.LIST_SECTION_TITLE,
            style = OndaTypography.titleLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${uiState.assignedCount}건",
            style = OndaTypography.bodyLarge.copy(
                color = OndaColors.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
    Spacer(modifier = Modifier.height(10.dp))

    uiState.operations.forEachIndexed { index, operation ->
        OperationListTile(
            operation = operation,
            statusLabel = MockTodayOperations.statusLabel(operation.status),
            onClick = { onDetail(operation.id) },
        )
        if (index != uiState.operations.lastIndex) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CurrentInProgressSection(
    operation: AssignedOperation,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = MockTodayOperations.IN_PROGRESS_SECTION_TITLE,
            style = OndaTypography.titleLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OndaColors.Primary,
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OndaColors.Surface, RoundedCornerShape(16.dp))
                .border(1.dp, OndaColors.Primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = operation.routeName,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AppStatusBadge(
                    label = MockTodayOperations.IN_PROGRESS_BADGE,
                    backgroundColor = OndaColors.SuccessSoft,
                    foregroundColor = OndaColors.SuccessText,
                    borderRadius = 8.dp,
                    fontSizeSp = 11f,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = operation.vehicleName,
                style = OndaTypography.bodyLarge.copy(
                    color = OndaColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${operation.departTime} 출발 · ${operation.origin} → ${operation.destination}",
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                    lineHeight = 18.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DepartureAlertBanner(alert: DepartureHomeAlert) {
    val (title, body) = when (alert) {
        DepartureHomeAlert.Imminent ->
            MockTodayOperations.ALERT_IMMINENT_TITLE to MockTodayOperations.ALERT_IMMINENT_BODY
        DepartureHomeAlert.Overdue ->
            MockTodayOperations.ALERT_OVERDUE_TITLE to MockTodayOperations.ALERT_OVERDUE_BODY
        DepartureHomeAlert.None -> return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.WarningSoft, RoundedCornerShape(12.dp))
            .border(1.dp, OndaColors.WarningBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (alert) {
            // DRI-01-02F: PNG에서 추출한 주황 삼각형 경고 아이콘
            DepartureHomeAlert.Overdue -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_departure_overdue_alert),
                    contentDescription = null,
                    modifier = Modifier.size(width = 34.dp, height = 28.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            // DRI-01-02E: 원형 시계 아이콘
            DepartureHomeAlert.Imminent -> {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(OndaColors.Warning, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            DepartureHomeAlert.None -> Unit
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OndaTypography.bodyLarge.copy(
                    color = OndaColors.WarningText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = OndaTypography.bodySmall.copy(
                    color = Color(0xFF302E2D),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                ),
            )
        }
    }
}

@Composable
private fun HomeActionSection(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
) {
    Spacer(modifier = Modifier.height(28.dp))
    androidx.compose.material3.Button(
        onClick = onRefresh,
        enabled = !isRefreshing,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OndaColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = OndaColors.Primary.copy(alpha = 0.45f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = MockTodayOperations.REFRESH_LABEL,
                style = OndaTypography.labelLarge,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.Autorenew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    OndaOutlinedButton(
        label = MockTodayOperations.DEMO_RESET_BUTTON,
        onClick = onReset,
        enabled = !isRefreshing,
        height = 44.dp,
    )
}

@Composable
private fun HomeHeader(
    unreadCount: Int,
    onAlarmClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = MockTodayOperations.SCREEN_TITLE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
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
            if (unreadCount > 0) {
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
private fun GreetingHeader() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val illusWidth = (maxWidth * 0.42f).coerceIn(148.dp, 176.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = MockTodayOperations.GREETING,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 27.5.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = MockTodayOperations.DATE_LABEL,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 14.sp,
                        color = OndaColors.TextSecondary,
                    ),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                painter = painterResource(id = R.drawable.today_home_illustration),
                contentDescription = null,
                modifier = Modifier.width(illusWidth),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    assignedCount: Int,
    unreadCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard(
            icon = Icons.Rounded.DirectionsBus,
            iconBackground = OndaColors.SuccessSoft,
            iconColor = OndaColors.SuccessText,
            label = MockTodayOperations.ASSIGNED_LABEL,
            value = "${assignedCount}건",
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            icon = Icons.Rounded.Notifications,
            iconBackground = OndaColors.PrimarySoft,
            iconColor = OndaColors.Primary,
            label = MockTodayOperations.UNREAD_ALARM_LABEL,
            value = "${unreadCount}건",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    iconBackground: Color,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(OndaColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = OndaTypography.labelSmall.copy(
                    color = OndaColors.TextSecondary,
                    fontSize = 12.sp,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = OndaTypography.bodyLarge.copy(
                    color = OndaColors.Primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
}

@Composable
private fun NextOperationCard(
    operation: AssignedOperation,
    onDetail: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.NextTripSoft, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFD9E6F8), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppStatusBadge(
                label = MockTodayOperations.NEXT_TRIP_BADGE,
                backgroundColor = OndaColors.Primary,
                foregroundColor = Color.White,
                isFilled = true,
            )
            Spacer(modifier = Modifier.weight(1f))
            AppStatusBadge(
                label = MockTodayOperations.statusLabel(operation.status),
                backgroundColor = when (operation.status) {
                    OperationStatus.InProgress,
                    OperationStatus.DepartingSoon,
                    OperationStatus.Waiting,
                    -> OndaColors.SuccessSoft
                    OperationStatus.Ended -> OndaColors.ErrorSoft
                    else -> OndaColors.PrimarySoft
                },
                foregroundColor = when (operation.status) {
                    OperationStatus.InProgress,
                    OperationStatus.DepartingSoon,
                    OperationStatus.Waiting,
                    -> OndaColors.SuccessText
                    OperationStatus.Ended -> OndaColors.Error
                    else -> OndaColors.Primary
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = operation.routeName,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = operation.vehicleName,
            style = OndaTypography.bodyLarge.copy(
                color = OndaColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${operation.departTime} 출발 예정",
            style = OndaTypography.bodyLarge.copy(
                color = OndaColors.Primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(OndaColors.SuccessText, CircleShape),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = operation.origin,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFB0B7C1),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
            )
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = operation.destination,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        DashedDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Autorenew,
                contentDescription = null,
                tint = OndaColors.TextSecondary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = OndaColors.TextSecondary)) {
                        append("운행 회차 ")
                    }
                    withStyle(
                        SpanStyle(
                            color = OndaColors.Primary,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    ) {
                        append(operation.round.toString())
                    }
                },
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Rounded.AccessTime,
                contentDescription = null,
                tint = OndaColors.TextSecondary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = OndaColors.TextSecondary)) {
                        append("예상 종료 ")
                    }
                    withStyle(
                        SpanStyle(
                            color = OndaColors.Primary,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    ) {
                        append(operation.expectedEndTime)
                    }
                },
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        OndaPrimaryButton(
            label = "${MockTodayOperations.DETAIL_BUTTON} >",
            onClick = onDetail,
            height = 44.dp,
            cornerRadius = 12.dp,
        )
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
            color = Color(0xFFC9D7EE),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
        )
    }
}

@Composable
private fun OperationListTile(
    operation: AssignedOperation,
    statusLabel: String,
    onClick: () -> Unit,
) {
    val badgeBg = when (operation.status) {
        OperationStatus.InProgress,
        OperationStatus.DepartingSoon,
        OperationStatus.Waiting,
        -> OndaColors.SuccessSoft
        OperationStatus.Ended -> OndaColors.ErrorSoft
        else -> OndaColors.PrimarySoft
    }
    val badgeFg = when (operation.status) {
        OperationStatus.InProgress,
        OperationStatus.DepartingSoon,
        OperationStatus.Waiting,
        -> OndaColors.SuccessText
        OperationStatus.Ended -> OndaColors.Error
        else -> OndaColors.Primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = operation.departTime,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OndaColors.Primary,
            ),
            modifier = Modifier.width(52.dp),
        )
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .width(1.dp)
                .height(32.dp)
                .background(OndaColors.Border),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operation.routeName,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.TextPrimary,
                    lineHeight = 17.5.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = operation.vehicleName,
                style = OndaTypography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OndaColors.TextSecondary,
                    lineHeight = 14.4.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AppStatusBadge(
            label = statusLabel,
            backgroundColor = badgeBg,
            foregroundColor = badgeFg,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = OndaColors.TextHint,
            modifier = Modifier.size(20.dp),
        )
    }
}
