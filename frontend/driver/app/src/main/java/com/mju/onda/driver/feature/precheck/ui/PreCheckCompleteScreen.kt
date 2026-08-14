package com.mju.onda.driver.feature.precheck.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.precheck.data.CompletedCheckItem
import com.mju.onda.driver.feature.precheck.data.MockPreCheckComplete
import com.mju.onda.driver.feature.precheck.viewmodel.PreCheckCompleteEvent
import com.mju.onda.driver.feature.precheck.viewmodel.PreCheckCompleteViewModel

@Composable
fun PreCheckCompleteScreen(
    onBack: () -> Unit,
    onStartOperation: () -> Unit = {},
    viewModel: PreCheckCompleteViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = OndaColors.Accent

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PreCheckCompleteEvent.NavigateBack -> onBack()
                PreCheckCompleteEvent.OpenStartConfirm -> onStartOperation()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockPreCheckComplete.SCREEN_TITLE,
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.precheck_complete_success),
                    contentDescription = null,
                    modifier = Modifier
                        .height(96.dp)
                        .aspectRatio(140f / 89f),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = MockPreCheckComplete.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = MockPreCheckComplete.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OndaColors.Surface, RoundedCornerShape(16.dp))
                        .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    uiState.items.forEachIndexed { index, item ->
                        CompletedCheckRow(item = item, accent = accent)
                        if (index != uiState.items.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(OndaColors.Border.copy(alpha = 0.65f)),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Image(
                    painter = painterResource(id = R.drawable.precheck_complete_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 163f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OndaPrimaryButton(
                    label = MockPreCheckComplete.START_LABEL,
                    onClick = viewModel::onStartOperation,
                    height = 48.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OndaOutlinedButton(
                    label = MockPreCheckComplete.BACK_LABEL,
                    onClick = viewModel::onBack,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun CompletedCheckRow(
    item: CompletedCheckItem,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = item.label,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                color = OndaColors.TextPrimary,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = item.detail,
            style = OndaTypography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
    }
}
