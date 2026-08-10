package com.onda.mju.student.ui.screen.login

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LoginOndaBlue = Color(0xFF0041F1)
internal val LoginFieldBorder = Color(0xFFDFE4E9)
internal val LoginErrorBorder = Color(0xFFE11D48)
internal val LoginPlaceholderGray = Color(0xFF767F8E)
internal val LoginSubtitleGray = Color(0xFF445066)
internal val LoginIconGray = Color(0xFF9AA3B2)
internal val LoginInfoBoxBg = Color(0xFFEDF4FE)
internal val LoginErrorText = Color(0xFFE11D48)

@Composable
internal fun LoginAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val borderColor = if (isError) LoginErrorBorder else LoginFieldBorder

    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = LoginIconGray,
            )
        },
        trailingIcon = trailingContent,
        placeholder = {
            Text(
                text = placeholder,
                color = LoginPlaceholderGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            errorContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            cursorColor = LoginOndaBlue,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
    )
}

@Composable
internal fun LoginFieldError(message: String?) {
    Text(
        text = message.orEmpty(),
        color = LoginErrorText,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp)
            .height(18.dp),
    )
}

internal fun isValidStudentIdOrEmail(value: String): Boolean {
    val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    val studentIdPattern = Regex("^\\d{8}$")
    return emailPattern.matches(value) || studentIdPattern.matches(value)
}

internal fun isValidEmail(value: String): Boolean {
    val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return emailPattern.matches(value)
}
