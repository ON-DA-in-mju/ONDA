package com.mju.onda.driver.feature.consent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.InfoIconCard
import com.mju.onda.driver.core.ui.components.OndaIllustration
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.consent.data.MockLocationConsent
import com.mju.onda.driver.feature.consent.viewmodel.LocationConsentEvent
import com.mju.onda.driver.feature.consent.viewmodel.LocationConsentViewModel

@Composable
fun LocationConsentScreen(
    onAgree: () -> Unit,
    onBack: () -> Unit,
    viewModel: LocationConsentViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LocationConsentEvent.NavigateToPermissionGuide -> onAgree()
                LocationConsentEvent.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockLocationConsent.TITLE,
                onBack = viewModel::onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(modifier = Modifier.widthIn(max = 430.dp)) {
                    OndaPrimaryButton(
                        label = MockLocationConsent.AGREE_LABEL,
                        onClick = viewModel::onAgree,
                        height = 44.dp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        OndaOutlinedButton(
                            label = MockLocationConsent.DISAGREE_LABEL,
                            onClick = viewModel::onDisagreeClick,
                            modifier = Modifier.weight(1f),
                            height = 40.dp,
                            fillMaxWidth = true,
                        )
                        OndaOutlinedButton(
                            label = MockLocationConsent.DETAIL_LABEL,
                            onClick = viewModel::onDetailClick,
                            modifier = Modifier.weight(1f),
                            height = 40.dp,
                            fillMaxWidth = true,
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
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = 430.dp)) {
                OndaIllustration(
                    drawableRes = R.drawable.location_consent_illustration,
                    horizontalPadding = 8.dp,
                    contentDescription = "위치정보 이용 안내 일러스트",
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = MockLocationConsent.HEADLINE,
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.6.sp,
                        letterSpacing = (-0.2).sp,
                        color = Color(0xFF0A1E3E),
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = MockLocationConsent.SUBTITLE,
                    style = OndaTypography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.2.sp,
                        color = Color(0xFF898FA5),
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(18.dp))

                uiState.items.forEachIndexed { index, item ->
                    InfoIconCard(
                        icon = item.icon,
                        title = item.title,
                        description = item.description,
                        iconBackgroundColor = Color(0xFFE8F8F7),
                        iconColor = Color(0xFF25ACAD),
                    )
                    if (index != uiState.items.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    if (uiState.showDetailDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDetailDialog,
            title = { Text(text = MockLocationConsent.DETAIL_LABEL) },
            text = {
                Text(
                    text = MockLocationConsent.DETAIL_BODY,
                    style = OndaTypography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDetailDialog) {
                    Text("확인")
                }
            },
        )
    }

    if (uiState.showDisagreeDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDisagreeDialog,
            title = { Text(text = MockLocationConsent.DISAGREE_LABEL) },
            text = {
                Text(
                    text = MockLocationConsent.DISAGREE_MESSAGE,
                    style = OndaTypography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDisagree) {
                    Text("돌아가기")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDisagreeDialog) {
                    Text("다시 보기")
                }
            },
        )
    }
}
