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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.MockSafeStopConfirm
import com.mju.onda.driver.feature.settings.viewmodel.SafeStopConfirmEvent
import com.mju.onda.driver.feature.settings.viewmodel.SafeStopConfirmViewModel

private val HeadlineBlue = Color(0xFF0A2A5C)
private val SubtitleBlue = Color(0xFF6B7A90)
private val CheckSoft = Color(0xFFDDF7F1)
private val CheckFg = Color(0xFF1F9D8A)
private val WarningSoft = Color(0xFFE8F8F5)
private val WarningFg = Color(0xFF1F9D8A)
private val CardDivider = Color(0xFFE3E9F1)

@Composable
fun SafeStopConfirmScreen(
    onBack: () -> Unit,
    onProceedStopOperation: (operationId: String) -> Unit,
    onReturnToOperation: (operationId: String?) -> Unit,
    viewModel: SafeStopConfirmViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SafeStopConfirmEvent.NavigateBack -> onBack()
                is SafeStopConfirmEvent.ProceedStopOperation ->
                    onProceedStopOperation(event.operationId)
                is SafeStopConfirmEvent.ReturnToOperation ->
                    onReturnToOperation(event.operationId)
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockSafeStopConfirm.SCREEN_TITLE,
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
                    painter = painterResource(id = R.drawable.safe_stop_confirm_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 199f),
                    contentScale = ContentScale.FillWidth,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = MockSafeStopConfirm.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineBlue,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = MockSafeStopConfirm.SUBTITLE,
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.5.sp,
                        color = SubtitleBlue,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                CheckCard(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(10.dp))

                WarningCard(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = viewModel::onConfirmSafeStop,
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
                        text = MockSafeStopConfirm.CONFIRM_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.TextOnPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onReturnToOperation,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockSafeStopConfirm.RETURN_LABEL,
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
private fun CheckCard(modifier: Modifier = Modifier) {
    InfoRowCard(
        modifier = modifier,
        icon = Icons.Outlined.Check,
        iconBg = CheckSoft,
        iconTint = CheckFg,
        title = MockSafeStopConfirm.CHECK_TITLE,
        titleColor = HeadlineBlue,
        description = MockSafeStopConfirm.CHECK_DESC,
        descriptionColor = SubtitleBlue,
        containerColor = OndaColors.Surface,
        bordered = true,
        showDivider = true,
        iconInCircle = true,
    )
}

@Composable
private fun WarningCard(modifier: Modifier = Modifier) {
    InfoRowCard(
        modifier = modifier,
        icon = Icons.Outlined.PrivacyTip,
        iconBg = Color.Transparent,
        iconTint = WarningFg,
        title = MockSafeStopConfirm.WARNING_TITLE,
        titleColor = WarningFg,
        description = MockSafeStopConfirm.WARNING_DESC,
        descriptionColor = SubtitleBlue,
        containerColor = WarningSoft,
        bordered = true,
        showDivider = true,
        iconInCircle = false,
    )
}

@Composable
private fun InfoRowCard(
    modifier: Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    titleColor: Color,
    description: String,
    descriptionColor: Color,
    containerColor: Color,
    bordered: Boolean,
    showDivider: Boolean,
    iconInCircle: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (bordered) {
                    Modifier
                        .background(containerColor, RoundedCornerShape(14.dp))
                        .border(1.dp, OndaColors.Border, RoundedCornerShape(14.dp))
                } else {
                    Modifier.background(containerColor, RoundedCornerShape(14.dp))
                },
            )
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (iconInCircle) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        // 구분선 유무와 관계없이 텍스트 시작 X를 동일하게 맞춤
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .width(1.dp)
                .height(48.dp)
                .background(if (showDivider) CardDivider else Color.Transparent),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    lineHeight = 20.sp,
                ),
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = description,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = descriptionColor,
                    lineHeight = 17.sp,
                ),
            )
        }
    }
}
