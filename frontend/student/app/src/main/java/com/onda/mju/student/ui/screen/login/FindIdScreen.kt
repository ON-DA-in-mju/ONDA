package com.onda.mju.student.ui.screen.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onda.mju.student.R
import com.onda.mju.student.data.auth.MockAuthRepository
import com.onda.mju.student.ui.theme.ONDAStudentTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FindIdScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onGoLoginClick: () -> Unit = {},
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var foundMaskedId by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun validate(): Boolean {
        var valid = true
        if (name.trim().isEmpty()) {
            nameError = "이름을 입력해주세요."
            valid = false
        } else {
            nameError = null
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            emailError = "학교 이메일을 입력해주세요."
            valid = false
        } else if (!isValidEmail(trimmedEmail)) {
            emailError = "이메일 형식을 확인해주세요."
            valid = false
        } else {
            emailError = null
        }
        return valid
    }

    fun attemptFind() {
        if (isLoading) return
        focusManager.clearFocus()
        if (!validate()) return

        isLoading = true
        scope.launch {
            delay(350)
            isLoading = false
            val ok = email.trim().contains(MockAuthRepository.MOCK_STUDENT_ID) ||
                email.trim().lowercase().endsWith("@mju.ac.kr")
            if (ok) {
                foundMaskedId = maskStudentId(MockAuthRepository.MOCK_STUDENT_ID)
                onShowMessage("아이디를 찾았습니다.")
            } else {
                foundMaskedId = null
                onShowMessage("일치하는 계정을 찾지 못했습니다.")
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

        Box(modifier = Modifier.fillMaxWidth()) {
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
                text = "아이디 찾기",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "가입 시 사용한 이름과\n학교 이메일로 아이디를 찾아드려요.",
                color = LoginSubtitleGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(36.dp))

            LoginAuthTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                    foundMaskedId = null
                },
                placeholder = "이름",
                leadingIcon = Icons.Filled.Person,
                isError = nameError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
            LoginFieldError(nameError)

            Spacer(modifier = Modifier.height(12.dp))

            LoginAuthTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                    foundMaskedId = null
                },
                placeholder = "학교 이메일",
                leadingIcon = Icons.Filled.Email,
                isError = emailError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { attemptFind() },
                ),
            )
            LoginFieldError(emailError)

            if (foundMaskedId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "찾은 아이디: $foundMaskedId",
                    color = LoginOndaBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LoginInfoBoxBg, RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { attemptFind() },
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
                        text = "아이디 찾기",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (foundMaskedId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGoLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LoginOndaBlue),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LoginOndaBlue,
                    ),
                ) {
                    Text(
                        text = "로그인으로 돌아가기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = LoginOndaBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "테스트용: 학교 이메일(@mju.ac.kr)을\n입력하면 아이디를 확인할 수 있어요.\n예) student@mju.ac.kr",
                color = Color.Black,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun maskStudentId(id: String): String {
    if (id.length < 4) return id
    return id.take(4) + "*".repeat(id.length - 4)
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun FindIdScreenPreview() {
    ONDAStudentTheme {
        FindIdScreen()
    }
}
