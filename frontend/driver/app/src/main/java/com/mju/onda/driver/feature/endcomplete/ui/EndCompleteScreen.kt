package com.mju.onda.driver.feature.endcomplete.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.endcomplete.data.EndCompleteSummary
import com.mju.onda.driver.feature.endcomplete.data.MockEndComplete
import com.mju.onda.driver.feature.endcomplete.viewmodel.EndCompleteEvent
import com.mju.onda.driver.feature.endcomplete.viewmodel.EndCompleteViewModel

private val HeadlineBlue = Color(0xFF065DFE)
private val FooterBg = Color(0xFFEDF4FE)
private val IconCircleBg = Color(0xFFEDF4FE)

@Composable
fun EndCompleteScreen(
    operationId: String,
    onGoToToday: () -> Unit,
    onOpenHistory: () -> Unit = {},
    viewModel: EndCompleteViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EndCompleteEvent.GoToTodayOperation -> onGoToToday()
                EndCompleteEvent.OpenHistory -> onOpenHistory()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(title = MockEndComplete.SCREEN_TITLE)
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
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.end_complete_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .aspectRatio(210f / 185f),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = MockEndComplete.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineBlue,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = MockEndComplete.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                SummaryCard(summary = uiState.summary)
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockEndComplete.GO_TODAY_LABEL,
                    onClick = viewModel::onGoToToday,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockEndComplete.HISTORY_LABEL,
                    onClick = viewModel::onHistory,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                FooterInfoBanner()
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: EndCompleteSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        SummaryRow(Icons.Rounded.Place, MockEndComplete.LABEL_ROUTE, summary.routeName)
        SummaryDivider()
        SummaryRow(Icons.Outlined.DirectionsBus, MockEndComplete.LABEL_VEHICLE, summary.vehicleName)
        SummaryDivider()
        SummaryRow(Icons.Outlined.Timer, MockEndComplete.LABEL_SCHEDULED, summary.scheduledDepart)
        SummaryDivider()
        SummaryRow(Icons.Outlined.Timer, MockEndComplete.LABEL_ACTUAL_START, summary.actualStart)
        SummaryDivider()
        SummaryRow(Icons.Outlined.Timer, MockEndComplete.LABEL_ACTUAL_END, summary.actualEnd)
        SummaryDivider()
        SummaryRow(Icons.Outlined.Timer, MockEndComplete.LABEL_TOTAL, summary.totalDuration)
    }
}

@Composable
private fun SummaryRow(
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
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(IconCircleBg, CircleShape),
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
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OndaColors.Border.copy(alpha = 0.7f)),
    )
}

@Composable
private fun FooterInfoBanner() {
    val iconSize = 30.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FooterBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(OndaColors.PrimarySoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(iconSize * 0.82f),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = MockEndComplete.FOOTER_INFO,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
