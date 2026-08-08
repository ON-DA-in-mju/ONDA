package com.mju.onda.driver.feature.history.data

import com.mju.onda.driver.core.OndaDates
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
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
    /** 실제 출발 시각 (예정 대비 약간의 오차) — 당분간 Mock 시각 유지 */
    val actualDepart: String,
    /** 운행 시간 표시값 (예: 18분, 42분) */
    val durationLabel: String,
    /** 운행 구간 (예: 09:03 ~ 09:21) */
    val timeRange: String,
    val status: HistoryResultStatus,
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
 * 시드 이력 날짜는 기기 오늘 기준으로 상대 배치한다.
 * 운행 시각(HH:mm)은 Mock 값을 유지한다.
 */
object MockOperationHistory {
    const val SCREEN_TITLE = "운행 이력"

    const val MAX_CUSTOM_DAYS = 7

    /** 실시간 오늘 */
    val MOCK_TODAY: LocalDate
        get() = OndaDates.today()

    /** 시드 첫 운행일 = 오늘로부터 7일 전 */
    val FIRST_TRIP_DATE: LocalDate
        get() = MOCK_TODAY.minusDays(7)

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
    const val PICKER_SUBTITLE = "시작일과 종료일을 선택해 주세요 (최대 7일)"
    const val PICKER_CONFIRM = "적용"
    const val PICKER_CANCEL = "취소"
    const val PICKER_MAX_RANGE_TOAST = "조회 기간은 최대 7일까지 선택할 수 있습니다."
    const val PICKER_NEED_DATE_TOAST = "날짜를 선택해 주세요."

    val WEEKDAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

    val PICKER_HINT: String
        get() = "첫 운행일(${OndaDates.monthDayLabel(FIRST_TRIP_DATE)}) 이전은 선택할 수 없습니다"

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

    private const val PLATE_DRIVER01 = "12가 3456"
    private const val PLATE_DRIVER02 = "34나 5678"

    fun plateForCurrentUser(): String =
        if (SessionStateHolder.currentUserId == "user02") PLATE_DRIVER02 else PLATE_DRIVER01

    private data class SeedDef(
        val id: String,
        val daysAgo: Long,
        val routeName: String,
        val vehicleName: String,
        val plateNumber: String,
        val actualDepart: String,
        val durationLabel: String,
        val timeRange: String,
        val status: HistoryResultStatus,
    )

    private fun toRecord(def: SeedDef): HistoryRecord {
        val date = MOCK_TODAY.minusDays(def.daysAgo)
        return HistoryRecord(
            id = def.id,
            date = date,
            dateLabel = OndaDates.historyListDateLabel(date),
            routeName = def.routeName,
            vehicleName = def.vehicleName,
            plateNumber = def.plateNumber,
            actualDepart = def.actualDepart,
            durationLabel = def.durationLabel,
            timeRange = def.timeRange,
            status = def.status,
        )
    }

    /** user01 시드 — 오늘~7일 전 (시각은 Mock 유지) */
    private val driver01SeedDefs: List<SeedDef> = listOf(
        SeedDef("h1", 1, "기흥역 통학버스", "2호차", PLATE_DRIVER01, "09:03", "18분", "09:03 ~ 09:21", HistoryResultStatus.Completed),
        SeedDef("h2", 2, "명지대역 셔틀", "1호차", PLATE_DRIVER01, "10:00", "42분", "10:00 ~ 10:42", HistoryResultStatus.AdminEnded),
        SeedDef("h3", 3, "시내 셔틀", "3호차", PLATE_DRIVER01, "14:00", "12분", "14:00 ~ 14:12", HistoryResultStatus.Interrupted),
        SeedDef("h4", 4, "기흥역 통학버스", "2호차", PLATE_DRIVER01, "09:04", "17분", "09:04 ~ 09:21", HistoryResultStatus.Completed),
        SeedDef("h5", 5, "명지대역 셔틀", "1호차", PLATE_DRIVER01, "10:01", "41분", "10:01 ~ 10:42", HistoryResultStatus.Completed),
        SeedDef("h6", 6, "시내 셔틀", "3호차", PLATE_DRIVER01, "14:01", "4분", "14:01 ~ 14:05", HistoryResultStatus.Interrupted),
        SeedDef("h7", 7, "기흥역 통학버스", "2호차", PLATE_DRIVER01, "09:02", "19분", "09:02 ~ 09:21", HistoryResultStatus.Completed),
        SeedDef("h8", 7, "명지대역 셔틀", "1호차", PLATE_DRIVER01, "09:58", "40분", "09:58 ~ 10:38", HistoryResultStatus.Completed),
    )

