package com.mju.onda.driver.feature.settings.ui



import android.Manifest

import android.content.Intent

import android.net.Uri

import android.os.Build

import android.provider.Settings

import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.result.contract.ActivityResultContracts

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

import androidx.compose.material.icons.outlined.Notifications

import androidx.compose.material.icons.outlined.OpenInNew

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Switch

import androidx.compose.material3.SwitchDefaults

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.DisposableEffect

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

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

import com.mju.onda.driver.core.ui.components.OndaPrimaryButton

import com.mju.onda.driver.core.ui.components.OndaTopBar

import com.mju.onda.driver.feature.settings.data.AlarmSettingItem

import com.mju.onda.driver.feature.settings.data.MockAlarmSettings

import com.mju.onda.driver.feature.settings.viewmodel.AlarmSettingsEvent

import com.mju.onda.driver.feature.settings.viewmodel.AlarmSettingsViewModel



private val InfoBannerBg = Color(0xFFEDF4FE)

private val IconSoftBg = Color(0xFFEDF4FE)

private val RowIconSize = 28.dp

private val RowIconCircle = 34.dp



@Composable

fun AlarmSettingsScreen(

    onBack: () -> Unit,

    onGoToSettings: () -> Unit,

    viewModel: AlarmSettingsViewModel = viewModel(),

) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current



    val permissionLauncher = rememberLauncherForActivityResult(

        ActivityResultContracts.RequestPermission(),

    ) { granted ->

        viewModel.onNotificationPermissionResult(granted, context)

        if (!granted) {

            openAppNotificationSettings(context)

        }

    }



    val settingsLauncher = rememberLauncherForActivityResult(

        ActivityResultContracts.StartActivityForResult(),

    ) {

        viewModel.refreshFromSystem(context)

    }



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

                AlarmSettingsEvent.NavigateBack -> onBack()

                AlarmSettingsEvent.GoToSettings -> onGoToSettings()

                AlarmSettingsEvent.Saved -> {

                    Toast.makeText(

                        context,

                        MockAlarmSettings.SAVE_TOAST,

                        Toast.LENGTH_SHORT,

                    ).show()

                }

                AlarmSettingsEvent.RequestNotificationPermission -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                    } else {

                        settingsLauncher.launch(appNotificationSettingsIntent(context))

                    }

                }

                AlarmSettingsEvent.OpenAppNotificationSettings -> {

                    settingsLauncher.launch(appNotificationSettingsIntent(context))

                }

            }

        }

    }



    Scaffold(

        containerColor = OndaColors.Background,

        topBar = {

            OndaTopBar(

                title = MockAlarmSettings.SCREEN_TITLE,

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



                AlarmSettingsCard(

                    notificationsEnabled = uiState.notificationsEnabled,

                    items = uiState.items,

                    onMasterToggle = viewModel::onMasterToggle,

                    onToggle = viewModel::onToggle,

                    modifier = Modifier.padding(horizontal = 16.dp),

                )



                Spacer(modifier = Modifier.height(12.dp))



                AlarmInfoBanner(modifier = Modifier.padding(horizontal = 16.dp))



                Spacer(modifier = Modifier.height(20.dp))



                OndaPrimaryButton(

                    label = MockAlarmSettings.SAVE_LABEL,

                    onClick = viewModel::onSave,

                    enabled = uiState.notificationsEnabled && uiState.hasChanges,

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

                        imageVector = Icons.Outlined.OpenInNew,

                        contentDescription = null,

                        tint = OndaColors.Primary,

                        modifier = Modifier.size(18.dp),

                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(

                        text = MockAlarmSettings.GO_SETTINGS_LABEL,

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

private fun AlarmSettingsCard(

    notificationsEnabled: Boolean,

    items: List<AlarmSettingItem>,

    onMasterToggle: (Boolean) -> Unit,

    onToggle: (String, Boolean) -> Unit,

    modifier: Modifier = Modifier,

) {

    Column(

        modifier = modifier

            .fillMaxWidth()

            .background(OndaColors.Surface, RoundedCornerShape(16.dp))

            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))

            .padding(horizontal = 8.dp, vertical = 4.dp),

    ) {

        AlarmMasterRow(

            enabled = notificationsEnabled,

            onToggle = onMasterToggle,

        )

        HorizontalDivider(

            modifier = Modifier.padding(horizontal = 10.dp),

            color = OndaColors.Border.copy(alpha = 0.85f),

        )

        items.forEachIndexed { index, item ->

            AlarmSettingRow(

                item = item,

                interactive = notificationsEnabled,

                onToggle = onToggle,

            )

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

private fun AlarmMasterRow(

    enabled: Boolean,

    onToggle: (Boolean) -> Unit,

) {

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = 10.dp, vertical = 10.dp),

        verticalAlignment = Alignment.CenterVertically,

    ) {

        Box(

            modifier = Modifier

                .size(RowIconCircle)

                .background(IconSoftBg, CircleShape),

            contentAlignment = Alignment.Center,

        ) {

            Icon(

                imageVector = Icons.Outlined.Notifications,

                contentDescription = null,

                tint = OndaColors.Primary,

                modifier = Modifier.size(RowIconSize),

            )

        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(

            text = MockAlarmSettings.MASTER_LABEL,

            modifier = Modifier.weight(1f),

            style = OndaTypography.bodyLarge.copy(

                fontSize = 14.sp,

                fontWeight = FontWeight.Bold,

                color = OndaColors.TextPrimary,

            ),

        )

        Text(

            text = if (enabled) MockAlarmSettings.STATUS_ON else MockAlarmSettings.STATUS_OFF,

            style = OndaTypography.bodyLarge.copy(

                fontSize = 13.sp,

                fontWeight = FontWeight.SemiBold,

                color = if (enabled) OndaColors.Primary else OndaColors.TextMuted,

            ),

        )

        Spacer(modifier = Modifier.width(8.dp))

        Switch(

            checked = enabled,

            onCheckedChange = onToggle,

            colors = SwitchDefaults.colors(

                checkedThumbColor = OndaColors.TextOnPrimary,

                checkedTrackColor = OndaColors.Primary,

                checkedBorderColor = OndaColors.Primary,

                uncheckedThumbColor = OndaColors.TextOnPrimary,

                uncheckedTrackColor = OndaColors.Border,

                uncheckedBorderColor = OndaColors.Border,

            ),

        )

    }

}



@Composable

private fun AlarmSettingRow(

    item: AlarmSettingItem,

    interactive: Boolean,

    onToggle: (String, Boolean) -> Unit,

) {

    val labelColor = if (interactive) OndaColors.TextPrimary else OndaColors.TextMuted

    val statusColor = when {

        !interactive -> OndaColors.TextMuted

        item.enabled -> OndaColors.Primary

        else -> OndaColors.TextMuted

    }



    Row(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = 10.dp, vertical = 10.dp),

        verticalAlignment = Alignment.CenterVertically,

    ) {

        Box(

            modifier = Modifier

                .size(RowIconCircle)

                .background(

                    if (interactive) IconSoftBg else OndaColors.Border.copy(alpha = 0.35f),

                    CircleShape,

                ),

            contentAlignment = Alignment.Center,

        ) {

            Icon(

                imageVector = Icons.Outlined.Notifications,

                contentDescription = null,

                tint = if (interactive) OndaColors.Primary else OndaColors.TextMuted,

                modifier = Modifier.size(RowIconSize),

            )

        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(

            text = item.label,

            modifier = Modifier.weight(1f),

            style = OndaTypography.bodyLarge.copy(

                fontSize = 14.sp,

                fontWeight = FontWeight.SemiBold,

                color = labelColor,

            ),

        )

        Text(

            text = if (item.enabled) MockAlarmSettings.STATUS_ON else MockAlarmSettings.STATUS_OFF,

            style = OndaTypography.bodyLarge.copy(

                fontSize = 13.sp,

                fontWeight = FontWeight.SemiBold,

                color = statusColor,

            ),

        )

        Spacer(modifier = Modifier.width(8.dp))

        Switch(

            checked = item.enabled,

            onCheckedChange = { onToggle(item.id, it) },

            enabled = interactive,

            colors = SwitchDefaults.colors(

                checkedThumbColor = OndaColors.TextOnPrimary,

                checkedTrackColor = OndaColors.Primary,

                checkedBorderColor = OndaColors.Primary,

                uncheckedThumbColor = OndaColors.TextOnPrimary,

                uncheckedTrackColor = OndaColors.Border,

                uncheckedBorderColor = OndaColors.Border,

                disabledCheckedThumbColor = OndaColors.TextOnPrimary,

                disabledCheckedTrackColor = OndaColors.Border,

                disabledUncheckedThumbColor = OndaColors.TextOnPrimary,

                disabledUncheckedTrackColor = OndaColors.Border.copy(alpha = 0.5f),

            ),

        )

    }

}



@Composable

private fun AlarmInfoBanner(modifier: Modifier = Modifier) {

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

                modifier = Modifier.size(RowIconSize),

            )

        }

        Text(

            text = MockAlarmSettings.INFO_BANNER,

            style = OndaTypography.bodySmall.copy(

                fontSize = 12.5.sp,

                color = OndaColors.TextPrimary,

                lineHeight = 17.sp,

            ),

        )

    }

}



private fun appNotificationSettingsIntent(context: android.content.Context): Intent {

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {

            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

        }

    } else {

        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {

            data = Uri.fromParts("package", context.packageName, null)

        }

    }

}



private fun openAppNotificationSettings(context: android.content.Context) {

    context.startActivity(appNotificationSettingsIntent(context))

}

