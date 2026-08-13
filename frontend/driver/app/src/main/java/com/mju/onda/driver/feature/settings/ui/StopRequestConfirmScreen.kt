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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Place
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
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.MockStopRequestConfirm
import com.mju.onda.driver.feature.settings.viewmodel.StopRequestConfirmEvent
import com.mju.onda.driver.feature.settings.viewmodel.StopRequestConfirmViewModel

private val TealIcon = Color(0xFF2BB673)
private val TealIconBg = Color(0xFFE8F8F5)
private val HeadlineBlue = Color(0xFF0A2A5C)
private val SubtitleGray = Color(0xFF6B7A90)
private val RowIconSize = 22.dp
private val HeaderIconCircle = 64.dp
private val HeaderIconSize = 34.dp

@Composable
fun StopRequestConfirmScreen(
    onBack: () -> Unit,
    onSent: () -> Unit,
    viewModel: StopRequestConfirmViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StopRequestConfirmEvent.NavigateBackToDetail -> onBack()
                StopRequestConfirmEvent.Sent -> onSent()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockStopRequestConfirm.SCREEN_TITLE,
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
                    painter = painterResource(id = R.drawable.settings_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 168f),
                    contentScale = ContentScale.FillWidth,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ConfirmCard(
                    reason = uiState.reason,
                    routeName = uiState.routeName,
                    vehicleName = uiState.vehicleName,
                    locationLabel = uiState.locationLabel,
                    attachmentLabel = uiState.attachmentLabel,
                    includeLocation = uiState.includeLocation,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PNG와 달리 요청 전송을 위, 취소를 아래로 배치
                Button(
                    onClick = viewModel::onSend,
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
                        text = MockStopRequestConfirm.SEND_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.TextOnPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onCancel,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockStopRequestConfirm.CANCEL_LABEL,
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
private fun ConfirmCard(
    reason: String,
    routeName: String,
    vehicleName: String,
    locationLabel: String,
    attachmentLabel: String,
    includeLocation: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(HeaderIconCircle)
                .background(TealIconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = TealIcon,
                modifier = Modifier.size(HeaderIconSize),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = MockStopRequestConfirm.HEADLINE,
            style = OndaTypography.headlineLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
            ),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = MockStopRequestConfirm.SUBTITLE,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            ),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        SummaryList(
            reason = reason,
            routeName = routeName,
            vehicleName = vehicleName,
            locationLabel = locationLabel,
            attachmentLabel = attachmentLabel,
            includeLocation = includeLocation,
        )
    }
}

@Composable
private fun SummaryList(
    reason: String,
    routeName: String,
    vehicleName: String,
    locationLabel: String,
    attachmentLabel: String,
    includeLocation: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OndaColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        SummaryRow(
            icon = Icons.Outlined.WarningAmber,
            label = MockStopRequestConfirm.LABEL_REASON,
            value = reason,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(
            icon = Icons.Outlined.DirectionsBus,
            label = MockStopRequestConfirm.LABEL_ROUTE,
            value = routeName,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(
            icon = Icons.Outlined.DirectionsCar,
            label = MockStopRequestConfirm.LABEL_VEHICLE,
            value = vehicleName,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(
            icon = Icons.Outlined.Place,
            label = MockStopRequestConfirm.LABEL_LOCATION,
            value = locationLabel,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(
            icon = Icons.Outlined.AttachFile,
            label = MockStopRequestConfirm.LABEL_ATTACHMENT,
            value = attachmentLabel,
            valueColor = if (includeLocation) TealIcon else OndaColors.TextPrimary,
        )
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = OndaColors.TextPrimary,
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
            tint = TealIcon,
            modifier = Modifier.size(RowIconSize),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            modifier = Modifier.width(72.dp),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                textAlign = TextAlign.End,
            ),
            textAlign = TextAlign.End,
        )
    }
}
