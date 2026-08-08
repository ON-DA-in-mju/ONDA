package com.mju.onda.driver.feature.settings.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.MockDriverSettings
import com.mju.onda.driver.feature.settings.data.MockSafeStopHistory
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryItem
import com.mju.onda.driver.feature.settings.data.SafeStopReviewStatus
import com.mju.onda.driver.feature.settings.viewmodel.SafeStopHistoryEvent
import com.mju.onda.driver.feature.settings.viewmodel.SafeStopHistoryViewModel

private val IconSoftBg = Color(0xFFEDF4FE)
private val SuccessText = Color(0xFF00897B)
private val ConfirmedText = Color(0xFF2BB673)

@Composable
fun SafeStopHistoryScreen(
    onBack: () -> Unit,
    onOpenNewRequest: () -> Unit,
    onOpenReceived: () -> Unit,
    onOpenApproved: () -> Unit,
    onOpenContinue: () -> Unit,
    viewModel: SafeStopHistoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SafeStopHistoryEvent.NavigateBack -> onBack()
                SafeStopHistoryEvent.OpenNewRequest -> onOpenNewRequest()
                SafeStopHistoryEvent.NotInOperation -> {
                    Toast.makeText(
                        context,
                        MockDriverSettings.NOT_IN_OPERATION_TOAST,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                SafeStopHistoryEvent.Refreshed -> {
                    Toast.makeText(
                        context,
                        MockSafeStopHistory.REFRESH_TOAST,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is SafeStopHistoryEvent.OpenReceived -> onOpenReceived()
                is SafeStopHistoryEvent.OpenApproved -> onOpenApproved()
                is SafeStopHistoryEvent.OpenContinue -> onOpenContinue()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockSafeStopHistory.SCREEN_TITLE,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
            ) {
                if (uiState.items.isEmpty()) {
                    EmptyHistory()
                } else {
                    HistoryGroupedByDate(
                        items = uiState.items,
                        onItemClick = viewModel::onItemClick,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.canCreateRequest) {
                    Button(
                        onClick = viewModel::onNewRequest,
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
                        Text(
                            text = MockSafeStopHistory.NEW_REQUEST_LABEL,
                            style = OndaTypography.labelLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OndaColors.TextOnPrimary,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedButton(
                    onClick = viewModel::onRefresh,
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
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = MockSafeStopHistory.REFRESH_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            color = OndaColors.Primary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = MockSafeStopHistory.EMPTY_TITLE,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OndaColors.TextPrimary,
                textAlign = TextAlign.Center,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockSafeStopHistory.EMPTY_SUBTITLE,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryGroupedByDate(
    items: List<SafeStopHistoryItem>,
    onItemClick: (SafeStopHistoryItem) -> Unit,
) {
    val groups = items
        .groupBy { it.dateLabel }
        .toList()
        .sortedByDescending { (date, _) -> dateSortKey(date) }

    Column(modifier = Modifier.fillMaxWidth()) {
        groups.forEachIndexed { groupIndex, (dateLabel, dayItems) ->
            if (groupIndex > 0) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = dateLabel,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.TextPrimary,
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            HistoryListCard(
                items = dayItems,
                onItemClick = onItemClick,
            )
        }
    }
}

/** "8월 7일" → 월·일로 정렬 (최신 날짜가 위) */
private fun dateSortKey(dateLabel: String): Int {
    val match = Regex("""(\d+)\s*월\s*(\d+)\s*일""").find(dateLabel) ?: return 0
    val month = match.groupValues[1].toIntOrNull() ?: 0
    val day = match.groupValues[2].toIntOrNull() ?: 0
    return month * 100 + day
}

@Composable
private fun HistoryListCard(
    items: List<SafeStopHistoryItem>,
    onItemClick: (SafeStopHistoryItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        items.forEachIndexed { index, item ->
            HistoryRow(item = item, onClick = { onItemClick(item) })
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = OndaColors.Border.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: SafeStopHistoryItem,
    onClick: () -> Unit,
) {
    val statusLabel = when (item.reviewStatus) {
        SafeStopReviewStatus.Pending -> MockSafeStopHistory.STATUS_PENDING
        SafeStopReviewStatus.Confirmed -> MockSafeStopHistory.STATUS_CONFIRMED
        SafeStopReviewStatus.ActionCompleted -> MockSafeStopHistory.STATUS_ACTION_COMPLETED
    }
    val statusColor = when (item.reviewStatus) {
        SafeStopReviewStatus.Pending -> SuccessText
        SafeStopReviewStatus.Confirmed -> ConfirmedText
        SafeStopReviewStatus.ActionCompleted -> OndaColors.TextPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DirectionsBus,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.reason,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OndaColors.TextPrimary,
                ),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${item.routeName} · ${item.vehicleName} · ${item.requestedAt}",
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
        Text(
            text = statusLabel,
            style = OndaTypography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            ),
        )
    }
}
