package com.mju.onda.driver.feature.startconfirm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Autorenew
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.AppStatusBadge
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.startconfirm.data.MockStartConfirm
import com.mju.onda.driver.feature.startconfirm.viewmodel.StartConfirmEvent
import com.mju.onda.driver.feature.startconfirm.viewmodel.StartConfirmUiState
import com.mju.onda.driver.feature.startconfirm.viewmodel.StartConfirmViewModel

@Composable
fun StartConfirmScreen(
    onBack: () -> Unit,
    onConfirmStart: () -> Unit = {},
    viewModel: StartConfirmViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StartConfirmEvent.NavigateBack -> onBack()
                StartConfirmEvent.ConfirmStart -> onConfirmStart()
            }
        }
    }

    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirmDialog,
            title = { Text(text = MockStartConfirm.DIALOG_TITLE) },
            text = { Text(text = MockStartConfirm.DIALOG_MESSAGE) },
            confirmButton = {
                TextButton(onClick = viewModel::onDialogConfirmYes) {
                    Text(
                        text = MockStartConfirm.DIALOG_CONFIRM,
                        color = OndaColors.TextPrimary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirmDialog) {
                    Text(
                        text = MockStartConfirm.DIALOG_DISMISS,
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
                title = MockStartConfirm.SCREEN_TITLE,
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
                OperationSummaryHeader(uiState = uiState)
                Spacer(modifier = Modifier.height(8.dp))
                OperationInfoRows(uiState = uiState)
                Spacer(modifier = Modifier.height(20.dp))
                StartConfirmCard(onConfirmStart = viewModel::onConfirmStart)
            }
        }
    }
}

@Composable
private fun OperationSummaryHeader(uiState: StartConfirmUiState) {
    val info = uiState.info
    Column(modifier = Modifier.fillMaxWidth()) {
        AppStatusBadge(
            label = uiState.statusLabel,
            backgroundColor = OndaColors.PrimarySoft,
            foregroundColor = OndaColors.Primary,
            borderRadius = 8.dp,
            fontSizeSp = 11f,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = info.routeName,
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            AppStatusBadge(
                label = info.vehicleName,
                backgroundColor = OndaColors.PrimarySoft,
                foregroundColor = OndaColors.Primary,
                borderRadius = 8.dp,
                fontSizeSp = 11f,
            )
        }
    }
}

@Composable
private fun OperationInfoRows(uiState: StartConfirmUiState) {
    val info = uiState.info
    val rows = listOf(
        Triple(Icons.Rounded.Autorenew, MockStartConfirm.LABEL_ROUND, uiState.roundLabel),
        Triple(Icons.Outlined.AccessTime, MockStartConfirm.LABEL_DEPART_TIME, info.departTime),
        Triple(Icons.Outlined.LocationOn, MockStartConfirm.LABEL_ORIGIN, info.origin),
        Triple(Icons.Outlined.LocationOn, MockStartConfirm.LABEL_DESTINATION, info.destination),
        Triple(Icons.Outlined.Schedule, MockStartConfirm.LABEL_DURATION, uiState.durationLabel),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (icon, label, value) ->
            InfoRow(icon = icon, label = label, value = value)
            if (index != rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OndaColors.Border.copy(alpha = 0.75f)),
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
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
private fun StartConfirmCard(onConfirmStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 에셋 안에서 종 stroke 중심이 오른쪽으로 ~4.6% 치우쳐 있어
        // 아래 운행 시작 버튼(버스 아이콘) 중심과 세로로 맞추기 위해 보정
        Image(
            painter = painterResource(id = R.drawable.start_confirm_bell),
            contentDescription = null,
            modifier = Modifier
                .height(128.dp)
                .offset(x = (-12).dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = MockStartConfirm.CONFIRM_TITLE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = buildAnnotatedString {
                append(MockStartConfirm.CONFIRM_BODY_LINE1)
                append('\n')
                append(MockStartConfirm.CONFIRM_BODY_LINE2)
                append('\n')
                withStyle(
                    SpanStyle(
                        color = OndaColors.Primary,
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    append(MockStartConfirm.CONFIRM_BODY_HIGHLIGHT)
                }
                append(MockStartConfirm.CONFIRM_BODY_LINE3_SUFFIX)
            },
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = OndaColors.TextSecondary,
                textAlign = TextAlign.Center,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        StartOperationButton(onClick = onConfirmStart)
    }
}

@Composable
private fun StartOperationButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Image(
        painter = painterResource(id = R.drawable.start_confirm_button),
        contentDescription = MockStartConfirm.START_LABEL,
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(402f / 271f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentScale = ContentScale.Fit,
    )
}
