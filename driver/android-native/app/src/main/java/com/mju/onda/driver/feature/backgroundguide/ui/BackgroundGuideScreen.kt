package com.mju.onda.driver.feature.backgroundguide.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
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
import com.mju.onda.driver.core.ui.components.InfoIconCard
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.backgroundguide.data.BackgroundGuideIcon
import com.mju.onda.driver.feature.backgroundguide.data.MockBackgroundGuide
import com.mju.onda.driver.feature.backgroundguide.viewmodel.BackgroundGuideEvent
import com.mju.onda.driver.feature.backgroundguide.viewmodel.BackgroundGuideViewModel

private val HeadlineBlue = Color(0xFF0A2A5C)
private val FooterBannerBg = Color(0xFFEAF7F5)

@Composable
fun BackgroundGuideScreen(
    onBack: () -> Unit,
    viewModel: BackgroundGuideViewModel = viewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                BackgroundGuideEvent.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockBackgroundGuide.SCREEN_TITLE,
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
                Image(
                    painter = painterResource(id = R.drawable.background_guide_illustration),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(358f / 157f),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = MockBackgroundGuide.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineBlue,
                        lineHeight = 28.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = MockBackgroundGuide.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = OndaColors.TextSecondary,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(18.dp))

                viewModel.items.forEachIndexed { index, item ->
                    InfoIconCard(
                        icon = iconFor(item.iconKind),
                        title = item.title,
                        description = item.description,
                        iconBackgroundColor = OndaColors.AccentSoft,
                        iconColor = OndaColors.Accent,
                    )
                    if (index != viewModel.items.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                FooterNotice()
                Spacer(modifier = Modifier.height(20.dp))
                OndaPrimaryButton(
                    label = MockBackgroundGuide.CONFIRM_LABEL,
                    onClick = viewModel::onConfirm,
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun FooterNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FooterBannerBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(OndaColors.AccentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.Accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = MockBackgroundGuide.FOOTER_NOTICE,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.sp,
                color = OndaColors.TextSecondary,
            ),
        )
    }
}

private fun iconFor(kind: BackgroundGuideIcon): ImageVector = when (kind) {
    BackgroundGuideIcon.ScreenLock -> Icons.Outlined.LockOpen
    BackgroundGuideIcon.NoForceStop -> Icons.Outlined.Cancel
    BackgroundGuideIcon.Battery -> Icons.Outlined.BatteryFull
}
