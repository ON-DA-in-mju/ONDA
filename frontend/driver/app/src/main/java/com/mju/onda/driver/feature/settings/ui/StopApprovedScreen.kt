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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PrivacyTip
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
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.MockStopApproved
import com.mju.onda.driver.feature.settings.viewmodel.StopApprovedEvent
import com.mju.onda.driver.feature.settings.viewmodel.StopApprovedViewModel

private val IconSoftBg = Color(0xFFEDF4FE)
private val InfoBannerBg = Color(0xFFEDF4FE)
private val SuccessText = Color(0xFF2BB673)
private val HeadlineBlack = Color(0xFF111111)
private val SubtitleGray = Color(0xFF6B7A90)
private val RowIconSize = 22.dp
private val RowIconCircle = 34.dp

@Composable
fun StopApprovedScreen(
    onBack: () -> Unit,
    onEndOperation: (operationId: String) -> Unit,
    onContactAdmin: () -> Unit,
    viewModel: StopApprovedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StopApprovedEvent.NavigateBack -> onBack()
                is StopApprovedEvent.EndOperation -> onEndOperation(event.operationId)
                StopApprovedEvent.ContactAdmin -> onContactAdmin()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockStopApproved.SCREEN_TITLE,
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
                    painter = painterResource(id = R.drawable.stop_approved_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 200f)
                        .padding(horizontal = 12.dp),
                    contentScale = ContentScale.Fit,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = buildAnnotatedString {
                        append(MockStopApproved.HEADLINE_PREFIX)
                        withStyle(SpanStyle(color = SuccessText, fontWeight = FontWeight.ExtraBold)) {
                            append(MockStopApproved.HEADLINE_HIGHLIGHT)
                        }
                        append(MockStopApproved.HEADLINE_SUFFIX)
                    },
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
                    text = MockStopApproved.SUBTITLE,
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

                ApprovedInfoCard(
                    approvedAt = uiState.approvedAt,
                    reason = uiState.reason,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                SafetyBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = viewModel::onEndOperation,
                    enabled = uiState.actionsEnabled,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OndaColors.Primary,
                        contentColor = OndaColors.TextOnPrimary,
                        disabledContainerColor = OndaColors.Border,
                        disabledContentColor = OndaColors.TextSecondary,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockStopApproved.END_OPERATION_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.actionsEnabled) {
                                OndaColors.TextOnPrimary
                            } else {
                                OndaColors.TextSecondary
                            },
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
                        text = MockStopApproved.CONTACT_ADMIN_LABEL,
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
private fun ApprovedInfoCard(
    approvedAt: String,
    reason: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        InfoRow(Icons.Outlined.AccessTime, MockStopApproved.LABEL_TIME) {
            Text(
                text = approvedAt,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        InfoRow(Icons.Outlined.Info, MockStopApproved.LABEL_REASON) {
            Text(
                text = reason,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        InfoRow(Icons.Outlined.Place, MockStopApproved.LABEL_LOCATION) {
            Text(
                text = MockStopApproved.LOCATION_ENDED,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuccessText,
                ),
            )
        }
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.9f))
        InfoRow(Icons.Outlined.Person, MockStopApproved.LABEL_ADMIN) {
            Text(
                text = MockStopApproved.ADMIN_GUIDE,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    lineHeight = 17.sp,
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 180.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(
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
private fun SafetyBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(InfoBannerBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.PrivacyTip,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = MockStopApproved.SAFETY_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 18.sp,
            ),
        )
    }
}
