package com.mju.onda.driver.feature.endprocessing.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.mju.onda.driver.feature.endprocessing.data.MockEndProcessing
import com.mju.onda.driver.feature.endprocessing.viewmodel.EndProcessingEvent
import com.mju.onda.driver.feature.endprocessing.viewmodel.EndProcessingViewModel
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.startprocessing.data.ProcessingStep
import com.mju.onda.driver.feature.startprocessing.data.ProcessingStepStatus

private val HeadlineBlue = Color(0xFF0A2A5C)
private val StepLine = Color(0xFF7EB6FF)
private val FooterBg = Color(0xFFEDF4FE)
private val StepIconSize = 26.dp

@Composable
fun EndProcessingScreen(
    operationId: String = "",
    onFinished: () -> Unit = {},
    screenTitle: String = MockEndProcessing.SCREEN_TITLE,
    headline: String = MockEndProcessing.HEADLINE,
    viewModel: EndProcessingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        val id = operationId.ifBlank {
            OperationRuntimeStateHolder.activeOperationId().orEmpty()
        }
        viewModel.start(id)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EndProcessingEvent.ProcessingFinished -> onFinished()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(title = screenTitle)
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
                    painter = painterResource(id = R.drawable.end_processing_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(296f / 232f),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = headline,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineBlue,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = MockEndProcessing.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(22.dp))
                EndProcessingStepList(steps = uiState.steps)
                Spacer(modifier = Modifier.height(20.dp))
                FooterInfoBanner()
            }
        }
    }
}

@Composable
private fun EndProcessingStepList(steps: List<ProcessingStep>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val next = steps.getOrNull(index + 1)
            EndProcessingStepRow(
                step = step,
                connectorToNext = next?.status,
                isLast = index == steps.lastIndex,
            )
        }
    }
}

@Composable
private fun EndProcessingStepRow(
    step: ProcessingStep,
    connectorToNext: ProcessingStepStatus?,
    isLast: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp),
        ) {
            StepIcon(status = step.status)
            if (!isLast) {
                Spacer(modifier = Modifier.height(4.dp))
                StepConnector(nextStatus = connectorToNext)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = step.title,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (step.status) {
                        ProcessingStepStatus.Pending -> OndaColors.TextMuted
                        ProcessingStepStatus.Failed -> Color(0xFFE05A3C)
                        else -> OndaColors.TextPrimary
                    },
                ),
            )
            step.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = OndaColors.TextSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StepIcon(status: ProcessingStepStatus) {
    when (status) {
        ProcessingStepStatus.Completed -> {
            Image(
                painter = painterResource(id = R.drawable.start_processing_done),
                contentDescription = null,
                modifier = Modifier.size(StepIconSize),
                contentScale = ContentScale.Fit,
            )
        }
        ProcessingStepStatus.InProgress -> {
            val spin by rememberInfiniteTransition(label = "end-step-loading").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "end-step-loading-angle",
            )
            Image(
                painter = painterResource(id = R.drawable.start_processing_loading),
                contentDescription = null,
                modifier = Modifier
                    .size(StepIconSize)
                    .rotate(spin),
                contentScale = ContentScale.Fit,
            )
        }
        ProcessingStepStatus.Pending -> {
            Box(
                modifier = Modifier
                    .size(StepIconSize)
                    .border(1.5.dp, OndaColors.Border, CircleShape)
                    .background(Color.White, CircleShape),
            )
        }
        ProcessingStepStatus.Failed -> {
            Box(
                modifier = Modifier
                    .size(StepIconSize)
                    .background(Color(0xFFFFE8E0), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE05A3C),
                    ),
                )
            }
        }
    }
}

@Composable
private fun StepConnector(nextStatus: ProcessingStepStatus?) {
    val solid = nextStatus == ProcessingStepStatus.Completed ||
        nextStatus == ProcessingStepStatus.InProgress
    val color = if (solid) StepLine else OndaColors.Border
    Canvas(
        modifier = Modifier
            .width(2.dp)
            .height(28.dp),
    ) {
        if (solid) {
            drawLine(
                color = color,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = size.width,
            )
        } else {
            drawLine(
                color = color,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = size.width,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
            )
        }
    }
}

@Composable
private fun FooterInfoBanner() {
    // 2�??�내 블록 ?�이????1/3
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
            text = MockEndProcessing.FOOTER_INFO,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
