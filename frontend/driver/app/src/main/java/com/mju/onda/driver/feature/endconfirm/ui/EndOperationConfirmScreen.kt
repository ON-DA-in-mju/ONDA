package com.mju.onda.driver.feature.endconfirm.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mju.onda.driver.core.ui.components.AppStatusBadge
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.endconfirm.data.EndOperationConfirmInfo
import com.mju.onda.driver.feature.endconfirm.data.MockEndOperationConfirm
import com.mju.onda.driver.feature.endconfirm.viewmodel.EndOperationConfirmEvent
import com.mju.onda.driver.feature.endconfirm.viewmodel.EndOperationConfirmViewModel

private val WarningIconBg = Color(0xFFFFF0E8)
private val HeadlineBlue = Color(0xFF0A2A5C)

@Composable
fun EndOperationConfirmScreen(
    operationId: String,
    onBack: () -> Unit,
    onGoToProcessing: () -> Unit,
    viewModel: EndOperationConfirmViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EndOperationConfirmEvent.NavigateBack -> onBack()
                EndOperationConfirmEvent.GoToEndProcessing -> onGoToProcessing()
            }
        }
    }

    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirmDialog,
            title = { Text(text = MockEndOperationConfirm.DIALOG_TITLE) },
            text = { Text(text = MockEndOperationConfirm.DIALOG_MESSAGE) },
            confirmButton = {
                TextButton(onClick = viewModel::onDialogConfirmYes) {
                    Text(
                        text = MockEndOperationConfirm.DIALOG_CONFIRM,
                        color = OndaColors.TextPrimary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirmDialog) {
                    Text(
                        text = MockEndOperationConfirm.DIALOG_DISMISS,
                        color = OndaColors.TextPrimary,
                    )
                }
            },
        )
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockEndOperationConfirm.SCREEN_TITLE,
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
            ) {
                OperationSummaryHeader(info = uiState.info)
                Spacer(modifier = Modifier.height(20.dp))
                ConfirmCard(onConfirm = viewModel::onConfirmEnd)
            }
        }
    }
}

@Composable
private fun OperationSummaryHeader(info: EndOperationConfirmInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = info.routeName,
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeadlineBlue,
                ),
                modifier = Modifier.weight(1f),
            )
            AppStatusBadge(
                label = info.vehicleName,
                backgroundColor = OndaColors.PrimarySoft,
                foregroundColor = OndaColors.Primary,
                borderRadius = 8.dp,
                fontSizeSp = 11f,
            )
            Spacer(modifier = Modifier.width(6.dp))
            AppStatusBadge(
                label = MockEndOperationConfirm.BADGE_IN_PROGRESS,
                backgroundColor = OndaColors.SuccessSoft,
                foregroundColor = OndaColors.SuccessText,
                borderRadius = 8.dp,
                fontSizeSp = 11f,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TimeRow(
            icon = Icons.Outlined.AccessTime,
            label = MockEndOperationConfirm.LABEL_ACTUAL_START,
            value = info.actualStartTime,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .height(1.dp)
                .background(OndaColors.Border.copy(alpha = 0.7f)),
        )
        TimeRow(
            icon = Icons.Outlined.Timer,
            label = MockEndOperationConfirm.LABEL_ELAPSED,
            value = info.elapsedTime,
        )
    }
}

@Composable
private fun TimeRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
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
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun ConfirmCard(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(WarningIconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = OndaColors.Warning,
                modifier = Modifier.size(53.dp),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = MockEndOperationConfirm.CONFIRM_TITLE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HeadlineBlue,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockEndOperationConfirm.CONFIRM_BODY,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = OndaColors.TextSecondary,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        EndOperationPressButton(onClick = onConfirm)
    }
}

@Composable
private fun EndOperationPressButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "end-op-btn-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "end-op-btn-alpha",
    )

    Image(
        painter = painterResource(id = R.drawable.end_operation_button),
        contentDescription = MockEndOperationConfirm.END_LABEL,
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(248f / 282f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentScale = ContentScale.Fit,
    )
}
