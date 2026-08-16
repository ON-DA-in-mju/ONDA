package com.mju.onda.driver.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Place
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
import com.mju.onda.driver.feature.settings.data.InterruptedEndSummary
import com.mju.onda.driver.feature.settings.data.MockInterruptedEndComplete
import com.mju.onda.driver.feature.settings.viewmodel.InterruptedEndCompleteEvent
import com.mju.onda.driver.feature.settings.viewmodel.InterruptedEndCompleteViewModel

private val Teal = Color(0xFF2BB673)
private val HeadlineTeal = Color(0xFF1F9D8A)
private val SubtitleGray = Color(0xFF6B7A90)
private val RowIconSize = 22.dp

@Composable
fun InterruptedEndCompleteScreen(
    operationId: String = "",
    onBack: () -> Unit,
    onGoToToday: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: InterruptedEndCompleteViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                InterruptedEndCompleteEvent.NavigateBack -> onBack()
                InterruptedEndCompleteEvent.GoToToday -> onGoToToday()
                InterruptedEndCompleteEvent.OpenHistory -> onOpenHistory()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockInterruptedEndComplete.SCREEN_TITLE,
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
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.interrupted_end_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = MockInterruptedEndComplete.HEADLINE,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineTeal,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = MockInterruptedEndComplete.SUBTITLE,
                    modifier = Modifier.padding(horizontal = 28.dp),
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.sp,
                        color = SubtitleGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(18.dp))

                SummaryCard(
                    summary = uiState.summary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::onGoToToday,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
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
                        text = MockInterruptedEndComplete.GO_TODAY_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.TextOnPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onOpenHistory,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockInterruptedEndComplete.HISTORY_LABEL,
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
private fun SummaryCard(
    summary: InterruptedEndSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        SummaryRow(Icons.Outlined.Place, MockInterruptedEndComplete.LABEL_ROUTE, summary.routeName)
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(Icons.Outlined.DirectionsCar, MockInterruptedEndComplete.LABEL_VEHICLE, summary.vehicleName)
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(Icons.Outlined.WarningAmber, MockInterruptedEndComplete.LABEL_REASON, summary.reason)
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(Icons.Outlined.AccessTime, MockInterruptedEndComplete.LABEL_ACTUAL_START, summary.actualStart)
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(Icons.Outlined.AccessTime, MockInterruptedEndComplete.LABEL_INTERRUPTED_AT, summary.interruptedAt)
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(Icons.Outlined.AccessTime, MockInterruptedEndComplete.LABEL_TOTAL, summary.totalDuration)
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Teal,
            modifier = Modifier.size(RowIconSize),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        Text(
            text = value,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.TextPrimary,
            ),
        )
    }
}
