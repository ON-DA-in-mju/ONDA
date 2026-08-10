package com.onda.mju.student.ui.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R
import com.onda.mju.student.data.auth.AuthResult
import com.onda.mju.student.data.auth.MockAuthRepository
import com.onda.mju.student.ui.theme.ONDAStudentTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    authRepository: MockAuthRepository = remember { MockAuthRepository() },
    onBackClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onFindIdClick: () -> Unit = {},
    onFindPasswordClick: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var studentIdError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun validateInputs(): Boolean {
        var valid = true
        val trimmedId = studentId.trim()

        if (trimmedId.isEmpty()) {
            studentIdError = "학번을 입력해주세요."
            valid = false
        } else if (!isValidStudentIdOrEmail(trimmedId)) {
            studentIdError = "학번 또는 이메일 형식을 확인해주세요."
            valid = false
        } else {
            studentIdError = null
        }

        if (password.isEmpty()) {
            passwordError = "비밀번호를 입력해주세요."
            valid = false
        } else if (password.length < 4) {
            passwordError = "비밀번호를 다시 확인해주세요."
            valid = false
        } else {
            passwordError = null
        }

        return valid
    }

    fun attemptLogin() {
        if (isLoading) return
        focusManager.clearFocus()
        if (!validateInputs()) return

        isLoading = true
        scope.launch {
            when (authRepository.login(studentId, password)) {
                AuthResult.Success -> {
                    isLoading = false
                    onLoginSuccess()
                }

                AuthResult.InvalidCredentials -> {
                    isLoading = false
                    studentIdError = null
                    passwordError = null
                    onShowMessage("학번 또는 비밀번호를 다시 확인해주세요.")
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 21.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.Black,
                )
            }

            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "ON-DA",
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(120.dp)
                    .height(47.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "명지대학교 계정으로\n로그인",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "학교 포털 또는 학교 이메일 계정으로\n인증해주세요.",
                color = LoginSubtitleGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(36.dp))

            LoginAuthTextField(
                value = studentId,
                onValueChange = {
                    studentId = it
                    if (studentIdError != null) studentIdError = null
                },
                placeholder = "아이디 또는 이메일",
                leadingIcon = Icons.Filled.Person,
                isError = studentIdError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
            LoginFieldError(studentIdError)

            Spacer(modifier = Modifier.height(12.dp))

            LoginAuthTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                placeholder = "비밀번호",
                leadingIcon = Icons.Filled.Lock,
                isError = passwordError != null,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingContent = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) "비밀번호 숨김" else "비밀번호 표시",
                            tint = LoginIconGray,
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { attemptLogin() },
                ),
            )
            LoginFieldError(passwordError)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { attemptLogin() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(59.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginOndaBlue,
                    contentColor = Color.White,
                    disabledContainerColor = LoginOndaBlue.copy(alpha = 0.7f),
                    disabledContentColor = Color.White,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "로그인",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "아이디 찾기",
                    color = LoginOndaBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onFindIdClick)
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
                    text = "비밀번호 찾기",
                    color = LoginOndaBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onFindPasswordClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 102.dp)
                .background(LoginInfoBoxBg, RoundedCornerShape(13.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = LoginOndaBlue,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "명지대학교 포털 계정으로 로그인하면\n재학생 인증이 완료되어\nON-DA 서비스를 이용할 수 있어요.",
                color = Color.Black,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun LoginScreenPreview() {
    ONDAStudentTheme {
        LoginScreen()
    }
}
