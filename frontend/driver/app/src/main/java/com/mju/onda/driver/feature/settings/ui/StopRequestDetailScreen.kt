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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.MockStopRequestDetail
import com.mju.onda.driver.feature.settings.viewmodel.StopRequestDetailEvent
import com.mju.onda.driver.feature.settings.viewmodel.StopRequestDetailViewModel

private val IconSoftBg = Color(0xFFEDF4FE)
private val InfoBannerBg = Color(0xFFEDF4FE)
private val SuccessSoft = Color(0xFFE6F4F1)
private val SuccessText = Color(0xFF00897B)
private val ToggleGreen = Color(0xFF2BB673)
private val RowIconSize = 28.dp
private val RowIconCircle = 34.dp

@Composable
fun StopRequestDetailScreen(
    selectedReason: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: StopRequestDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(selectedReason) {
        viewModel.load(selectedReason)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StopRequestDetailEvent.NavigateBack -> onBack()
                StopRequestDetailEvent.Submitted -> onSubmitted()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockStopRequestDetail.SCREEN_TITLE,
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
            ) {
                Image(
                    painter = painterResource(id = R.drawable.settings_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 168f),
                    contentScale = ContentScale.FillWidth,
                )

                Spacer(modifier = Modifier.height(12.dp))

                VehicleInfoCard(
                    routeName = uiState.routeName,
                    vehicleName = uiState.vehicleName,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = MockStopRequestDetail.SELECTED_REASON_PREFIX,
                        style = OndaTypography.bodyLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OndaColors.TextPrimary,
                        ),
                    )
                    Text(
                        text = uiState.selectedReason,
                        style = OndaTypography.bodyLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.Primary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = MockStopRequestDetail.MESSAGE_LABEL,
                        style = OndaTypography.bodyLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OndaColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = MockStopRequestDetail.MESSAGE_REQUIRED_HINT,
                        style = OndaTypography.bodyLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OndaColors.Primary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                MessageInputBox(
                    value = uiState.message,
                    onValueChange = viewModel::onMessageChange,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))

                ShareOptionsCard(
                    attachLocation = uiState.attachLocation,
                    contactable = uiState.contactable,
                    onAttachLocationChange = viewModel::onAttachLocationChange,
                    onContactableChange = viewModel::onContactableChange,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = viewModel::onSubmit,
                    enabled = uiState.canSubmit,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OndaColors.Primary,
                        contentColor = OndaColors.TextOnPrimary,
                        disabledContainerColor = OndaColors.Border,
                        disabledContentColor = OndaColors.TextHint,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockStopRequestDetail.SUBMIT_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.canSubmit) {
                                OndaColors.TextOnPrimary
                            } else {
                                OndaColors.TextHint
                            },
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onPrevious,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockStopRequestDetail.PREV_LABEL,
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
private fun VehicleInfoCard(
    routeName: String,
    vehicleName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        InfoRow(icon = Icons.Outlined.DirectionsBus, label = routeName)
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.85f))
        InfoRow(icon = Icons.Outlined.DirectionsCar, label = vehicleName)
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.85f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIcon(Icons.Outlined.Wifi)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = MockStopRequestDetail.STATUS_LABEL,
                modifier = Modifier.weight(1f),
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            StatusBadge()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(icon)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun CircleIcon(icon: ImageVector) {
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
}

@Composable
private fun StatusBadge() {
    Text(
        text = MockStopRequestDetail.STATUS_IN_PROGRESS,
        modifier = Modifier
            .background(SuccessSoft, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = OndaTypography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SuccessText,
        ),
    )
}

@Composable
private fun MessageInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .background(OndaColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 22.dp),
            textStyle = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(OndaColors.Primary),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = MockStopRequestDetail.MESSAGE_HINT,
                        style = OndaTypography.bodyLarge.copy(
                            fontSize = 14.sp,
                            color = OndaColors.TextHint,
                        ),
                    )
                }
                inner()
            },
        )
        Text(
            text = "${value.length}/${MockStopRequestDetail.MAX_MESSAGE_LENGTH}",
            modifier = Modifier.align(Alignment.BottomEnd),
            style = OndaTypography.labelSmall.copy(
                fontSize = 11.sp,
                color = OndaColors.TextHint,
            ),
        )
    }
}

@Composable
private fun ShareOptionsCard(
    attachLocation: Boolean,
    contactable: Boolean,
    onAttachLocationChange: (Boolean) -> Unit,
    onContactableChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        ShareOptionRow(
            icon = Icons.Outlined.Place,
            title = MockStopRequestDetail.LOCATION_TITLE,
            description = MockStopRequestDetail.LOCATION_DESC,
            checked = attachLocation,
            onCheckedChange = onAttachLocationChange,
        )
        HorizontalDivider(color = OndaColors.Border.copy(alpha = 0.85f))
        ShareOptionRow(
            icon = Icons.Outlined.Phone,
            title = MockStopRequestDetail.CONTACT_TITLE,
            description = MockStopRequestDetail.CONTACT_DESC,
            checked = contactable,
            onCheckedChange = onContactableChange,
        )
    }
}

@Composable
private fun ShareOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
                .background(SuccessSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SuccessText,
                modifier = Modifier.size(RowIconSize),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = MockStopRequestDetail.REQUIRED_LABEL,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OndaColors.Primary,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = OndaColors.TextSecondary,
                    lineHeight = 16.sp,
                ),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OndaColors.TextOnPrimary,
                checkedTrackColor = ToggleGreen,
                checkedBorderColor = ToggleGreen,
                uncheckedThumbColor = OndaColors.TextOnPrimary,
                uncheckedTrackColor = OndaColors.Border,
                uncheckedBorderColor = OndaColors.Border,
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = MockStopRequestDetail.INFO_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
            ),
        )
    }
}
