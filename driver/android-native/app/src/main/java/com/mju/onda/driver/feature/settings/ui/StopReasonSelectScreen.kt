package com.mju.onda.driver.feature.settings.ui

import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mju.onda.driver.R
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaTopBar
import com.mju.onda.driver.feature.settings.data.MockStopReasonSelect
import com.mju.onda.driver.feature.settings.data.StopReasonItem
import com.mju.onda.driver.feature.settings.viewmodel.StopReasonSelectEvent
import com.mju.onda.driver.feature.settings.viewmodel.StopReasonSelectViewModel

private val IconSoftBg = Color(0xFFEDF4FE)
private val WarningSoft = Color(0xFFE8F8F5)
private val WarningFg = Color(0xFF1F9D8A)
private val HeadlineBlue = Color(0xFF0A2A5C)
private val SubtitleBlue = Color(0xFF6B7A90)
private val RowIconSize = 28.dp
private val RowIconCircle = 34.dp

@Composable
fun StopReasonSelectScreen(
    onBack: () -> Unit,
    onNext: (reasonLabel: String) -> Unit,
    viewModel: StopReasonSelectViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StopReasonSelectEvent.NavigateBack -> onBack()
                is StopReasonSelectEvent.ProceedNext -> onNext(event.reasonLabel)
            }
        }
    }

    Scaffold(
        containerColor = OndaColors.Background,
        topBar = {
            OndaTopBar(
                title = MockStopReasonSelect.SCREEN_TITLE,
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
                horizontalAlignment = Alignment.CenterHorizontally,
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

                Text(
                    text = MockStopReasonSelect.HEADLINE,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = OndaTypography.headlineLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HeadlineBlue,
                        textAlign = TextAlign.Center,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = MockStopReasonSelect.SUBTITLE,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = OndaTypography.bodyLarge.copy(
                        fontSize = 13.sp,
                        color = SubtitleBlue,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                ReasonListCard(
                    reasons = uiState.reasons,
                    selectedId = uiState.selectedId,
                    otherDetail = uiState.otherDetail,
                    onSelect = viewModel::onSelect,
                    onOtherDetailChange = viewModel::onOtherDetailChange,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                SafetyWarningBanner(modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = viewModel::onNext,
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
                    Text(
                        text = MockStopReasonSelect.NEXT_LABEL,
                        style = OndaTypography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OndaColors.TextOnPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = viewModel::onPrevious,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, OndaColors.Primary),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(
                        text = MockStopReasonSelect.PREV_LABEL,
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
private fun ReasonListCard(
    reasons: List<StopReasonItem>,
    selectedId: String,
    otherDetail: String,
    onSelect: (String) -> Unit,
    onOtherDetailChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OndaColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, OndaColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        reasons.forEachIndexed { index, item ->
            ReasonRow(
                item = item,
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
            )
            if (item.id == MockStopReasonSelect.OTHER_ID && selectedId == MockStopReasonSelect.OTHER_ID) {
                OtherDetailField(
                    value = otherDetail,
                    onValueChange = onOtherDetailChange,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            if (index < reasons.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    color = OndaColors.Border.copy(alpha = 0.85f),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            color = OndaColors.Border.copy(alpha = 0.85f),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = OndaColors.Primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = MockStopReasonSelect.FOOTER_INFO,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = OndaColors.TextSecondary,
                ),
            )
        }
    }
}

@Composable
private fun ReasonRow(
    item: StopReasonItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (selected) {
                    Modifier
                        .border(1.5.dp, OndaColors.Primary, RoundedCornerShape(12.dp))
                        .background(OndaColors.PrimarySoft.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconSize = RowIconSize * item.iconScale
        Box(
            modifier = Modifier
                .size(RowIconCircle)
                .background(IconSoftBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (item.iconRes != null) {
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    contentScale = ContentScale.Fit,
                )
            } else if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = OndaColors.Primary,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            style = OndaTypography.bodyLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.TextPrimary,
            ),
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = OndaColors.Primary,
                unselectedColor = OndaColors.Border,
            ),
        )
    }
}

@Composable
private fun OtherDetailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Compose BasicTextField는 Windows 에뮬레이터 + 한글 IME에서 조합창이
    // 화면 왼쪽 위로 뜨며 입력이 깨지는 경우가 있어, 네이티브 EditText를 사용합니다.
    val textColor = OndaColors.TextPrimary.toArgb()
    val hintColor = OndaColors.TextHint.toArgb()
    val hint = MockStopReasonSelect.OTHER_HINT
    val maxLength = MockStopReasonSelect.OTHER_MAX_LENGTH
    val horizontalPad = with(LocalDensity.current) { 12.dp.roundToPx() }
    val verticalPad = with(LocalDensity.current) { 12.dp.roundToPx() }
    val onValueChangeStable = remember(onValueChange) { onValueChange }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .border(1.2.dp, OndaColors.BorderFocused, RoundedCornerShape(8.dp))
            .background(OndaColors.Surface, RoundedCornerShape(8.dp)),
        factory = { context ->
            EditText(context).apply {
                setText(value)
                setSelection(text?.length ?: 0)
                setHint(hint)
                setHintTextColor(hintColor)
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                background = null
                isSingleLine = true
                maxLines = 1
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                inputType = InputType.TYPE_CLASS_TEXT
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
                filters = arrayOf(InputFilter.LengthFilter(maxLength))
                setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad)
                doAfterTextChanged { editable ->
                    onValueChangeStable(editable?.toString().orEmpty())
                }
            }
        },
        update = { editText ->
            val current = editText.text?.toString().orEmpty()
            if (current != value && !editText.isComposingText()) {
                val cursor = editText.selectionEnd.coerceIn(0, value.length)
                editText.setText(value)
                editText.setSelection(cursor)
            }
        },
    )
}

private fun EditText.isComposingText(): Boolean {
    val editable = text ?: return false
    return android.view.inputmethod.BaseInputConnection.getComposingSpanStart(editable) >= 0
}

@Composable
private fun SafetyWarningBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WarningSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.PrivacyTip,
            contentDescription = null,
            tint = WarningFg,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = MockStopReasonSelect.WARNING,
            style = OndaTypography.bodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = OndaColors.TextPrimary,
                lineHeight = 17.sp,
            ),
        )
    }
}
