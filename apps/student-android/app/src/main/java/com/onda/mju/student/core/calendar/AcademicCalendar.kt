package com.onda.mju.student.core.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 명지대 학기/방학 + 주말·공휴일 판별.
 * - 1학기: 3월 1일부터 19주
 * - 2학기: 9월 1일부터 19주
 * - 그 외: 방학
 */
object AcademicCalendar {
    const val SEMESTER_WEEKS = 19
    private val koreaZone = ZoneId.of("Asia/Seoul")
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val fixedHolidaysMd = listOf(
        1 to 1,
        3 to 1,
        5 to 5,
        6 to 6,
        8 to 15,
        10 to 3,
        10 to 9,
        12 to 25,
    )

    private val movableHolidays = setOf(
        "2026-02-16", "2026-02-17", "2026-02-18",
        "2026-05-24",
        "2026-09-24", "2026-09-25", "2026-09-26",
    )

    enum class SemesterType { SEMESTER, VACATION }

    fun todayDateKey(): String = LocalDate.now(koreaZone).format(dateFmt)

    fun semesterForDate(input: String): SemesterType {
        val key = input.take(10)
        val year = key.substring(0, 4).toInt()
        val springStart = LocalDate.of(year, 3, 1)
        val springEnd = springStart.plusWeeks(SEMESTER_WEEKS.toLong()).minusDays(1)
        val fallStart = LocalDate.of(year, 9, 1)
        val fallEnd = fallStart.plusWeeks(SEMESTER_WEEKS.toLong()).minusDays(1)
        val date = LocalDate.parse(key)
        return if ((!date.isBefore(springStart) && !date.isAfter(springEnd)) ||
            (!date.isBefore(fallStart) && !date.isAfter(fallEnd))
        ) {
            SemesterType.SEMESTER
        } else {
            SemesterType.VACATION
        }
    }

    fun isKoreanPublicHoliday(input: String): Boolean {
        val key = input.take(10)
        if (movableHolidays.contains(key)) return true
        val date = LocalDate.parse(key)
        return fixedHolidaysMd.any { (m, d) -> date.monthValue == m && date.dayOfMonth == d }
    }

    fun isWeekendDate(input: String): Boolean {
        val date = LocalDate.parse(input.take(10))
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }

    fun isCityVacationServiceDay(input: String): Boolean =
        semesterForDate(input) == SemesterType.VACATION ||
            isWeekendDate(input) ||
            isKoreanPublicHoliday(input)

    fun isSemesterWeekday(input: String): Boolean =
        semesterForDate(input) == SemesterType.SEMESTER &&
            !isWeekendDate(input) &&
            !isKoreanPublicHoliday(input)
}
