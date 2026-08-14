package com.mju.onda.driver.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mju.onda.driver.core.theme.OndaColors

@Composable
fun FindPasswordScreen(
    onBack: () -> Unit,
) {
    var account by remember { mutableStateOf("") }
    var accountError by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        focusManager.clearFocus()
        if (account.trim().isEmpty()) {
            accountError = "아이디를 입력해주세요."
            return
        }
        accountError = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 21.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        FindAccountHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "비밀번호 찾기",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "가입한 아이디를 입력하면\n재설정 안내를 보내드려요.",
                color = FindSubtitleGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(36.dp))

            FindAccountTextField(
                value = account,
                onValueChange = {
                    account = it
                    if (accountError != null) accountError = null
                },
                placeholder = "아이디",
                leadingIcon = Icons.Filled.Person,
                isError = accountError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            FindAccountFieldError(accountError)

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(59.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OndaColors.Primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "재설정 안내 받기",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        FindAccountInfoBox(
            text = "비밀번호 재설정은 관리자가 안내합니다.\n관리자 연락처 031-123-4567로\n문의해 주세요.",
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
