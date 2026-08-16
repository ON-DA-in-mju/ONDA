package com.mju.onda.driver.core

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 운행 실제 시작·종료·경과 시각 포맷.
 * 표시는 항상 Asia/Seoul. 에뮬레이터/기기 타임존(UTC 등)을 쓰지 않는다.
 */
object OperationTripClock {
    private val hmFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)

    fun formatHm(millis: Long): String {
        if (millis <= 0L) return "--:--"
        return Instant.ofEpochMilli(millis).atZone(KoreaTime.zone).format(hmFmt)
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

    /** 예: "00:18" (이력 상세 총 운행 시간). 77분은 01:17. */
    fun formatDurationHm(startMillis: Long, endMillis: Long): String =
        formatDurationHmFromMinutes(elapsedMinutes(startMillis, endMillis))

    fun formatDurationHmFromMinutes(totalMin: Int): String {
        val safe = totalMin.coerceAtLeast(0)
        val h = safe / 60
        val m = safe % 60
        return String.format(Locale.KOREA, "%02d:%02d", h, m)
    }

    /** "77분" → "01:17" */
    fun formatDurationHmFromLabel(durationLabel: String): String {
        val mins = durationLabel.removeSuffix("분").trim().toIntOrNull() ?: 0
        return formatDurationHmFromMinutes(mins)
    }

    fun formatTimeRange(startMillis: Long, endMillis: Long): String =
        "${formatHm(startMillis)} ~ ${formatHm(endMillis)}"

    fun parseInstantMillis(raw: String?): Long? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed == "null") return null
        runCatching { Instant.parse(trimmed).toEpochMilli() }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }.getOrNull()?.let { return it }
        val normalized = trimmed.replace(' ', 'T')
        runCatching { Instant.parse(normalized).toEpochMilli() }.getOrNull()?.let { return it }
        runCatching {
            OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }
        return null
    }
}
