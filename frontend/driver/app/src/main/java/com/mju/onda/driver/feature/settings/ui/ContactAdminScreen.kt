package com.mju.onda.driver.feature.settings.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.ContactInfoRow
import com.mju.onda.driver.feature.settings.data.MockContactAdmin
import com.mju.onda.driver.feature.settings.viewmodel.ContactAdminEvent
import com.mju.onda.driver.feature.settings.viewmodel.ContactAdminViewModel

private val InfoBannerBg = Color(0xFFEDF4FE)
private val IconSoftBg = Color(0xFFEDF4FE)
private val RowIconSize = 28.dp
private val RowIconCircle = 34.dp

@Composable
fun ContactAdminScreen(
    onBack: () -> Unit,
    viewModel: ContactAdminViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ContactAdminEvent.NavigateBack -> onBack()
                ContactAdminEvent.CallAdmin -> {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse(MockContactAdmin.PHONE_URI)),
                        )
                    }
                }
                ContactAdminEvent.EmailInquiry -> {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse(MockContactAdmin.EMAIL_URI)),
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockContactAdmin.SCREEN_TITLE,
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

                ContactInfoCard(
                    rows = uiState.contactRows,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                EmergencyInfoBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = viewModel::onCallAdmin,
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
                    Icon(
                        imageVector = Icons.Outlined.HeadsetMic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = MockContactAdmin.CALL_ADMIN_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            color = OndaColors.TextOnPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onEmailInquiry,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = MockContactAdmin.EMAIL_INQUIRY_LABEL,
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
private fun ContactInfoCard(
    rows: List<ContactInfoRow>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        rows.forEachIndexed { index, row ->
            ContactInfoRowItem(row = row)
            if (index < rows.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    color = OndaColors.Border.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun ContactInfoRowItem(row: ContactInfoRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIcon(icon = row.icon)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = row.label,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.TextPrimary,
            ),
        )
        Text(
            text = row.value,
            style = OndaTypography.bodyLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.Primary,
                textDecoration = if (row.underlined) TextDecoration.Underline else null,
            ),
        )
    }
}

@Composable
private fun EmergencyInfoBanner(modifier: Modifier = Modifier) {
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
            modifier = Modifier.size(RowIconSize),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = MockContactAdmin.INFO_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 17.sp,
            ),
        )
    }
}

@Composable
private fun CircleIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(RowIconCircle)
            .background(IconSoftBg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(RowIconSize),
        )
    }
}
