package com.mju.onda.driver.feature.startcomplete.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
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
import com.mju.onda.driver.core.ui.components.AppStatusBadge
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.startcomplete.data.MockStartComplete
import com.mju.onda.driver.feature.startcomplete.data.StartCompleteInfo
import com.mju.onda.driver.feature.startcomplete.viewmodel.StartCompleteEvent
import com.mju.onda.driver.feature.startcomplete.viewmodel.StartCompleteViewModel

private val HeadlineTeal = Color(0xFF1F8A8C)
private val StatusBarBg = Color(0xFFE8F8F7)
private val StatusBadgeBg = Color(0xFFDDF7F1)

@Composable
fun StartCompleteScreen(
    onGoToOperation: () -> Unit = {},
    viewModel: StartCompleteViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) { /* 뒤로가기 불가 — 운행 화면으로만 진행 */ }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StartCompleteEvent.GoToOperation -> onGoToOperation()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockStartComplete.SCREEN_TITLE,
                onBack = null,
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
                Image(
                    painter = painterResource(id = R.drawable.start_complete_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(305f / 192f),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = MockStartComplete.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineTeal,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = MockStartComplete.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                OperationInfoCard(info = uiState.info)
                Spacer(modifier = Modifier.height(12.dp))
                LocationStatusBar(
                    statusValue = uiState.locationStatusValue,
                    transmissionOk = uiState.transmissionOk,
                )
                Spacer(modifier = Modifier.height(24.dp))
                OndaPrimaryButton(
                    label = MockStartComplete.GO_OPERATION_LABEL,
                    onClick = viewModel::onGoToOperation,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun OperationInfoCard(info: StartCompleteInfo) {
    val rows = listOf(
        Triple(Icons.Outlined.Route, MockStartComplete.LABEL_ROUTE, info.routeName),
        Triple(Icons.Outlined.DirectionsBus, MockStartComplete.LABEL_VEHICLE, info.vehicleName),
        Triple(Icons.Outlined.AccessTime, MockStartComplete.LABEL_ACTUAL_START, info.actualStartTime),
        Triple(Icons.Outlined.LocationOn, MockStartComplete.LABEL_ORIGIN, info.origin),
        Triple(Icons.Rounded.Place, MockStartComplete.LABEL_DESTINATION, info.destination),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        rows.forEachIndexed { index, (icon, label, value) ->
            InfoRow(
                icon = icon,
                iconTint = if (index % 2 == 0) OndaColors.Accent else OndaColors.Primary,
                label = label,
                value = value,
            )
            if (index != rows.lastIndex) {
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
private fun InfoRow(
    icon: ImageVector,
    iconTint: Color,
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
            tint = iconTint,
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
private fun LocationStatusBar(
    statusValue: String,
    transmissionOk: Boolean,
) {
    val iconTint = if (transmissionOk) OndaColors.Accent else OndaColors.Warning
    val badgeBg = if (transmissionOk) StatusBadgeBg else OndaColors.WarningSoft
    val badgeFg = if (transmissionOk) OndaColors.Accent else OndaColors.Warning

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StatusBarBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = MockStartComplete.LOCATION_STATUS_LABEL,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        AppStatusBadge(
            label = statusValue,
            backgroundColor = badgeBg,
            foregroundColor = badgeFg,
            borderRadius = 999.dp,
            fontSizeSp = 11f,
        )
    }
}
