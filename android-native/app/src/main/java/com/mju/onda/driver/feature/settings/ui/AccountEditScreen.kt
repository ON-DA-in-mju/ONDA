package com.mju.onda.driver.feature.settings.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.mju.onda.driver.core.ui.components.OndaOutlinedButton
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTextField
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.MockAccountInfo
import com.mju.onda.driver.feature.settings.viewmodel.AccountEditEvent
import com.mju.onda.driver.feature.settings.viewmodel.AccountEditViewModel

private val InfoBannerBg = Color(0xFFEDF4FE)

@Composable
fun AccountEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AccountEditViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AccountEditEvent.NavigateBack -> onBack()
                AccountEditEvent.Saved -> {
                    Toast.makeText(context, MockAccountInfo.SAVE_TOAST, Toast.LENGTH_SHORT).show()
                    onSaved()
                }
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockAccountInfo.EDIT_SCREEN_TITLE,
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

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    EditInfoBanner()
                    Spacer(modifier = Modifier.height(18.dp))

                    FieldLabel(MockAccountInfo.info.driverNameLabel)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OndaTextField(
                            value = uiState.givenName,
                            onValueChange = viewModel::onGivenNameChange,
                            hint = MockAccountInfo.NAME_HINT,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = MockAccountInfo.NAME_SUFFIX.trim(),
                            style = OndaTypography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OndaColors.TextSecondary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FieldLabel(MockAccountInfo.info.driverIdLabel)
                    Spacer(modifier = Modifier.height(6.dp))
                    OndaTextField(
                        value = uiState.driverId,
                        onValueChange = {},
                        hint = MockAccountInfo.info.driverIdLabel,
                        enabled = false,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FieldLabel(MockAccountInfo.info.orgLabel)
                    Spacer(modifier = Modifier.height(6.dp))
                    OndaTextField(
                        value = uiState.organization,
                        onValueChange = viewModel::onOrganizationChange,
                        hint = MockAccountInfo.info.orgLabel,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FieldLabel(MockAccountInfo.info.vehicleLabel)
                    Spacer(modifier = Modifier.height(6.dp))
                    OndaTextField(
                        value = uiState.vehicleName,
                        onValueChange = viewModel::onVehicleNameChange,
                        hint = MockAccountInfo.info.vehicleLabel,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    FieldLabel(MockAccountInfo.info.contactLabel)
                    Spacer(modifier = Modifier.height(6.dp))
                    OndaTextField(
                        value = uiState.contactStatus,
                        onValueChange = viewModel::onContactStatusChange,
                        hint = MockAccountInfo.info.contactLabel,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OndaPrimaryButton(
                        label = MockAccountInfo.SAVE_LABEL,
                        onClick = viewModel::onSave,
                        height = 48.dp,
                        fontSize = 15.sp,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OndaOutlinedButton(
                        label = MockAccountInfo.CANCEL_LABEL,
                        onClick = viewModel::onCancel,
                        height = 48.dp,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = OndaTypography.bodyLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = OndaColors.TextSecondary,
        ),
    )
}

@Composable
private fun EditInfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoBannerBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = OndaColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = MockAccountInfo.EDIT_BANNER,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                color = OndaColors.TextPrimary,
                lineHeight = 17.sp,
            ),
        )
    }
}
