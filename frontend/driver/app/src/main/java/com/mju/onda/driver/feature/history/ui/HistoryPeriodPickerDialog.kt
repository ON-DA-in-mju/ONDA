package com.mju.onda.driver.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mju.onda.driver.core.theme.OndaColors
import com.mju.onda.driver.core.theme.OndaTypography
import com.mju.onda.driver.core.ui.components.OndaPrimaryButton
import com.mju.onda.driver.feature.history.data.MockOperationHistory
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HistoryPeriodPickerDialog(
    visibleMonth: LocalDate,
    draftStart: LocalDate?,
    draftEnd: LocalDate?,
    draftRangeLabel: String,
    onDismiss: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
) {
    val month = remember(visibleMonth) { YearMonth.from(visibleMonth) }
    val days = remember(month) { buildCalendarDays(month) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OndaColors.Surface, RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(OndaColors.PrimarySoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = OndaColors.Primary,
                        modifier = Modifier.size(33.dp),
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = MockOperationHistory.PICKER_TITLE,
                        style = OndaTypography.titleLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = MockOperationHistory.PICKER_SUBTITLE,
                        style = OndaTypography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = OndaColors.TextSecondary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEDF4FE), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = draftRangeLabel,
                    style = OndaTypography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OndaColors.Primary,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${month.year}년 ${month.monthValue}월",
                style = OndaTypography.titleLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MockOperationHistory.WEEKDAY_LABELS.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = OndaTypography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = OndaColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    week.forEach { day ->
                        CalendarDayCell(
                            date = day,
                            month = month,
                            draftStart = draftStart,
                            draftEnd = draftEnd,
                            onClick = onDayClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = MockOperationHistory.PICKER_HINT,
                style = OndaTypography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = OndaColors.TextSecondary,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OndaPrimaryButton(
                label = MockOperationHistory.PICKER_CONFIRM,
                onClick = onConfirm,
                enabled = draftStart != null,
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            ) {
                Text(
                    text = MockOperationHistory.PICKER_CANCEL,
                    style = OndaTypography.labelLarge.copy(
                        fontSize = 14.sp,
                        color = OndaColors.TextSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    month: YearMonth,
    draftStart: LocalDate?,
    draftEnd: LocalDate?,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (date == null) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    val inMonth = YearMonth.from(date) == month
    val withinBounds = MockOperationHistory.isWithinSelectableBounds(date)
    val maxEnd = draftStart?.plusDays((MockOperationHistory.MAX_CUSTOM_DAYS - 1).toLong())
    val selectingEnd = draftStart != null && draftEnd == null
    val withinMaxFromStart = when {
        !selectingEnd -> true
        date.isBefore(draftStart) -> true // 새 시작으로 허용
        maxEnd == null -> true
        else -> !date.isAfter(maxEnd)
    }
    val enabled = inMonth && withinBounds && withinMaxFromStart

    val rangeEnd = draftEnd ?: draftStart
    val inSelectedRange = draftStart != null && rangeEnd != null &&
        !date.isBefore(draftStart) && !date.isAfter(rangeEnd)
    val isEndpoint = date == draftStart || date == draftEnd ||
        (draftStart != null && draftEnd == null && date == draftStart)

    val textColor = when {
        !enabled -> OndaColors.TextSecondary.copy(alpha = 0.35f)
        isEndpoint -> OndaColors.TextOnPrimary
        inSelectedRange -> OndaColors.Primary
        else -> OndaColors.TextPrimary
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isEndpoint -> OndaColors.Primary
                    inSelectedRange -> OndaColors.PrimarySoft
                    else -> Color.Transparent
                },
            )
            .then(
                if (date == MockOperationHistory.MOCK_TODAY && !isEndpoint) {
                    Modifier.border(1.dp, OndaColors.Primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled) { onClick(date) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = OndaTypography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = if (isEndpoint || inSelectedRange) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
            ),
        )
    }
}

/** 일요일 시작 주간 그리드. 해당 월이 아닌 칸은 null. */
private fun buildCalendarDays(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value % 7 // Sunday=0 ... Saturday=6
    val daysInMonth = month.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()
    repeat(leading) { cells += null }
    for (day in 1..daysInMonth) {
        cells += month.atDay(day)
    }
    while (cells.size % 7 != 0) {
        cells += null
    }
    return cells
}
