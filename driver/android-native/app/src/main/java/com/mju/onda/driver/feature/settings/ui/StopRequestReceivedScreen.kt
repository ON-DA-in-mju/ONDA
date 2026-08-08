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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mju.onda.driver.feature.settings.data.MockStopRequestReceived
import com.mju.onda.driver.feature.settings.viewmodel.StopRequestReceivedEvent
import com.mju.onda.driver.feature.settings.viewmodel.StopRequestReceivedViewModel

private val IconSoftBg = Color(0xFFEDF4FE)
private val InfoBannerBg = Color(0xFFEDF4FE)
private val InfoBannerBorder = Color(0xFFB7D0F8)
private val SuccessSoft = Color(0xFFE6F4F1)
private val SuccessText = Color(0xFF00897B)
private val CancelSoft = Color(0xFFFFEBEE)
private val CancelText = Color(0xFFE53935)
private val HeadlineBlack = Color(0xFF111111)
private val SubtitleGray = Color(0xFF6B7A90)
private val RowIconSize = 22.dp
private val RowIconCircle = 34.dp

@Composable
fun StopRequestReceivedScreen(
    onBack: () -> Unit,
    onGoToList: () -> Unit,
    onContactAdmin: () -> Unit,
    onOpenApproved: () -> Unit = {},
    onOpenContinue: () -> Unit = {},
    viewModel: StopRequestReceivedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StopRequestReceivedEvent.NavigateBack -> onBack()
                StopRequestReceivedEvent.GoToList -> onGoToList()
                StopRequestReceivedEvent.ContactAdmin -> onContactAdmin()
                StopRequestReceivedEvent.Cancelled -> onGoToList()
                is StopRequestReceivedEvent.OpenApproved -> onOpenApproved()
                is StopRequestReceivedEvent.OpenContinue -> onOpenContinue()
            }
        }
    }

    if (uiState.showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCancelConfirmDialog,
            text = {
                Text(
                    text = MockStopRequestReceived.CANCEL_CONFIRM_MESSAGE,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 15.sp,
                        color = OndaColors.TextPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onCancelConfirmYes) {
                    Text(
                        text = MockStopRequestReceived.CANCEL_CONFIRM_YES,
                        color = CancelText,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCancelConfirmDialog) {
                    Text(
                        text = MockStopRequestReceived.CANCEL_CONFIRM_NO,
                        color = OndaColors.TextSecondary,
                    )
                }
            },
        )
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockStopRequestReceived.SCREEN_TITLE,
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
                    painter = painterResource(id = R.drawable.stop_request_received_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 200f)
                        .padding(horizontal = 8.dp),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = MockStopRequestReceived.HEADLINE,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineBlack,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = MockStopRequestReceived.SUBTITLE,
                    modifier = Modifier.padding(horizontal = 28.dp),
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.sp,
                        color = SubtitleGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(18.dp))

                ReceivedSummaryCard(
                    reason = uiState.reason,
                    requestedAt = uiState.requestedAt,
                    adminStatus = uiState.adminStatus,
                    adminCancelled = uiState.adminStatus == MockStopRequestReceived.ADMIN_CANCELLED,
                    gpsStatus = uiState.gpsStatus,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::onGoToList,
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
                        text = MockStopRequestReceived.LIST_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.TextOnPrimary,
                        ),
                    )
                }

                if (uiState.canCancel) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = viewModel::onCancelRequestClick,
                        enabled = !uiState.cancelling,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, CancelText),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CancelText,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Text(
                            text = MockStopRequestReceived.CANCEL_REQUEST_LABEL,
                            style = OndaTypography.labelLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CancelText,
                            ),
                        )
                    }
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
                        text = MockStopRequestReceived.CONTACT_ADMIN_LABEL,
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
private fun ReceivedSummaryCard(
    reason: String,
    requestedAt: String,
    adminStatus: String,
    adminCancelled: Boolean,
    gpsStatus: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        SummaryRow(
            icon = Icons.Outlined.DirectionsCar,
            label = MockStopRequestReceived.LABEL_REASON,
            value = {
                Text(
                    text = reason,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OndaColors.TextPrimary,
                    ),
                )
            },
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(
            icon = Icons.Outlined.AccessTime,
            label = MockStopRequestReceived.LABEL_TIME,
            value = {
                Text(
                    text = requestedAt,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OndaColors.TextPrimary,
                    ),
                )
            },
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(
            icon = Icons.Outlined.Person,
            label = MockStopRequestReceived.LABEL_ADMIN,
            value = {
                Text(
                    text = adminStatus,
                    modifier = Modifier
                        .background(
                            if (adminCancelled) CancelSoft else SuccessSoft,
                            RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    style = OndaTypography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (adminCancelled) CancelText else SuccessText,
                    ),
                )
            },
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        SummaryRow(
            icon = Icons.Outlined.Place,
            label = MockStopRequestReceived.LABEL_GPS,
            value = {
                Text(
                    text = gpsStatus,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessText,
                    ),
                )
            },
        )
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(RowIconCircle)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(RowIconSize),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                color = OndaColors.TextSecondary,
            ),
        )
        value()
    }
}

@Composable
private fun InfoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(InfoBannerBg, RoundedCornerShape(12.dp))
            .border(1.dp, InfoBannerBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = MockStopRequestReceived.INFO_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 20.sp,
            ),
        )
    }
}
