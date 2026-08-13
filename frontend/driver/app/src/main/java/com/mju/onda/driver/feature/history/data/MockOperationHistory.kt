package com.mju.onda.driver.feature.history.data

import com.mju.onda.driver.core.OndaDates
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class HistoryPeriodFilter {
    Today,
    Last7Days,
    Custom,
}

enum class HistoryResultStatus {
    Completed, // 정상 완료
    AdminEnded, // 관리자 종료
    Interrupted, // 운행 중단
}

data class HistoryRecord(
    val id: String,
    val date: LocalDate,
    val dateLabel: String,
    val routeName: String,
    val vehicleName: String,
    val plateNumber: String,
    /** 실제 출발 시각 */
    val actualDepart: String,
    /** 운행 시간 표시값 (예: 18분, 77분) */
    val durationLabel: String,
    /** 운행 구간 (예: 09:03 ~ 09:21) */
    val timeRange: String,
    val status: HistoryResultStatus,
    val origin: String = "-",
    val destination: String = "-",
    val scheduledDepart: String = "-",
    val startedAtMillis: Long = 0L,
    val endedAtMillis: Long = 0L,
) {
    val dayOfMonth: Int get() = date.dayOfMonth
}

data class HistoryDateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init {
        require(!end.isBefore(start)) { "end must be >= start" }
    }
}

/**
 * 운행 이력 화면 문구·기간 선택 규칙.
 * 목록/상세 데이터는 HistoryOperationsApi · HistoryRuntimeStateHolder 에서만 온다.
 */
object MockOperationHistory {
    const val SCREEN_TITLE = "운행 이력"

    /** 실시간 오늘 */
    val MOCK_TODAY: LocalDate
        get() = OndaDates.today()

    /** 조회 가능 시작일 = 오늘로부터 3개월 전 */
    val FIRST_TRIP_DATE: LocalDate
        get() = MOCK_TODAY.minusMonths(3)

    /** 기간 선택 최대 일수 = 조회 가능 전체 구간 (최근 3개월) */
    val MAX_CUSTOM_DAYS: Int
        get() = inclusiveDayCount(FIRST_TRIP_DATE, MOCK_TODAY).coerceAtLeast(1)

    val TODAY_DAY: Int
        get() = MOCK_TODAY.dayOfMonth

    val FIRST_TRIP_DAY: Int
        get() = FIRST_TRIP_DATE.dayOfMonth

    const val FILTER_TODAY = "오늘"
    const val FILTER_LAST_7 = "최근 7일"
    const val FILTER_CUSTOM = "기간 선택"

    const val LABEL_ACTUAL = "실제 출발"
    const val LABEL_DURATION = "운행 시간"

    const val STATUS_COMPLETED = "정상 완료"
    const val STATUS_ADMIN = "관리자 종료"
    const val STATUS_INTERRUPTED = "운행 중단"

    const val FOOTER_INFO = "최근 3개월의 운행 이력을 확인할 수 있습니다"

    const val EMPTY_TITLE = "확인할 운행 기록이 없습니다."
    const val EMPTY_SUBTITLE = "선택한 기간에 표시할 운행 이력이 없어요."
    const val EMPTY_INFO_TITLE = "운행 이력은 운행 완료 후 저장돼요"
    const val EMPTY_INFO_BODY = "운행을 완료하면 경로, 시간, 승하차 정보가 여기에 기록됩니다."
    const val EMPTY_GO_TODAY = "오늘의 운행으로"
    const val EMPTY_REFRESH = "새로고침"

    const val RANGE_CUSTOM_HINT = "기간을 선택해 주세요"

    const val PICKER_TITLE = "기간 선택"
    const val PICKER_SUBTITLE = "시작일과 종료일을 선택해 주세요 (최대 3개월)"
    const val PICKER_CONFIRM = "적용"
    const val PICKER_CANCEL = "취소"
    const val PICKER_MAX_RANGE_TOAST = "조회 기간은 최근 3개월까지 선택할 수 있습니다."
    const val PICKER_NEED_DATE_TOAST = "날짜를 선택해 주세요."

    val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

    val PICKER_HINT: String
        get() = "3개월 이전(${OndaDates.monthDayLabel(FIRST_TRIP_DATE)})은 선택할 수 없습니다"

    fun summaryText(count: Int): String = "총 ${count}건의 운행 이력이 있습니다."

    fun statusLabel(status: HistoryResultStatus): String = when (status) {
        HistoryResultStatus.Completed -> STATUS_COMPLETED
        HistoryResultStatus.AdminEnded -> STATUS_ADMIN
        HistoryResultStatus.Interrupted -> STATUS_INTERRUPTED
    }

    fun weekdayLabel(date: LocalDate): String = OndaDates.weekdayShort(date)

    fun formatDayLabel(date: LocalDate): String = OndaDates.dayLabelWithWeekday(date)

    fun formatRangeLabel(range: HistoryDateRange): String =
        if (range.start == range.end) {
            formatDayLabel(range.start)
        } else {
            "${formatDayLabel(range.start)} - ${formatDayLabel(range.end)}"
        }

    fun inclusiveDayCount(start: LocalDate, end: LocalDate): Int =
        ChronoUnit.DAYS.between(start, end).toInt() + 1

    fun isWithinSelectableBounds(date: LocalDate): Boolean =
        !date.isBefore(FIRST_TRIP_DATE) && !date.isAfter(MOCK_TODAY)

    fun isValidCustomRange(start: LocalDate, end: LocalDate): Boolean {
        if (end.isBefore(start)) return false
        if (!isWithinSelectableBounds(start) || !isWithinSelectableBounds(end)) return false
        return inclusiveDayCount(start, end) <= MAX_CUSTOM_DAYS
    }

    fun isInLast7Days(date: LocalDate): Boolean {
        val start = MOCK_TODAY.minusDays(6)
        return !date.isBefore(start) && !date.isAfter(MOCK_TODAY)
    }

    fun isToday(date: LocalDate): Boolean = date == MOCK_TODAY

    fun rangeLabel(
        filter: HistoryPeriodFilter,
        customRange: HistoryDateRange? = null,
    ): String = when (filter) {
        HistoryPeriodFilter.Today -> formatDayLabel(MOCK_TODAY)
        HistoryPeriodFilter.Last7Days ->
            formatRangeLabel(HistoryDateRange(MOCK_TODAY.minusDays(6), MOCK_TODAY))
        HistoryPeriodFilter.Custom -> customRange?.let(::formatRangeLabel) ?: RANGE_CUSTOM_HINT
    }
}
