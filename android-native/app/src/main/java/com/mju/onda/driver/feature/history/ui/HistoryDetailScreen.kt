package com.mju.onda.driver.feature.history.ui

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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.history.data.HistoryDetailInfo
import com.mju.onda.driver.feature.history.data.HistoryResultStatus
import com.mju.onda.driver.feature.history.data.MockHistoryDetail
import com.mju.onda.driver.feature.history.data.MockOperationHistory
import com.mju.onda.driver.feature.history.viewmodel.HistoryDetailEvent
import com.mju.onda.driver.feature.history.viewmodel.HistoryDetailViewModel

private val AdminOrange = Color(0xFFEA7A2F)
private val AdminOrangeSoft = Color(0xFFFFF1E6)
private val InfoBannerBg = Color(0xFFEDF4FE)

@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    onGoToTodayHome: () -> Unit,
    viewModel: HistoryDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HistoryDetailEvent.NavigateBack -> onBack()
                HistoryDetailEvent.GoToTodayHome -> onGoToTodayHome()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockHistoryDetail.SCREEN_TITLE,
                onBack = viewModel::onBack,
                actions = {
                    IconButton(onClick = viewModel::onHome) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = MockHistoryDetail.HOME_CD,
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
                    .padding(bottom = 28.dp),
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Image(
                    painter = painterResource(id = R.drawable.operation_detail_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 163f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(14.dp))
                SummaryCard(detail = uiState.detail)
                Spacer(modifier = Modifier.height(12.dp))
                DetailRowsCard(detail = uiState.detail)
                Spacer(modifier = Modifier.height(14.dp))
                InfoBanner()
            }
        }
    }
}

@Composable
private fun SummaryCard(detail: HistoryDetailInfo) {
    val (badgeBg, badgeFg) = statusColors(detail.status)
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
                    .size(44.dp)
                    .background(OndaColors.PrimarySoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsBus,
                    contentDescription = null,
                    tint = OndaColors.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.routeName,
                    style = OndaTypography.titleLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail.vehicleName,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = OndaColors.TextSecondary,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = MockOperationHistory.statusLabel(detail.status),
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = OndaColors.Border)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryHalfCell(
                label = MockHistoryDetail.LABEL_DATE,
                value = detail.dateDisplay,
                valueColor = OndaColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(OndaColors.Border),
            )
            SummaryHalfCell(
                label = MockHistoryDetail.LABEL_STATUS,
                value = MockOperationHistory.statusLabel(detail.status),
                valueColor = badgeFg,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryHalfCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = OndaTypography.labelSmall.copy(
                fontSize = 12.sp,
                color = OndaColors.TextSecondary,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetailRowsCard(detail: HistoryDetailInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        DetailRow(
            icon = Icons.Outlined.AccessTime,
            label = MockHistoryDetail.LABEL_SCHEDULED,
            value = detail.scheduledDepart,
            valueColor = OndaColors.Primary,
        )
        DetailRow(
            icon = Icons.Outlined.PlayArrow,
            label = MockHistoryDetail.LABEL_ACTUAL_START,
            value = detail.actualStart,
            valueColor = OndaColors.Primary,
        )
        DetailRow(
            icon = Icons.Outlined.Stop,
            label = MockHistoryDetail.LABEL_ACTUAL_END,
            value = detail.actualEnd,
            valueColor = OndaColors.Primary,
        )
        DetailRow(
            icon = Icons.Outlined.Timer,
            label = MockHistoryDetail.LABEL_TOTAL,
            value = detail.totalDuration,
            valueColor = OndaColors.Primary,
        )
        DetailRow(
            icon = Icons.Outlined.Place,
            label = MockHistoryDetail.LABEL_ORIGIN,
            value = detail.origin,
        )
        DetailRow(
            icon = Icons.Outlined.Place,
            label = MockHistoryDetail.LABEL_DEST,
            value = detail.destination,
        )
        DetailRow(
            icon = Icons.Rounded.NearMe,
            label = MockHistoryDetail.LABEL_LOCATION_TX,
            value = detail.locationTxStatus,
            valueColor = OndaColors.Primary,
        )
        DetailRow(
            icon = Icons.Outlined.Shield,
            label = MockHistoryDetail.LABEL_FINAL,
            value = detail.finalStatusLabel,
            valueColor = OndaColors.Primary,
            showDivider = false,
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = OndaColors.TextPrimary,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(OndaColors.PrimarySoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OndaColors.Primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = OndaTypography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = OndaColors.TextPrimary,
                ),
            )
            Text(
                text = value,
                style = OndaTypography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                ),
            )
        }
        if (showDivider) {
            HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun InfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoBannerBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = MockHistoryDetail.INFO_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.sp,
                color = OndaColors.TextSecondary,
                lineHeight = 17.sp,
            ),
        )
    }
}

private fun statusColors(status: HistoryResultStatus): Pair<Color, Color> = when (status) {
    HistoryResultStatus.Completed -> OndaColors.SuccessSoft to OndaColors.SuccessText
    HistoryResultStatus.AdminEnded -> AdminOrangeSoft to AdminOrange
    HistoryResultStatus.Interrupted -> OndaColors.ErrorSoft to OndaColors.Error
}
