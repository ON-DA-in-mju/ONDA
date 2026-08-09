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
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.AccountInfo
import com.mju.onda.driver.feature.settings.data.MockAccountInfo
import com.mju.onda.driver.feature.settings.viewmodel.AccountInfoEvent
import com.mju.onda.driver.feature.settings.viewmodel.AccountInfoViewModel

private val InfoBannerBg = Color(0xFFEDF4FE)
private val IconSoftBg = Color(0xFFEDF4FE)

@Composable
fun AccountInfoScreen(
    onBack: () -> Unit,
    onGoToSettings: () -> Unit,
    onOpenEdit: () -> Unit,
    viewModel: AccountInfoViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AccountInfoEvent.NavigateBack -> onBack()
                AccountInfoEvent.GoToSettings -> onGoToSettings()
                AccountInfoEvent.OpenEdit -> onOpenEdit()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockAccountInfo.SCREEN_TITLE,
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

                AccountInfoCard(
                    info = uiState.info,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccountInfoBanner(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(20.dp))

                OndaPrimaryButton(
                    label = MockAccountInfo.EDIT_LABEL,
                    onClick = viewModel::onEdit,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    height = 48.dp,
                    fontSize = 15.sp,
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onGoToSettings,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = MockAccountInfo.GO_SETTINGS_LABEL,
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
private fun AccountInfoCard(
    info: AccountInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        // 첫 행: 버스 아이콘 + 기사명 라벨 / 값
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(IconSoftBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsBus,
                    contentDescription = null,
                    tint = OndaColors.Primary,
                    modifier = Modifier.size(66.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = info.driverNameLabel,
                style = OndaTypography.bodyMedium.copy(
                    fontSize = 14.sp,
                    color = OndaColors.TextPrimary,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = info.driverName,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.TextPrimary,
                ),
                textAlign = TextAlign.End,
            )
        }

        HorizontalDivider(color = OndaColors.Border)

        InfoRow(label = info.driverIdLabel, value = info.driverId, valueFontSize = 17.sp)
        HorizontalDivider(color = OndaColors.Border)
        InfoRow(label = info.orgLabel, value = info.organization)
        HorizontalDivider(color = OndaColors.Border)
        InfoRow(label = info.vehicleLabel, value = info.vehicleName)
        HorizontalDivider(color = OndaColors.Border)
        InfoRow(label = info.contactLabel, value = info.contactStatus)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueFontSize: TextUnit = 14.sp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = OndaTypography.bodyMedium.copy(
                fontSize = 14.sp,
                color = OndaColors.TextPrimary,
            ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = valueFontSize,
                fontWeight = FontWeight.Bold,
                color = OndaColors.TextPrimary,
            ),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AccountInfoBanner(modifier: Modifier = Modifier) {
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
            text = MockAccountInfo.INFO_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 17.sp,
            ),
        )
    }
}
