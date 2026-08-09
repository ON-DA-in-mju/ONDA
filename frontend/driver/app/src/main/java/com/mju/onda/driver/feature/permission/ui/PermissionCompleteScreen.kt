package com.mju.onda.driver.feature.permission.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
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
import com.mju.onda.driver.core.ui.components.OndaLogo
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.StatusIconRow
import com.mju.onda.driver.feature.permission.data.MockPermissionComplete
import com.mju.onda.driver.feature.permission.viewmodel.PermissionCompleteEvent
import com.mju.onda.driver.feature.permission.viewmodel.PermissionCompleteViewModel

@Composable
fun PermissionCompleteScreen(
    onGoToOperation: () -> Unit,
    viewModel: PermissionCompleteViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshFromSystem(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshFromSystem(context)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PermissionCompleteEvent.NavigateToTodayOperation -> onGoToOperation()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(OndaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val minHeight = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                OndaLogo(height = 32.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = R.drawable.permission_complete_success),
                    contentDescription = "권한 설정 완료",
                    modifier = Modifier.width(200.dp),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = MockPermissionComplete.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = MockPermissionComplete.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color(0xFFA1A6B7),
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OndaColors.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, OndaColors.Border, RoundedCornerShape(12.dp)),
                ) {
                    uiState.statusItems.forEachIndexed { index, item ->
                        StatusIconRow(
                            icon = item.icon,
                            label = item.label,
                            statusText = item.statusText,
                            statusColor = if (item.isActive) {
                                OndaColors.Primary
                            } else {
                                OndaColors.TextHint
                            },
                            showDivider = index != uiState.statusItems.lastIndex,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.permission_complete_bus),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )

            OndaPrimaryButton(
                label = MockPermissionComplete.GO_TO_OPERATION_LABEL,
                onClick = viewModel::goToOperation,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                height = 52.dp,
                cornerRadius = 12.dp,
            )
        }
    }
}
