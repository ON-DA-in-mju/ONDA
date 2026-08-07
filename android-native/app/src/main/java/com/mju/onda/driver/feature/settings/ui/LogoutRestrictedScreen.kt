package com.mju.onda.driver.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
import com.mju.onda.driver.feature.settings.data.MockLogoutRestricted
import com.mju.onda.driver.feature.settings.viewmodel.LogoutRestrictedEvent
import com.mju.onda.driver.feature.settings.viewmodel.LogoutRestrictedUiState
import com.mju.onda.driver.feature.settings.viewmodel.LogoutRestrictedViewModel

private val IconSoftBg = Color(0xFFEDF4FE)
private val InfoBannerBg = Color(0xFFEDF4FE)
private val HeadlineBlack = Color(0xFF111111)
private val SubtitleGray = Color(0xFF6B7A90)
private val StatusBlue = OndaColors.Primary
private val StatusGreen = Color(0xFF2BB673)

@Composable
fun LogoutRestrictedScreen(
    onBack: () -> Unit,
    onGoToOperation: (operationId: String) -> Unit,
    onContactAdmin: () -> Unit,
    viewModel: LogoutRestrictedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LogoutRestrictedEvent.NavigateBack -> onBack()
                is LogoutRestrictedEvent.GoToOperation -> onGoToOperation(event.operationId)
                LogoutRestrictedEvent.ContactAdmin -> onContactAdmin()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockLogoutRestricted.SCREEN_TITLE,
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
                Image(
                    painter = painterResource(id = R.drawable.logout_confirm_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 200f)
                        .padding(horizontal = 8.dp),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.height(8.dp))

                WarningPromptCard(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(12.dp))

                OperationStatusCard(
                    uiState = uiState,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::onGoToOperation,
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
                        text = MockLogoutRestricted.GO_OPERATION_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.TextOnPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onContactAdmin,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockLogoutRestricted.CONTACT_ADMIN_LABEL,
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
private fun WarningPromptCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = MockLogoutRestricted.HEADLINE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HeadlineBlack,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = MockLogoutRestricted.SUBTITLE,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OperationStatusCard(
    uiState: LogoutRestrictedUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        StatusRow(
            label = MockLogoutRestricted.LABEL_ROUTE,
            value = uiState.routeName,
            valueColor = OndaColors.TextPrimary,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        StatusRow(
            label = MockLogoutRestricted.LABEL_VEHICLE,
            value = uiState.vehicleName,
            valueColor = OndaColors.TextPrimary,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        StatusRow(
            label = MockLogoutRestricted.LABEL_STATUS,
            value = uiState.statusLabel,
            valueColor = StatusBlue,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        StatusRow(
            label = MockLogoutRestricted.LABEL_LOCATION,
            value = uiState.locationStatus,
            valueColor = if (uiState.transmissionOk) StatusGreen else OndaColors.Warning,
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                color = OndaColors.TextSecondary,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            ),
        )
    }
}

@Composable
private fun InfoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBannerBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = MockLogoutRestricted.INFO_BANNER,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 12.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 17.sp,
            ),
        )
    }
}
