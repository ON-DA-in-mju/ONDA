package com.mju.onda.driver.feature.auth.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.constants.AppStrings
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.DriverBadge
import com.mju.onda.driver.core.ui.components.OndaIllustration
import com.mju.onda.driver.core.ui.components.OndaLogo
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.core.ui.components.OndaTextField
import com.mju.onda.driver.feature.auth.viewmodel.LoginEvent
import com.mju.onda.driver.feature.auth.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onOpenFindId: () -> Unit = {},
    onOpenFindPassword: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.NavigateToLocationConsent -> onLoginSuccess()
                LoginEvent.OpenFindId -> onOpenFindId()
                LoginEvent.OpenFindPassword -> onOpenFindPassword()
                LoginEvent.ShowHelpMessage -> {
                    Toast.makeText(
                        context,
                        AppStrings.LOGIN_HELP_MESSAGE,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val minHeight = maxHeight

        Column(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OndaLogo(height = 40.dp)
            Spacer(modifier = Modifier.height(28.dp))
            DriverBadge(label = AppStrings.DRIVER_BADGE)
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = AppStrings.LOGIN_TITLE,
                style = OndaTypography.headlineLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = AppStrings.LOGIN_SUBTITLE,
                style = OndaTypography.bodySmall.copy(color = OndaColors.Subtitle),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(26.dp))

            OndaIllustration(
                drawableRes = R.drawable.login_illustration,
                contentDescription = "로그인 일러스트",
            )

            Spacer(modifier = Modifier.height(22.dp))

            OndaTextField(
                value = uiState.id,
                onValueChange = viewModel::onIdChange,
                hint = AppStrings.LOGIN_ID_HINT,
                leadingIcon = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                enabled = !uiState.isLoading,
            )
            Spacer(modifier = Modifier.height(18.dp))
            OndaTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                hint = AppStrings.LOGIN_PASSWORD_HINT,
                leadingIcon = Icons.Outlined.Lock,
                trailingIcon = if (uiState.obscurePassword) {
                    Icons.Outlined.Visibility
                } else {
                    Icons.Outlined.VisibilityOff
                },
                onTrailingClick = viewModel::togglePasswordVisibility,
                visualTransformation = if (uiState.obscurePassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    },
                ),
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !uiState.isLoading) {
                        viewModel.onAutoLoginChange(!uiState.autoLogin)
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.autoLogin,
                    onCheckedChange = viewModel::onAutoLoginChange,
                    enabled = !uiState.isLoading,
                    colors = CheckboxDefaults.colors(
                        checkedColor = OndaColors.Primary,
                        uncheckedColor = OndaColors.Border,
                    ),
                )
                Text(
                    text = AppStrings.LOGIN_AUTO_LOGIN,
                    style = OndaTypography.labelMedium.copy(
                        color = OndaColors.TextPrimary,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = OndaTypography.labelSmall.copy(
                        color = OndaColors.Error,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            OndaPrimaryButton(
                label = AppStrings.LOGIN_BUTTON,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                isLoading = uiState.isLoading,
                height = 50.dp,
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = AppStrings.LOGIN_FIND_ID,
                    color = OndaColors.Primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(enabled = !uiState.isLoading) {
                            viewModel.onFindIdClick()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color(0xFFCBD5E1)),
                )
                Text(
                    text = AppStrings.LOGIN_FIND_PASSWORD,
                    color = OndaColors.Primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(enabled = !uiState.isLoading) {
                            viewModel.onFindPasswordClick()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clickable(enabled = !uiState.isLoading) {
                        viewModel.onHelpClick()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = AppStrings.LOGIN_HELP,
                    style = OndaTypography.labelMedium,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = OndaColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))
            Text(
                text = AppStrings.APP_VERSION_LABEL,
                style = OndaTypography.labelSmall,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
        }
    }
}
