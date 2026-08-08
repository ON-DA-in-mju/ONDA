package com.mju.onda.driver.feature.settings.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.DirectionsBus
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.DriverProfile
import com.mju.onda.driver.feature.settings.data.MockDriverSettings
import com.mju.onda.driver.feature.settings.data.SettingsMenuId
import com.mju.onda.driver.feature.settings.data.SettingsMenuItem
import com.mju.onda.driver.feature.settings.viewmodel.DriverSettingsEvent
import com.mju.onda.driver.feature.settings.viewmodel.DriverSettingsViewModel

private val InfoBannerBg = Color(0xFFEDF4FE)
private val IconSoftBg = Color(0xFFEDF4FE)

@Composable
fun DriverSettingsScreen(
    onBack: () -> Unit,
    onOpenLogoutConfirm: () -> Unit,
    onOpenLogoutRestricted: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onOpenDevicePermission: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
    onOpenLocationConsentManage: () -> Unit,
    onOpenContactAdmin: () -> Unit,
    onOpenSafeStopHistory: () -> Unit,
    viewModel: DriverSettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
                DriverSettingsEvent.NavigateBack -> onBack()
                DriverSettingsEvent.OpenLogoutConfirm -> onOpenLogoutConfirm()
                DriverSettingsEvent.OpenLogoutRestricted -> onOpenLogoutRestricted()
                DriverSettingsEvent.OpenAccountInfo -> onOpenAccountInfo()
                DriverSettingsEvent.OpenDevicePermission -> onOpenDevicePermission()
                DriverSettingsEvent.OpenAlarmSettings -> onOpenAlarmSettings()
                DriverSettingsEvent.OpenLocationConsentManage -> onOpenLocationConsentManage()
                DriverSettingsEvent.OpenContactAdmin -> onOpenContactAdmin()
                DriverSettingsEvent.OpenSafeStopHistory -> onOpenSafeStopHistory()
                DriverSettingsEvent.NotInOperation -> {
                    Toast.makeText(
                        context,
                        MockDriverSettings.NOT_IN_OPERATION_TOAST,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                DriverSettingsEvent.MenuPending -> {
                    Toast.makeText(context, MockDriverSettings.PENDING_TOAST, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockDriverSettings.SCREEN_TITLE,
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

                ProfileCard(
                    profile = uiState.profile,
                    onClick = viewModel::onProfileClick,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .offset(y = (-18).dp)
                        .zIndex(1f),
                )

                MenuCard(
                    items = uiState.menuItems,
                    onItemClick = viewModel::onMenuClick,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .offset(y = (-8).dp),
                )

                Spacer(modifier = Modifier.height(6.dp))

                InfoBanner(
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                LogoutButton(
                    onClick = viewModel::onLogout,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: DriverProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(OndaColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsBus,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.nameLabel,
                style = OndaTypography.labelSmall.copy(
                    fontSize = 12.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = profile.name,
                style = OndaTypography.titleLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = profile.orgLabel,
                style = OndaTypography.labelSmall.copy(
                    fontSize = 12.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = profile.organization,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = OndaColors.TextHint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MenuCard(
    items: List<SettingsMenuItem>,
    onItemClick: (SettingsMenuId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        items.forEachIndexed { index, item ->
            MenuRow(
                icon = menuIcon(item.id),
                label = item.label,
                onClick = { onItemClick(item.id) },
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = OndaColors.Border.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = OndaColors.TextHint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InfoBanner(modifier: Modifier = Modifier) {
    // 메뉴 라벨 시작 X와 맞춤: 카드 내부 6+12 + 아이콘열 34 + 간격 12 = 64
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBannerBg, RoundedCornerShape(14.dp))
            .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = MockDriverSettings.INFO_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.TextPrimary,
                lineHeight = 19.sp,
            ),
        )
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, OndaColors.Primary),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = MockDriverSettings.LOGOUT_LABEL,
            style = OndaTypography.labelLarge.copy(
                fontSize = 15.sp,
                color = OndaColors.Primary,
            ),
        )
    }
}

private fun menuIcon(id: SettingsMenuId): ImageVector = when (id) {
    SettingsMenuId.DevicePermission -> Icons.Outlined.Shield
    SettingsMenuId.Alarm -> Icons.Outlined.Notifications
    SettingsMenuId.LocationConsent -> Icons.Outlined.Place
    SettingsMenuId.ContactAdmin -> Icons.Outlined.HeadsetMic
    SettingsMenuId.SafeStop -> Icons.Outlined.WarningAmber
}
