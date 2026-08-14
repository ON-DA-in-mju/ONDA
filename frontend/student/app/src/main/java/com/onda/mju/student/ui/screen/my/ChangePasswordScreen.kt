package com.onda.mju.student.ui.screen.my

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.data.auth.AuthResult
import com.onda.mju.student.data.auth.SupabaseAuthRepository
import com.onda.mju.student.ui.screen.login.LoginAuthTextField
import com.onda.mju.student.ui.screen.login.LoginFieldError
import com.onda.mju.student.ui.screen.login.LoginOndaBlue
import kotlinx.coroutines.launch

private val TitleBlack = Color(0xFF111827)
private val BodyGray = Color(0xFF6B7280)

@Composable
fun ChangePasswordScreen(
    modifier: Modifier = Modifier,
    authRepository: SupabaseAuthRepository = remember { SupabaseAuthRepository() },
    onBackClick: () -> Unit = {},
    onChanged: () -> Unit = {},
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<String?>(null) }
    var newError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun submit() {
        var ok = true
        if (currentPassword.isEmpty()) {
            currentError = "현재 비밀번호를 입력해주세요."
            ok = false
        } else {
            currentError = null
        }
        if (newPassword.length < 6) {
            newError = "새 비밀번호는 6자 이상이어야 합니다."
            ok = false
        } else {
            newError = null
        }
        if (confirmPassword != newPassword) {
            confirmError = "새 비밀번호가 일치하지 않습니다."
            ok = false
        } else {
            confirmError = null
        }
        if (!ok) return

        focusManager.clearFocus()
        formError = null
        scope.launch {
            isSaving = true
            when (val result = authRepository.changePassword(currentPassword, newPassword)) {
                AuthResult.Success -> onChanged()
                AuthResult.InvalidCredentials -> {
                    currentError = "현재 비밀번호가 올바르지 않습니다."
                }
                is AuthResult.Failure -> {
                    formError = result.message
                }
            }
            isSaving = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(
                "비밀번호 변경",
                color = TitleBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("현재 비밀번호", color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            PasswordField(
                value = currentPassword,
                onValueChange = {
                    currentPassword = it
                    currentError = null
                    formError = null
                },
                visible = currentVisible,
                onToggleVisible = { currentVisible = !currentVisible },
                isError = currentError != null,
                imeAction = ImeAction.Next,
            )
            LoginFieldError(currentError)

            Spacer(modifier = Modifier.height(16.dp))
            Text("새 비밀번호", color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            PasswordField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    newError = null
                    formError = null
                },
                visible = newVisible,
                onToggleVisible = { newVisible = !newVisible },
                isError = newError != null,
                imeAction = ImeAction.Next,
            )
            LoginFieldError(newError)

            Spacer(modifier = Modifier.height(16.dp))
            Text("새 비밀번호 확인", color = TitleBlack, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            PasswordField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmError = null
                    formError = null
                },
                visible = confirmVisible,
                onToggleVisible = { confirmVisible = !confirmVisible },
                isError = confirmError != null,
                imeAction = ImeAction.Done,
                onDone = { submit() },
            )
            LoginFieldError(confirmError)
            LoginFieldError(formError)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "보안을 위해 현재 비밀번호 확인 후 새 비밀번호로 변경됩니다.",
                color = BodyGray,
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { if (!isSaving) submit() },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginOndaBlue,
                    contentColor = Color.White,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(22.dp),
                    )
                } else {
                    Text("변경하기", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    isError: Boolean,
    imeAction: ImeAction,
    onDone: (() -> Unit)? = null,
) {
    LoginAuthTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "비밀번호 입력",
        leadingIcon = Icons.Filled.Lock,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone?.invoke() },
        ),
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingContent = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "비밀번호 숨김" else "비밀번호 표시",
                    tint = BodyGray,
                )
            }
        },
    )
}
