package com.mju.onda.driver.core

import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 운행 실제 시작·종료·경과 시각 포맷 (기기 시계 기준) */
object OperationTripClock {

    fun formatHm(millis: Long): String {
        if (millis <= 0L) return "--:--"
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(
            Locale.KOREA,
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        )
    }

    fun elapsedMillis(startMillis: Long, endMillis: Long = System.currentTimeMillis()): Long {
        if (startMillis <= 0L) return 0L
        return (endMillis - startMillis).coerceAtLeast(0L)
    }

    fun elapsedMinutes(startMillis: Long, endMillis: Long = System.currentTimeMillis()): Int =
        TimeUnit.MILLISECONDS.toMinutes(elapsedMillis(startMillis, endMillis)).toInt()

    /** 예: "18분" */
    fun formatElapsedMinutes(startMillis: Long, endMillis: Long = System.currentTimeMillis()): String =
        "${elapsedMinutes(startMillis, endMillis)}분"

    /** 예: "00:18:05" */
    fun formatDurationHms(startMillis: Long, endMillis: Long = System.currentTimeMillis()): String {
        val totalSec = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis(startMillis, endMillis))
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.KOREA, "%02d:%02d:%02d", h, m, s)
    }

    /** 예: "00:18" (이력 상세 총 운행 시간) */
    fun formatDurationHm(startMillis: Long, endMillis: Long): String {
        val totalMin = elapsedMinutes(startMillis, endMillis)
        val h = totalMin / 60
        val m = totalMin % 60
        return String.format(Locale.KOREA, "%02d:%02d", h, m)
    }

    fun formatTimeRange(startMillis: Long, endMillis: Long): String =
        "${formatHm(startMillis)} ~ ${formatHm(endMillis)}"
}
