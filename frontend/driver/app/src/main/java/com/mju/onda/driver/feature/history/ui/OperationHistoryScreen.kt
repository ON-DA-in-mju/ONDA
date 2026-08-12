package com.mju.onda.driver.feature.history.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.mju.onda.driver.core.ui.components.DriverBottomNav
import com.mju.onda.driver.core.ui.components.DriverNavTab
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.history.data.HistoryPeriodFilter
import com.mju.onda.driver.feature.history.data.HistoryRecord
import com.mju.onda.driver.feature.history.data.HistoryResultStatus
import com.mju.onda.driver.feature.history.data.MockOperationHistory
import com.mju.onda.driver.feature.history.viewmodel.OperationHistoryEvent
import com.mju.onda.driver.feature.history.viewmodel.OperationHistoryViewModel

private val SummaryBg = Color(0xFFEDF4FE)
private val PlateBg = Color(0xFFF3F4F6)
private val ActualGreen = Color(0xFF16A34A)
private val AdminOrange = Color(0xFFEA7A2F)
private val AdminOrangeSoft = Color(0xFFFFF1E6)
private val InterruptedRed = Color(0xFFDC2626)
private val InterruptedRedSoft = Color(0xFFFEECEC)

@Composable
fun OperationHistoryScreen(
    onGoToToday: () -> Unit,
    onOpenDetail: (recordId: String) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: OperationHistoryViewModel = viewModel(),
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
                OperationHistoryEvent.NavigateBack -> Unit
                OperationHistoryEvent.GoToToday -> onGoToToday()
                OperationHistoryEvent.OpenSettings -> onOpenSettings()
                OperationHistoryEvent.MaxRangeExceeded -> {
                    Toast.makeText(
                        context,
                        MockOperationHistory.PICKER_MAX_RANGE_TOAST,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                OperationHistoryEvent.NeedDateSelection -> {
                    Toast.makeText(
                        context,
                        MockOperationHistory.PICKER_NEED_DATE_TOAST,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is OperationHistoryEvent.OpenDetail -> onOpenDetail(event.recordId)
            }
        }
    }

    if (uiState.showPeriodPicker) {
        HistoryPeriodPickerDialog(
            visibleMonth = uiState.visibleMonth,
            draftStart = uiState.draftStart,
            draftEnd = uiState.draftEnd,
            draftRangeLabel = uiState.draftRangeLabel,
            onDismiss = viewModel::dismissPeriodPicker,
            onDayClick = viewModel::onDraftDayClick,
            onConfirm = viewModel::confirmPeriodPicker,
        )
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockOperationHistory.SCREEN_TITLE,
            )
        },
        bottomBar = {
            DriverBottomNav(
                current = DriverNavTab.History,
                onTabSelected = { tab ->
                    when (tab) {
                        DriverNavTab.Today -> viewModel.onTodayTab()
                        DriverNavTab.History -> Unit
                        DriverNavTab.Settings -> viewModel.onSettingsTab()
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
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    PeriodFilterRow(
                        selected = uiState.filter,
                        onSelect = viewModel::onFilterSelected,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SummaryBanner(
                        rangeLabel = uiState.rangeLabel,
                        count = uiState.count,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (uiState.records.isEmpty()) {
                    item {
                        HistoryEmptyContent(
                            onGoToToday = viewModel::onTodayTab,
                            onRefresh = viewModel::refresh,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                } else {
                    items(uiState.records, key = { it.id }) { record ->
                        HistoryCard(
                            record = record,
                            onClick = { viewModel.onRecordClick(record.id) },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        FooterInfoBanner()
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selected: HistoryPeriodFilter,
    onSelect: (HistoryPeriodFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = MockOperationHistory.FILTER_TODAY,
            selected = selected == HistoryPeriodFilter.Today,
            onClick = { onSelect(HistoryPeriodFilter.Today) },
            modifier = Modifier.weight(1f),
        )
        FilterChip(
            label = MockOperationHistory.FILTER_LAST_7,
            selected = selected == HistoryPeriodFilter.Last7Days,
            onClick = { onSelect(HistoryPeriodFilter.Last7Days) },
            modifier = Modifier.weight(1.15f),
        )
        FilterChip(
            label = MockOperationHistory.FILTER_CUSTOM,
            selected = selected == HistoryPeriodFilter.Custom,
            onClick = { onSelect(HistoryPeriodFilter.Custom) },
            modifier = Modifier.weight(1.15f),
            leadingIcon = Icons.Outlined.CalendarMonth,
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val border = if (selected) OndaColors.Primary else OndaColors.Border
    val textColor = if (selected) OndaColors.Primary else OndaColors.TextSecondary
    val bg = if (selected) OndaColors.PrimarySoft else OndaColors.Surface

    Row(
        modifier = modifier
            .height(40.dp)
            .background(bg, RoundedCornerShape(20.dp))
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = OndaTypography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryBanner(
    rangeLabel: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SummaryBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(OndaColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rangeLabel,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = MockOperationHistory.summaryText(count),
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun HistoryEmptyContent(
    onGoToToday: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.history_empty_illustration),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(280f / 200f),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = MockOperationHistory.EMPTY_TITLE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockOperationHistory.EMPTY_SUBTITLE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
                lineHeight = 18.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SummaryBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MockOperationHistory.EMPTY_INFO_TITLE,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = MockOperationHistory.EMPTY_INFO_BODY,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = OndaColors.TextSecondary,
                        lineHeight = 17.sp,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onGoToToday,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OndaColors.Primary,
                contentColor = OndaColors.TextOnPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = MockOperationHistory.EMPTY_GO_TODAY,
                style = OndaTypography.labelLarge.copy(
                    fontSize = 15.sp,
                    color = OndaColors.TextOnPrimary,
                ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, OndaColors.Primary),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = MockOperationHistory.EMPTY_REFRESH,
                style = OndaTypography.labelLarge.copy(
                    fontSize = 15.sp,
                    color = OndaColors.Primary,
                ),
            )
        }
    }
}

@Composable
private fun HistoryCard(
    record: HistoryRecord,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(OndaColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1) 날짜
        Box(
            modifier = Modifier
                .width(58.dp)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = record.dateLabel,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OndaColors.TextSecondary,
                    lineHeight = 16.sp,
                ),
                textAlign = TextAlign.Center,
            )
        }

        CardVerticalDivider()

        // 2) 노선·차량
        Column(
            modifier = Modifier
                .weight(1.15f)
                .padding(horizontal = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(OndaColors.PrimarySoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsBus,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.routeName,
                        style = OndaTypography.bodyLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = record.vehicleName,
                        style = OndaTypography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = OndaColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // 배지 좌측 패딩만큼 당겨서 「차」를 노선/호차 첫 글자와 X 정렬
                    Text(
                        text = record.plateNumber,
                        style = OndaTypography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            color = OndaColors.TextSecondary,
                        ),
                        modifier = Modifier
                            .offset(x = (-6).dp)
                            .background(PlateBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }

        CardVerticalDivider()

        // 3) 실제 출발 / 운행 시간
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = MockOperationHistory.LABEL_ACTUAL,
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = OndaColors.TextHint,
                        lineHeight = 12.sp,
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = MockOperationHistory.LABEL_DURATION,
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = OndaColors.TextHint,
                        lineHeight = 12.sp,
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = record.actualDepart,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OndaColors.Primary,
                        lineHeight = 16.sp,
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = record.durationLabel,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (record.status == HistoryResultStatus.Interrupted) {
                            InterruptedRed
                        } else {
                            ActualGreen
                        },
                        lineHeight = 16.sp,
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = null,
                    tint = OndaColors.TextHint,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = record.timeRange,
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = OndaColors.TextSecondary,
                        lineHeight = 13.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        CardVerticalDivider()

        // 4) 상태 + 화살표
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusBadge(status = record.status)
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = OndaColors.TextHint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CardVerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(OndaColors.Border),
    )
}

@Composable
private fun StatusBadge(status: HistoryResultStatus) {
    val (bg, fg, icon) = when (status) {
        HistoryResultStatus.Completed -> Triple(
            OndaColors.SuccessSoft,
            OndaColors.SuccessText,
            Icons.Outlined.Check,
        )
        HistoryResultStatus.AdminEnded -> Triple(
            AdminOrangeSoft,
            AdminOrange,
            Icons.Outlined.Person,
        )
        HistoryResultStatus.Interrupted -> Triple(
            InterruptedRedSoft,
            InterruptedRed,
            Icons.Outlined.Close,
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(fg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = MockOperationHistory.statusLabel(status),
            style = OndaTypography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            ),
            modifier = Modifier
                .background(bg, RoundedCornerShape(8.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun FooterInfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SummaryBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = MockOperationHistory.FOOTER_INFO,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
