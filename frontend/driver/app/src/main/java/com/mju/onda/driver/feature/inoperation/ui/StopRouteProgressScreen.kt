package com.mju.onda.driver.feature.inoperation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.inoperation.data.RouteStop
import com.mju.onda.driver.feature.inoperation.data.StopProgressPhase
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgress
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgressState
import com.mju.onda.driver.feature.inoperation.viewmodel.StopRouteProgressEvent
import com.mju.onda.driver.feature.inoperation.viewmodel.StopRouteProgressViewModel

private val TimelineLine = Color(0xFFD5DEEA)

@Composable
fun StopRouteProgressScreen(
    operationId: String,
    onBack: () -> Unit,
    viewModel: StopRouteProgressViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StopRouteProgressEvent.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = "정류장 노선 보기",
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
                    .padding(bottom = 28.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.stop_route_progress_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(350f / 140f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(12.dp))
                RouteHeaderCard(progress = uiState.progress)
                Spacer(modifier = Modifier.height(12.dp))
                FocusProgressCard(progress = uiState.progress)
                Spacer(modifier = Modifier.height(12.dp))
                FullTimelineCard(progress = uiState.progress)
                Spacer(modifier = Modifier.height(12.dp))
                SafetyTipCard()
            }
        }
    }
}

@Composable
private fun RouteHeaderCard(progress: StopRouteProgressState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(OndaColors.PrimarySoft, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsBus,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = progress.routeName.ifBlank { "노선" },
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OndaColors.TextPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = progress.vehicleName.ifBlank { "차량" },
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (progress.hasGps) "GPS 연동" else "GPS 대기",
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress.hasGps) OndaColors.SuccessText else OndaColors.WarningText,
                ),
            )
            progress.distanceToCurrentMeters?.let { meters ->
                Text(
                    text = if (meters < 1000) "${meters}m" else String.format("%.1fkm", meters / 1000.0),
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = OndaColors.TextHint,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FocusProgressCard(progress: StopRouteProgressState) {
    val previous = StopRouteProgress.previousName(progress)
    val current = StopRouteProgress.currentName(progress) ?: "정류장 없음"
    val next = StopRouteProgress.nextName(progress)
    val phase = StopRouteProgress.phaseLabel(progress)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = phase,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.Primary,
            ),
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (previous != null) {
            FocusSideStop(name = previous)
            TimelineConnector(tall = false)
        }

        Text(
            text = current,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OndaColors.TextPrimary,
                lineHeight = 34.sp,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (next != null) {
            TimelineConnector(tall = false)
            FocusSideStop(name = next)
        } else if (progress.phase == StopProgressPhase.Arrived) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "이 노선의 마지막 정류장입니다",
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun FocusSideStop(name: String) {
    Text(
        text = name,
        style = OndaTypography.bodyMedium.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = OndaColors.TextMuted,
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(0.92f),
    )
}

@Composable
private fun TimelineConnector(tall: Boolean) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .width(2.dp)
            .height(if (tall) 28.dp else 18.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(TimelineLine),
    )
}

@Composable
private fun FullTimelineCard(progress: StopRouteProgressState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "전체 정류장",
            style = OndaTypography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OndaColors.TextPrimary,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "정류장 120m 진입 후 140m 이상 벗어나면 지나간 것으로 표시됩니다",
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (progress.stops.isEmpty()) {
            Text(
                text = "이 노선에 등록된 정류장이 없습니다",
                style = OndaTypography.bodySmall.copy(color = OndaColors.TextMuted),
            )
        } else {
            progress.stops.forEachIndexed { index, stop ->
                TimelineStopRow(
                    stop = stop,
                    index = index,
                    currentIndex = progress.currentIndex,
                    lastPassedIndex = progress.lastPassedIndex,
                    isLast = index == progress.stops.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun TimelineStopRow(
    stop: RouteStop,
    index: Int,
    currentIndex: Int,
    lastPassedIndex: Int,
    isLast: Boolean,
) {
    val isPassed = index <= lastPassedIndex
    val isCurrent = !isPassed && index == currentIndex
    val nodeSize = 20.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .size(nodeSize)
                    .background(
                        color = when {
                            isCurrent -> OndaColors.Primary
                            isPassed -> OndaColors.Success
                            else -> OndaColors.SurfaceMuted
                        },
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isCurrent -> OndaColors.Primary
                            isPassed -> OndaColors.Success
                            else -> TimelineLine
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isPassed -> Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                    isCurrent -> Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape),
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            if (isPassed || isCurrent) OndaColors.Primary.copy(alpha = 0.35f)
                            else TimelineLine,
                        ),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
                .padding(top = 1.dp),
        ) {
            Text(
                text = stop.name,
                style = OndaTypography.bodyMedium.copy(
                    fontSize = if (isCurrent) 16.sp else 14.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = when {
                        isCurrent -> OndaColors.Primary
                        isPassed -> OndaColors.TextSecondary
                        else -> OndaColors.TextMuted
                    },
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SafetyTipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.AccentSoft, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "안내",
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OndaColors.Accent,
            ),
        )
        Text(
            text = "운전 중에는 화면을 조작하지 마세요. 정류장 정보는 GPS로 자동 갱신됩니다.",
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
                lineHeight = 18.sp,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
