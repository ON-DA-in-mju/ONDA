package com.mju.onda.driver.feature.startprocessing.ui

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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.startprocessing.data.MockStartProcessing
import com.mju.onda.driver.feature.startprocessing.data.ProcessingStep
import com.mju.onda.driver.feature.startprocessing.data.ProcessingStepStatus
import com.mju.onda.driver.feature.startprocessing.viewmodel.StartProcessingEvent
import com.mju.onda.driver.feature.startprocessing.viewmodel.StartProcessingViewModel

private val HeadlineTeal = Color(0xFF1F8A8C)
private val StepLine = Color(0xFFB7E0E1)
private val ActiveCardBg = Color(0xFFEDF4FE)
private val FooterBg = Color(0xFFEFF5FF)
/** 로딩/완료 아이콘 동일 footprint */
private val StepIconSize = 26.dp

@Composable
fun StartProcessingScreen(
    onFinished: () -> Unit = {},
    viewModel: StartProcessingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StartProcessingEvent.NavigateBack -> Unit
                StartProcessingEvent.ProcessingFinished -> onFinished()
                StartProcessingEvent.ProcessingFailed -> {
                    Toast.makeText(
                        context,
                        MockStartProcessing.STEP_FAILED_TOAST,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(title = MockStartProcessing.SCREEN_TITLE)
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
                    painter = painterResource(id = R.drawable.start_processing_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(250f / 180f),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = MockStartProcessing.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineTeal,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${MockStartProcessing.SUBTITLE_LINE1}\n${MockStartProcessing.SUBTITLE_LINE2}",
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(22.dp))
                ProcessingStepList(steps = uiState.steps)
                Spacer(modifier = Modifier.height(20.dp))
                FooterInfoBanner()
            }
        }
    }
}

@Composable
private fun ProcessingStepList(steps: List<ProcessingStep>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val next = steps.getOrNull(index + 1)
            ProcessingStepRow(
                step = step,
                connectorToNext = next?.status,
                isLast = index == steps.lastIndex,
            )
        }
    }
}

@Composable
private fun ProcessingStepRow(
    step: ProcessingStep,
    connectorToNext: ProcessingStepStatus?,
    isLast: Boolean,
) {
    val isActive = step.status == ProcessingStepStatus.InProgress
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier
                        .background(ActiveCardBg, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                } else {
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                },
            ),
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
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
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
                        color = if (step.status == ProcessingStepStatus.Failed) {
                            Color(0xFFE05A3C)
                        } else {
                            OndaColors.TextSecondary
                        },
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
            val spin by rememberInfiniteTransition(label = "step-loading").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "step-loading-angle",
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FooterBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(OndaColors.PrimarySoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = MockStartProcessing.FOOTER_INFO,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
