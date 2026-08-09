package com.mju.onda.driver.feature.batterywarning.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.AppStatusBadge
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.batterywarning.data.BatteryDiagIcon
import com.mju.onda.driver.feature.batterywarning.data.BatteryDiagItem
import com.mju.onda.driver.feature.batterywarning.data.BatteryDiagTone
import com.mju.onda.driver.feature.batterywarning.data.BatteryWarningInfo
import com.mju.onda.driver.feature.batterywarning.data.MockBatteryWarning
import com.mju.onda.driver.feature.batterywarning.viewmodel.BatteryWarningEvent
import com.mju.onda.driver.feature.batterywarning.viewmodel.BatteryWarningViewModel

@Composable
fun BatteryWarningScreen(
    onBack: () -> Unit,
    viewModel: BatteryWarningViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                BatteryWarningEvent.NavigateBack -> onBack()
                BatteryWarningEvent.ShowStillIssues -> {
                    Toast.makeText(
                        context,
                        MockBatteryWarning.RECHECK_STILL_ISSUES,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                BatteryWarningEvent.ShowResolved -> {
                    Toast.makeText(
                        context,
                        MockBatteryWarning.RECHECK_OK,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockBatteryWarning.SCREEN_TITLE,
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
                TopWarningBanner(title = uiState.info.bannerTitle)
                Spacer(modifier = Modifier.height(14.dp))
                OperationSummaryCard(info = uiState.info)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = MockBatteryWarning.SECTION_DIAGNOSIS,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(10.dp))
                DiagnosisCard(items = uiState.info.items)
                Spacer(modifier = Modifier.height(12.dp))
                TipBanner()
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockBatteryWarning.CONFIRM_LABEL,
                    onClick = viewModel::onConfirmCharger,
                    isLoading = uiState.isConfirming,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockBatteryWarning.CLOSE_LABEL,
                    onClick = viewModel::onClose,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun TopWarningBanner(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.WarningSoft, RoundedCornerShape(14.dp))
            .border(1.dp, OndaColors.WarningBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Icon(
                imageVector = Icons.Outlined.BatteryAlert,
                contentDescription = null,
                tint = OndaColors.Warning,
                modifier = Modifier.size(40.dp),
            )
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = OndaColors.Warning,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.WarningText,
                    lineHeight = 20.sp,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MockBatteryWarning.BANNER_BODY,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun OperationSummaryCard(info: BatteryWarningInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppStatusBadge(
                label = MockBatteryWarning.BADGE_IN_OPERATION,
                backgroundColor = OndaColors.PrimarySoft,
                foregroundColor = OndaColors.Primary,
                borderRadius = 8.dp,
                fontSizeSp = 11f,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = info.routeName,
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = info.vehicleName,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(id = R.drawable.battery_warning_illustration),
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(184f / 126f),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun DiagnosisCard(items: List<BatteryDiagItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        items.forEachIndexed { index, item ->
            DiagnosisRow(item = item)
            if (index != items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OndaColors.Border.copy(alpha = 0.7f)),
                )
            }
        }
    }
}

@Composable
private fun DiagnosisRow(item: BatteryDiagItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(item.iconKind),
            contentDescription = null,
            tint = when (item.tone) {
                BatteryDiagTone.Warning -> OndaColors.TextMuted
                BatteryDiagTone.Ok -> OndaColors.Primary
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = item.label,
            style = OndaTypography.bodySmall.copy(
                fontSize = 14.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        StatusPill(item = item)
    }
}

@Composable
private fun StatusPill(item: BatteryDiagItem) {
    val bg = when (item.tone) {
        BatteryDiagTone.Warning -> OndaColors.WarningSoft
        BatteryDiagTone.Ok -> OndaColors.SuccessSoft
    }
    val fg = when (item.tone) {
        BatteryDiagTone.Warning -> OndaColors.WarningText
        BatteryDiagTone.Ok -> OndaColors.SuccessText
    }
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.value,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = when (item.tone) {
                BatteryDiagTone.Warning -> Icons.Rounded.WarningAmber
                BatteryDiagTone.Ok -> Icons.Outlined.CheckCircle
            },
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun TipBanner() {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    // 진단 ?? ?�이�?20 + gap 10 + "?�치 " ????"?? ?�작 ?�치
    val textStartOffset = remember(textMeasurer, density) {
        val prefixWidth = textMeasurer.measure(
            text = "?�치 ",
            style = TextStyle(fontSize = 14.sp),
        ).size.width
        20.dp + 10.dp + with(density) { prefixWidth.toDp() }
    }
    // 기존 28dp??1.5�?
    val iconSize = 42.dp
    // ??진단 ?? ?� ?�이�?0~20)�?"?�치 ?�송 ?�태" ?�작(30) ?�이???�이�?중심 배치
    val iconStart = ((20.dp + 30.dp) / 2) - (iconSize / 2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.WarningSoft, RoundedCornerShape(14.dp))
            .border(1.dp, OndaColors.WarningBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(textStartOffset),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = iconStart.coerceAtLeast(0.dp))
                    .size(iconSize)
                    .background(OndaColors.Warning.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = OndaColors.Warning,
                    modifier = Modifier.size(iconSize * 0.82f),
                )
            }
        }
        Text(
            text = MockBatteryWarning.TIP_BODY,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun iconFor(kind: BatteryDiagIcon): ImageVector = when (kind) {
    BatteryDiagIcon.Battery -> Icons.Outlined.BatteryAlert
    BatteryDiagIcon.Charging -> Icons.Outlined.Power
    BatteryDiagIcon.PowerSave -> Icons.Outlined.PowerSettingsNew
    BatteryDiagIcon.Location -> Icons.Rounded.Place
}
