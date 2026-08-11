package com.mju.onda.driver.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** 배차·이력 화면용 실시간 날짜 포맷 (시각은 별도 Mock 유지) */
object OndaDates {
    private val koreaZone = ZoneId.of("Asia/Seoul")

    fun today(): LocalDate = LocalDate.now(koreaZone)

    /** 오늘의 운행 홈: "8월 7일 금요일" */
    fun homeDateLabel(date: LocalDate = today()): String {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        return "${date.monthValue}월 ${date.dayOfMonth}일 $weekday"
    }

    /** 이력 리스트: "8월 7일\n(금)" */
    fun historyListDateLabel(date: LocalDate): String =
        "${date.monthValue}월 ${date.dayOfMonth}일\n(${weekdayShort(date)})"

    /** "8월 7일(금)" */
    fun dayLabelWithWeekday(date: LocalDate): String =
        "${date.monthValue}월 ${date.dayOfMonth}일(${weekdayShort(date)})"

    /** "8월 7일" */
    fun monthDayLabel(date: LocalDate = today()): String =
        "${date.monthValue}월 ${date.dayOfMonth}일"

    fun weekdayShort(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.SUNDAY -> "일"
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
    }
}
