package com.mju.onda.driver.feature.adminforceend.ui

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.GppBad
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
import androidx.compose.ui.platform.LocalContext
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
import com.mju.onda.driver.feature.adminforceend.data.AdminForceEndInfo
import com.mju.onda.driver.feature.adminforceend.data.MockAdminForceEnd
import com.mju.onda.driver.feature.adminforceend.viewmodel.AdminForceEndEvent
import com.mju.onda.driver.feature.adminforceend.viewmodel.AdminForceEndViewModel

private val HeadlineDark = Color(0xFF0A2A5C)
private val ShieldCircleBg = Color(0xFFFFEBEE)
private val ShieldRed = Color(0xFFE53935)
private val NoticeBg = Color(0xFFFFF0F0)
private val NoticeBorder = Color(0xFFF5C6C6)
private val IconCircleBg = Color(0xFFEDF4FE)

@Composable
fun AdminForceEndScreen(
    operationId: String,
    onBack: () -> Unit,
    onGoToToday: () -> Unit,
    viewModel: AdminForceEndViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(operationId) {
        viewModel.load(operationId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AdminForceEndEvent.NavigateBack -> onBack()
                AdminForceEndEvent.GoToTodayOperation -> onGoToToday()
                AdminForceEndEvent.ContactAdmin -> {
                    Toast.makeText(
                        context,
                        MockAdminForceEnd.CONTACT_ADMIN_TOAST,
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
                title = MockAdminForceEnd.SCREEN_TITLE,
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
                ForceEndShieldIcon(modifier = Modifier.fillMaxWidth(0.38f))
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = MockAdminForceEnd.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineDark,
                        lineHeight = 28.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = MockAdminForceEnd.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                SummaryCard(info = uiState.info)
                Spacer(modifier = Modifier.height(12.dp))
                NoticeBanner()
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockAdminForceEnd.GO_TODAY_LABEL,
                    onClick = viewModel::onGoToToday,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockAdminForceEnd.CONTACT_ADMIN_LABEL,
                    onClick = viewModel::onContactAdmin,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun ForceEndShieldIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShieldCircleBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.GppBad,
                contentDescription = null,
                tint = ShieldRed,
                modifier = Modifier.fillMaxSize(0.82f),
            )
        }
    }
}

@Composable
private fun SummaryCard(info: AdminForceEndInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        SummaryRow(Icons.Outlined.DirectionsBus, MockAdminForceEnd.LABEL_ROUTE, info.routeName)
        SummaryDivider()
        SummaryRow(Icons.Outlined.DirectionsCar, MockAdminForceEnd.LABEL_VEHICLE, info.vehicleName)
        SummaryDivider()
        SummaryRow(Icons.Outlined.AccessTime, MockAdminForceEnd.LABEL_ACTUAL_START, info.actualStart)
        SummaryDivider()
        SummaryRow(Icons.Outlined.AccessTime, MockAdminForceEnd.LABEL_PROCESSED_AT, info.processedAt)
        SummaryDivider()
        SummaryRow(Icons.Outlined.AccessTime, MockAdminForceEnd.LABEL_REASON, info.reason)
        SummaryDivider()
        SummaryRow(Icons.Outlined.Person, MockAdminForceEnd.LABEL_PROCESSOR, info.processor)
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
private fun NoticeBanner() {
    val iconSize = 42.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoticeBg, RoundedCornerShape(14.dp))
            .border(1.dp, NoticeBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(ShieldRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize * 0.82f),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = MockAdminForceEnd.NOTICE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
