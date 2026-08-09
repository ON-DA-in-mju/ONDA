package com.mju.onda.driver.feature.inoperation.ui

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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.SignalCellularAlt
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
import com.mju.onda.driver.feature.inoperation.data.InOperationMinimalInfo
import com.mju.onda.driver.feature.inoperation.data.MockInOperationMinimal
import com.mju.onda.driver.feature.inoperation.viewmodel.InOperationMinimalEvent
import com.mju.onda.driver.feature.inoperation.viewmodel.InOperationMinimalViewModel

private val SafetyBg = Color(0xFFEDF4FE)
private val LocationBadgeBg = Color(0xFFE8F8EF)

@Composable
fun InOperationMinimalScreen(
    operationId: String,
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onOpenDetailStatus: () -> Unit = {},
    onEndOperation: () -> Unit = {},
    viewModel: InOperationMinimalViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                InOperationMinimalEvent.NavigateBack -> onBack()
                InOperationMinimalEvent.GoHome -> onHome()
                InOperationMinimalEvent.OpenDetailStatus -> onOpenDetailStatus()
                InOperationMinimalEvent.EndOperation -> onEndOperation()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockInOperationMinimal.SCREEN_TITLE,
                onBack = viewModel::onBack,
                actions = {
                    IconButton(onClick = viewModel::onHome) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "홈",
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
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.in_operation_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(358f / 148f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OperationSummary(info = uiState.info)
                Spacer(modifier = Modifier.height(18.dp))
                ElapsedBlock(minutes = uiState.info.elapsedMinutes)
                Spacer(modifier = Modifier.height(10.dp))
                LocationOkBadge(info = uiState.info)
                Spacer(modifier = Modifier.height(18.dp))
                MetaCard(info = uiState.info)
                Spacer(modifier = Modifier.height(12.dp))
                SafetyBanner()
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockInOperationMinimal.DETAIL_STATUS_LABEL,
                    onClick = viewModel::onOpenDetailStatus,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockInOperationMinimal.END_OPERATION_LABEL,
                    onClick = viewModel::onEndOperation,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun OperationSummary(info: InOperationMinimalInfo) {
    Text(
        text = MockInOperationMinimal.HEADLINE,
        style = OndaTypography.headlineLarge.copy(
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        ),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "${info.routeName} ${info.vehicleName}",
        style = OndaTypography.bodyLarge.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = OndaColors.TextPrimary,
        ),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(OndaColors.SuccessText, CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = info.origin,
            style = OndaTypography.bodySmall.copy(fontSize = 13.sp),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = OndaColors.TextHint,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(16.dp),
        )
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = info.destination,
            style = OndaTypography.bodySmall.copy(fontSize = 13.sp),
        )
    }
}

@Composable
private fun ElapsedBlock(minutes: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "$minutes",
            style = OndaTypography.headlineLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OndaColors.Primary,
                lineHeight = 50.sp,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = MockInOperationMinimal.ELAPSED_SUFFIX,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OndaColors.Primary,
            ),
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun LocationOkBadge(info: InOperationMinimalInfo) {
    val ok = info.transmissionOk
    val bg = if (ok) LocationBadgeBg else Color(0xFFFFF0E8)
    val fg = if (ok) OndaColors.SuccessText else Color(0xFFF07A3A)
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.SignalCellularAlt,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = info.locationStatusLabel,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            ),
        )
    }
}

@Composable
private fun MetaCard(info: InOperationMinimalInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        MetaRow(
            icon = Icons.Outlined.AccessTime,
            label = MockInOperationMinimal.LABEL_ACTUAL_START,
            value = info.actualStartTime,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OndaColors.Border.copy(alpha = 0.7f)),
        )
        MetaRow(
            icon = Icons.Rounded.Place,
            label = MockInOperationMinimal.LABEL_LAST_TRANSMISSION,
            value = info.lastTransmissionLabel,
        )
    }
}

@Composable
private fun MetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                color = OndaColors.Primary,
            ),
        )
    }
}

@Composable
private fun SafetyBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SafetyBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Lightbulb,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = MockInOperationMinimal.SAFETY_TITLE,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = MockInOperationMinimal.SAFETY_BODY,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}
