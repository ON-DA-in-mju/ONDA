package com.mju.onda.driver.feature.alarm.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.DriverBottomNav
import com.mju.onda.driver.core.ui.components.DriverNavTab
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.alarm.data.AlarmCategory
import com.mju.onda.driver.feature.alarm.data.AlarmFilter
import com.mju.onda.driver.feature.alarm.data.MockOperationAlarms
import com.mju.onda.driver.feature.alarm.data.OperationAlarm
import com.mju.onda.driver.feature.alarm.viewmodel.OperationAlarmEvent
import com.mju.onda.driver.feature.alarm.viewmodel.OperationAlarmViewModel

@Composable
fun OperationAlarmListScreen(
    onBack: () -> Unit,
    onOpenAssignmentChange: (String) -> Unit = {},
    onOpenDepartureTimeChange: (String) -> Unit = {},
    onOpenOperationCancel: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: OperationAlarmViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                OperationAlarmEvent.NavigateBack -> onBack()
                is OperationAlarmEvent.OpenAssignmentChange -> {
                    onOpenAssignmentChange(event.alarmId)
                }
                is OperationAlarmEvent.OpenDepartureTimeChange -> {
                    onOpenDepartureTimeChange(event.alarmId)
                }
                is OperationAlarmEvent.OpenOperationCancel -> {
                    onOpenOperationCancel(event.alarmId)
                }
                is OperationAlarmEvent.OpenDetail -> {
                    Toast.makeText(
                        context,
                        "알림 상세는 다음 단계에서 연결합니다. (${event.alarmId})",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                OperationAlarmEvent.OpenHistory -> {
                    Toast.makeText(context, "운행 이력 화면은 다음 단계에서 연결합니다.", Toast.LENGTH_SHORT).show()
                }
                OperationAlarmEvent.OpenSettings -> onOpenSettings()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockOperationAlarms.SCREEN_TITLE,
                onBack = viewModel::onBack,
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "알림",
                            tint = OndaColors.Primary,
                            modifier = Modifier.size(24.dp),
                        )
                        if (uiState.hasUnread) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(7.dp)
                                    .background(OndaColors.Primary, CircleShape),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            DriverBottomNav(
                current = DriverNavTab.Today,
                onTabSelected = { tab ->
                    when (tab) {
                        DriverNavTab.Today -> onBack()
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
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                FilterChipRow(
                    selected = uiState.selectedFilter,
                    onSelected = viewModel::onFilterSelected,
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.items, key = { it.id }) { alarm ->
                        AlarmListItem(
                            alarm = alarm,
                            onClick = { viewModel.onItemClick(alarm.id) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    selected: AlarmFilter,
    onSelected: (AlarmFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MockOperationAlarms.filters.forEach { (filter, label) ->
            val isSelected = filter == selected
            Text(
                text = label,
                color = if (isSelected) Color.White else OndaColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        color = if (isSelected) OndaColors.Primary else OndaColors.Surface,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) OndaColors.Primary else OndaColors.Border,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onSelected(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AlarmListItem(
    alarm: OperationAlarm,
    onClick: () -> Unit,
) {
    val (icon, iconBg, iconTint) = alarmVisual(alarm)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        if (alarm.isUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(7.dp)
                    .background(OndaColors.Primary, CircleShape),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alarm.timeLabel,
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = OndaColors.TextHint,
                    ),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.title,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.body,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = OndaColors.TextHint,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun alarmVisual(alarm: OperationAlarm): Triple<ImageVector, Color, Color> {
    return when {
        alarm.category == AlarmCategory.OperationCancel ||
            alarm.title.contains("운행 취소") -> Triple(
            Icons.Rounded.Close,
            Color(0xFFFFEBEE),
            OndaColors.Error,
        )
        alarm.category == AlarmCategory.DepartureTimeChange ||
            alarm.title.contains("출발시간 변경") -> Triple(
            Icons.Rounded.AccessTime,
            OndaColors.PrimarySoft,
            OndaColors.Primary,
        )
        alarm.title.contains("출발시간 임박") || alarm.title.contains("출발") -> Triple(
            Icons.Rounded.Notifications,
            OndaColors.PrimarySoft,
            OndaColors.Primary,
        )
        alarm.title.contains("미시작") -> Triple(
            Icons.Rounded.DirectionsBus,
            Color(0xFFE8F8F7),
            OndaColors.Accent,
        )
        alarm.category == AlarmCategory.AssignmentChange -> Triple(
            Icons.Rounded.Autorenew,
            Color(0xFFE8F8F7),
            OndaColors.Accent,
        )
        else -> Triple(
            Icons.Rounded.VerifiedUser,
            OndaColors.PrimarySoft,
            OndaColors.Primary,
        )
    }
}
