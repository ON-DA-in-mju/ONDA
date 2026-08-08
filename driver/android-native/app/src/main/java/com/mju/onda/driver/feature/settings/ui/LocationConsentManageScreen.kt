package com.mju.onda.driver.feature.settings.ui

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.ConsentStatusRow
import com.mju.onda.driver.feature.settings.data.MockLocationConsentManage
import com.mju.onda.driver.feature.settings.viewmodel.LocationConsentManageDialog
import com.mju.onda.driver.feature.settings.viewmodel.LocationConsentManageEvent
import com.mju.onda.driver.feature.settings.viewmodel.LocationConsentManageViewModel

private val InfoBannerBg = Color(0xFFEDF4FE)
private val IconSoftBg = Color(0xFFEDF4FE)
private val SuccessSoft = Color(0xFFE6F4F1)
private val SuccessText = Color(0xFF00897B)
private val RowIconSize = 28.dp
private val RowIconCircle = 34.dp

@Composable
fun LocationConsentManageScreen(
    onBack: () -> Unit,
    viewModel: LocationConsentManageViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

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
        viewModel.refresh()
        viewModel.events.collect { event ->
            when (event) {
                LocationConsentManageEvent.NavigateBack -> onBack()
                is LocationConsentManageEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (uiState.dialog) {
        LocationConsentManageDialog.PrivacyPolicy -> {
            InfoDialog(
                title = MockLocationConsentManage.PRIVACY_POLICY_LABEL,
                body = MockLocationConsentManage.PRIVACY_POLICY_BODY,
                onDismiss = viewModel::dismissDialog,
            )
        }
        LocationConsentManageDialog.ConsentGuide -> {
            InfoDialog(
                title = MockLocationConsentManage.CONSENT_GUIDE_LABEL,
                body = MockLocationConsentManage.CONSENT_GUIDE_BODY,
                onDismiss = viewModel::dismissDialog,
            )
        }
        LocationConsentManageDialog.AgreeConfirm -> {
            ConfirmDialog(
                title = MockLocationConsentManage.AGREE_DIALOG_TITLE,
                body = MockLocationConsentManage.AGREE_DIALOG_MESSAGE,
                confirmLabel = MockLocationConsentManage.AGREE_DIALOG_CONFIRM,
                onConfirm = viewModel::confirmAgree,
                onDismiss = viewModel::dismissDialog,
            )
        }
        LocationConsentManageDialog.RevokeConfirm -> {
            ConfirmDialog(
                title = MockLocationConsentManage.REVOKE_DIALOG_TITLE,
                body = MockLocationConsentManage.REVOKE_DIALOG_MESSAGE,
                confirmLabel = MockLocationConsentManage.REVOKE_DIALOG_CONFIRM,
                onConfirm = viewModel::confirmRevoke,
                onDismiss = viewModel::dismissDialog,
                destructive = true,
            )
        }
        LocationConsentManageDialog.None -> Unit
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockLocationConsentManage.SCREEN_TITLE,
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

                Spacer(modifier = Modifier.height(16.dp))

                ConsentStatusSection(
                    rows = uiState.rows,
                    statusBadge = uiState.statusBadge,
                    isConsented = uiState.isConsented,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))

                ConsentInfoBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.isConsented) {
                    OndaOutlinedButton(
                        label = MockLocationConsentManage.REVOKE_LABEL,
                        onClick = viewModel::onRevokeClick,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        height = 48.dp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OndaPrimaryButton(
                        label = MockLocationConsentManage.PRIVACY_POLICY_LABEL,
                        onClick = viewModel::onPrivacyPolicy,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        height = 48.dp,
                        fontSize = 15.sp,
                    )
                } else {
                    OndaPrimaryButton(
                        label = MockLocationConsentManage.AGREE_LABEL,
                        onClick = viewModel::onAgreeClick,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        height = 48.dp,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OndaOutlinedButton(
                        label = MockLocationConsentManage.PRIVACY_POLICY_LABEL,
                        onClick = viewModel::onPrivacyPolicy,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        height = 48.dp,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OndaOutlinedButton(
                    label = MockLocationConsentManage.CONSENT_GUIDE_LABEL,
                    onClick = viewModel::onConsentGuide,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    height = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun ConsentStatusSection(
    rows: List<ConsentStatusRow>,
    statusBadge: String,
    isConsented: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = MockLocationConsentManage.SECTION_TITLE,
                modifier = Modifier.weight(1f),
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.TextPrimary,
                ),
            )
            StatusBadge(
                label = statusBadge,
                consented = isConsented,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        rows.forEach { row ->
            ConsentStatusRowItem(row = row)
        }
    }
}

@Composable
private fun StatusBadge(label: String, consented: Boolean) {
    val bg = if (consented) SuccessSoft else Color(0xFFFDECEA)
    val fg = if (consented) SuccessText else Color(0xFFD32F2F)
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (consented) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = OndaTypography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            ),
        )
    }
}

@Composable
private fun ConsentStatusRowItem(row: ConsentStatusRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(RowIconCircle)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(RowIconSize),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = row.label,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.TextPrimary,
            ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = row.value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OndaColors.TextPrimary,
            ),
        )
    }
}

@Composable
private fun ConsentInfoBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBannerBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(RowIconSize),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            BulletLine(MockLocationConsentManage.INFO_LINE_1)
            Spacer(modifier = Modifier.height(4.dp))
            BulletLine(MockLocationConsentManage.INFO_LINE_2)
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Text(
        text = "• $text",
        style = OndaTypography.bodySmall.copy(
            fontSize = 12.5.sp,
            color = OndaColors.TextPrimary,
            lineHeight = 17.sp,
        ),
    )
}

@Composable
private fun InfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.TextPrimary,
                ),
            )
        },
        text = {
            Text(
                text = body,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = OndaColors.TextPrimary,
                    lineHeight = 20.sp,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = MockLocationConsentManage.DIALOG_CONFIRM,
                    style = OndaTypography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = OndaColors.Primary,
                    ),
                )
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OndaColors.TextPrimary,
                ),
            )
        },
        text = {
            Text(
                text = body,
                style = OndaTypography.bodyLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = OndaColors.TextPrimary,
                    lineHeight = 20.sp,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = OndaTypography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (destructive) Color(0xFFD32F2F) else OndaColors.Primary,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = MockLocationConsentManage.DIALOG_CANCEL,
                    style = OndaTypography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = OndaColors.TextSecondary,
                    ),
                )
            }
        },
    )
}
