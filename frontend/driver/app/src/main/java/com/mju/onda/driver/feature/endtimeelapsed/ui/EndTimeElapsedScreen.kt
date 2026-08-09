package com.mju.onda.driver.feature.endtimeelapsed.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Route
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.endtimeelapsed.data.EndTimeElapsedInfo
import com.mju.onda.driver.feature.endtimeelapsed.data.MockEndTimeElapsed
import com.mju.onda.driver.feature.endtimeelapsed.viewmodel.EndTimeElapsedEvent
import com.mju.onda.driver.feature.endtimeelapsed.viewmodel.EndTimeElapsedViewModel

private val FooterBg = Color(0xFFEDF4FE)
private val HeadlineDark = Color(0xFF0A2A5C)
private val ClockCircleBg = Color(0xFFFFF0E8)
private val ClockOrange = Color(0xFFFF8A3D)

@Composable
fun EndTimeElapsedScreen(
    operationId: String,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onEndOperation: () -> Unit,
    viewModel: EndTimeElapsedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EndTimeElapsedEvent.NavigateBack -> onBack()
                EndTimeElapsedEvent.ContinueDriving -> onContinue()
                EndTimeElapsedEvent.EndOperation -> onEndOperation()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockEndTimeElapsed.SCREEN_TITLE,
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
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                ElapsedClockIcon(
                    modifier = Modifier.fillMaxWidth(0.42f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = MockEndTimeElapsed.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineDark,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = MockEndTimeElapsed.SUBHEAD,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OndaColors.TextPrimary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = MockEndTimeElapsed.BODY,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(22.dp))
                InfoList(info = uiState.info)
                Spacer(modifier = Modifier.height(22.dp))
                OndaPrimaryButton(
                    label = MockEndTimeElapsed.CONTINUE_LABEL,
                    onClick = viewModel::onContinue,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockEndTimeElapsed.END_LABEL,
                    onClick = viewModel::onEnd,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                FooterInfoBanner()
            }
        }
    }
}

@Composable
private fun ElapsedClockIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClockCircleBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = ClockOrange,
                modifier = Modifier.fillMaxSize(0.82f),
            )
        }
    }
}

@Composable
private fun InfoList(info: EndTimeElapsedInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        InfoRow(Icons.Outlined.Route, MockEndTimeElapsed.LABEL_ROUTE, info.routeName)
        InfoRow(Icons.Outlined.DirectionsCar, MockEndTimeElapsed.LABEL_VEHICLE, info.vehicleName)
        InfoRow(Icons.Outlined.AccessTime, MockEndTimeElapsed.LABEL_SCHEDULED_END, info.scheduledEnd)
        InfoRow(Icons.Outlined.AccessTime, MockEndTimeElapsed.LABEL_CURRENT, info.currentTime)
        InfoRow(
            Icons.Outlined.AccessTime,
            MockEndTimeElapsed.LABEL_OVERTIME,
            info.overtimeLabel,
            valueBold = true,
        )
        InfoRow(
            Icons.AutoMirrored.Outlined.Logout,
            MockEndTimeElapsed.LABEL_LAST_TX,
            info.lastTransmission,
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueBold: Boolean = false,
) {
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
            modifier = Modifier.size(20.dp),
        )
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
                fontWeight = if (valueBold) FontWeight.Bold else FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun FooterInfoBanner() {
    val iconSize = 42.dp
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
            text = MockEndTimeElapsed.FOOTER_INFO,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
