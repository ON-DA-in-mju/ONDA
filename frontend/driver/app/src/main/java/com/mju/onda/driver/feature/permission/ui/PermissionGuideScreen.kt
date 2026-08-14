package com.mju.onda.driver.feature.permission.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.mju.onda.driver.core.ui.components.InfoIconCard
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.permission.data.MockPermissionGuide
import com.mju.onda.driver.feature.permission.viewmodel.PermissionGuideEvent
import com.mju.onda.driver.feature.permission.viewmodel.PermissionGuideViewModel

@Composable
fun PermissionGuideScreen(
    onContinue: () -> Unit,
    onSkipToHome: () -> Unit,
    onBack: () -> Unit,
    viewModel: PermissionGuideViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onForegroundPermissionResult(context, locationGranted = fine || coarse)
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.onBackgroundPermissionResult(context)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PermissionGuideEvent.NavigateToPermissionComplete -> onContinue()
                PermissionGuideEvent.NavigateToTodayOperation -> onSkipToHome()
                PermissionGuideEvent.NavigateBack -> onBack()
                PermissionGuideEvent.RequestSystemLocationPermissions -> {
                    val permissions = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    locationPermissionLauncher.launch(permissions)
                }
                PermissionGuideEvent.RequestBackgroundLocationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundPermissionLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        )
                    } else {
                        viewModel.onBackgroundPermissionResult(context)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockPermissionGuide.TITLE,
                onBack = viewModel::onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.widthIn(max = 430.dp)) {
                    OndaPrimaryButton(
                        label = MockPermissionGuide.SETUP_LABEL,
                        onClick = viewModel::onSetupPermissions,
                        isLoading = uiState.isRequesting,
                        height = 44.dp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OndaOutlinedButton(
                        label = MockPermissionGuide.LATER_LABEL,
                        onClick = viewModel::onSetupLaterClick,
                        enabled = !uiState.isRequesting,
                        height = 44.dp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFFABB0BA),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = MockPermissionGuide.FOOTER_NOTICE,
                            style = OndaTypography.labelSmall.copy(
                                color = Color(0xFFABB0BA),
                                fontSize = 11.5.sp,
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 4.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 430.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.permission_guide_illustration),
                    contentDescription = "권한 안내 일러스트",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = MockPermissionGuide.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 29.7.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = MockPermissionGuide.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.3.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(18.dp))

                uiState.items.forEachIndexed { index, item ->
                    InfoIconCard(
                        icon = item.icon,
                        title = item.title,
                        description = item.description,
                        iconBackgroundColor = OndaColors.PrimarySoft,
                        iconColor = OndaColors.Primary,
                    )
                    if (index != uiState.items.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    if (uiState.showLaterDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLaterDialog,
            title = { Text(text = MockPermissionGuide.LATER_DIALOG_TITLE) },
            text = { Text(text = MockPermissionGuide.LATER_DIALOG_MESSAGE) },
            confirmButton = {
                TextButton(onClick = viewModel::onSetupPermissions) {
                    Text(MockPermissionGuide.LATER_DIALOG_CONFIRM)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmSkipForLater(context) }) {
                    Text(MockPermissionGuide.LATER_DIALOG_DISMISS)
                }
            },
        )
    }
}
