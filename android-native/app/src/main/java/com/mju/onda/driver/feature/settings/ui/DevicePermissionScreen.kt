package com.mju.onda.driver.feature.settings.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.DeviceStatusItem
import com.mju.onda.driver.feature.settings.data.DeviceStatusStyle
import com.mju.onda.driver.feature.settings.data.MockDevicePermission
import com.mju.onda.driver.feature.settings.viewmodel.DevicePermissionEvent
import com.mju.onda.driver.feature.settings.viewmodel.DevicePermissionViewModel

private val InfoBannerBg = Color(0xFFEDF4FE)
private val IconSoftBg = Color(0xFFEDF4FE)
private val SuccessSoft = Color(0xFFE6F4F1)
private val SuccessText = Color(0xFF00897B)
private val AccentSoft = Color(0xFFDDF7F1)
private val AccentText = Color(0xFF1F9D8A)
private val WarningSoft = Color(0xFFFFF4E5)
private val WarningText = Color(0xFFED6C02)
private val DeniedSoft = Color(0xFFFDECEA)
private val DeniedText = Color(0xFFD32F2F)

@Composable
fun DevicePermissionScreen(
    onBack: () -> Unit,
    viewModel: DevicePermissionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshFromSystem(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshFromSystem(context)
        viewModel.events.collect { event ->
            when (event) {
                DevicePermissionEvent.NavigateBack -> onBack()
                DevicePermissionEvent.RefreshDone -> {
                    Toast.makeText(
                        context,
                        MockDevicePermission.REFRESH_TOAST,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                DevicePermissionEvent.OpenSystemSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockDevicePermission.SCREEN_TITLE,
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

                StatusCard(
                    items = uiState.items,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.onRefresh(context) },
                    enabled = !uiState.isRefreshing,
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
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OndaColors.TextOnPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = MockDevicePermission.REFRESH_LABEL,
                            style = OndaTypography.labelLarge.copy(
                                fontSize = 15.sp,
                                color = OndaColors.TextOnPrimary,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onOpenSettings,
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
                        text = MockDevicePermission.OPEN_SETTINGS_LABEL,
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
private fun StatusCard(
    items: List<DeviceStatusItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        items.forEachIndexed { index, item ->
            StatusRow(item = item)
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    color = OndaColors.Border.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun StatusRow(item: DeviceStatusItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        StatusValue(item = item)
    }
}

@Composable
private fun StatusValue(item: DeviceStatusItem) {
    when (item.style) {
        DeviceStatusStyle.Plain -> {
            Text(
                text = item.value,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.TextPrimary,
                ),
            )
        }
        DeviceStatusStyle.Success -> StatusBadge(
            text = item.value,
            bg = SuccessSoft,
            fg = SuccessText,
            leading = Icons.Outlined.Check,
        )
        DeviceStatusStyle.Accent -> StatusBadge(
            text = item.value,
            bg = AccentSoft,
            fg = AccentText,
            leading = Icons.Outlined.Check,
        )
        DeviceStatusStyle.Warning -> StatusBadge(
            text = item.value,
            bg = WarningSoft,
            fg = WarningText,
            leading = Icons.Outlined.WarningAmber,
        )
        DeviceStatusStyle.Denied -> StatusBadge(
            text = item.value,
            bg = DeniedSoft,
            fg = DeniedText,
            leading = Icons.Outlined.Close,
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    bg: Color,
    fg: Color,
    leading: ImageVector?,
) {
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = OndaTypography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            ),
        )
    }
}

@Composable
private fun InfoBanner(modifier: Modifier = Modifier) {
    // 상태 카드와 동일 시작점(card 8 + row 10) → 라벨 X(icon 34 + gap 12)에 문구 정렬
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBannerBg, RoundedCornerShape(12.dp))
            .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(46.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = MockDevicePermission.INFO_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 17.sp,
            ),
        )
    }
}