    private val driver02SeedDefs: List<SeedDef> = listOf(
        SeedDef("h1", 1, "기흥역 통학버스", "1호차", PLATE_DRIVER02, "08:38", "28분", "08:38 ~ 09:06", HistoryResultStatus.Completed),
        SeedDef("h2", 2, "명지대역 셔틀", "1호차", PLATE_DRIVER02, "11:08", "36분", "11:08 ~ 11:44", HistoryResultStatus.AdminEnded),
        SeedDef("h3", 3, "시내 셔틀", "4호차", PLATE_DRIVER02, "14:22", "15분", "14:22 ~ 14:37", HistoryResultStatus.Interrupted),
        SeedDef("h4", 4, "기흥역 통학버스", "1호차", PLATE_DRIVER02, "08:39", "29분", "08:39 ~ 09:08", HistoryResultStatus.Completed),
        SeedDef("h5", 5, "명지대역 셔틀", "1호차", PLATE_DRIVER02, "11:11", "33분", "11:11 ~ 11:44", HistoryResultStatus.Completed),
        SeedDef("h6", 6, "시내 셔틀", "4호차", PLATE_DRIVER02, "14:21", "6분", "14:21 ~ 14:27", HistoryResultStatus.Interrupted),
        SeedDef("h7", 7, "기흥역 통학버스", "1호차", PLATE_DRIVER02, "08:37", "30분", "08:37 ~ 09:07", HistoryResultStatus.Completed),
        SeedDef("h8", 7, "명지대역 셔틀", "1호차", PLATE_DRIVER02, "11:09", "34분", "11:09 ~ 11:43", HistoryResultStatus.Completed),
    )

    /** 시드 전체. 로그인 계정별. */
    val allSeedRecords: List<HistoryRecord>
        get() {
            val defs = when (SessionStateHolder.currentUserId) {
                "user02" -> driver02SeedDefs
                else -> driver01SeedDefs
            }
            return defs.map(::toRecord)
        }

    fun isInLast7Days(date: LocalDate): Boolean {
        val start = MOCK_TODAY.minusDays(6)
        return !date.isBefore(start) && !date.isAfter(MOCK_TODAY)
    }

    fun isToday(date: LocalDate): Boolean = date == MOCK_TODAY

    fun forFilter(
        filter: HistoryPeriodFilter,
        runtimeRecords: List<HistoryRecord> = emptyList(),
        customRange: HistoryDateRange? = null,
    ): List<HistoryRecord> {
        val merged = (runtimeRecords + allSeedRecords)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<HistoryRecord> { it.date }
                    .thenByDescending { minutesOf(it.actualDepart) },
            )
        return when (filter) {
            HistoryPeriodFilter.Today -> merged.filter { isToday(it.date) }
            HistoryPeriodFilter.Last7Days -> merged.filter { isInLast7Days(it.date) }
            HistoryPeriodFilter.Custom -> {
                val range = customRange ?: return emptyList()
                merged.filter { !it.date.isBefore(range.start) && !it.date.isAfter(range.end) }
            }
        }
    }

    fun rangeLabel(
        filter: HistoryPeriodFilter,
        customRange: HistoryDateRange? = null,
    ): String = when (filter) {
        HistoryPeriodFilter.Today -> formatDayLabel(MOCK_TODAY)
        HistoryPeriodFilter.Last7Days ->
            formatRangeLabel(HistoryDateRange(MOCK_TODAY.minusDays(6), MOCK_TODAY))
        HistoryPeriodFilter.Custom -> customRange?.let(::formatRangeLabel) ?: RANGE_CUSTOM_HINT
    }

    private fun minutesOf(time: String): Int {
        val parts = time.split(":")
        if (parts.size < 2) return 0
        val hour = parts[0].toIntOrNull() ?: return 0
        val minute = parts[1].toIntOrNull() ?: return 0
        return hour * 60 + minute
    }
}
