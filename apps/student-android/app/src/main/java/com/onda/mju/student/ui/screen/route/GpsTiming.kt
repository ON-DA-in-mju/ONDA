package com.onda.mju.student.ui.screen.route

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Shared GPS freshness rules.
 * [LOCATION_STALE_THRESHOLD_SECONDS] matches driver-app ~90s location health check
 * and [RouteLiveScreen] connection status.
 */
const val LOCATION_STALE_THRESHOLD_SECONDS = 90L
const val HEARTBEAT_STALE_THRESHOLD_SECONDS = 90L

/** Prefer vehicle_locations.recorded_at (measurement/upload time), not created_at. */
fun parseRecordedAtInstant(recordedAt: String): Instant? {
    val trimmed = recordedAt.trim()
    if (trimmed.isEmpty()) return null
    return try {
        Instant.parse(trimmed)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(trimmed).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                ZonedDateTime.parse(trimmed).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}

fun timestampAgeSeconds(timestamp: String?, nowMillis: Long): Long? {
    if (timestamp.isNullOrBlank()) return null
    val instant = parseRecordedAtInstant(timestamp) ?: return null
    val ageMillis = (nowMillis - instant.toEpochMilli()).coerceAtLeast(0L)
    return ageMillis / 1_000L
}

fun formatLastLocationReceivedLabel(ageSeconds: Long?): String {
    if (ageSeconds == null) return "마지막 위치 수신 없음"
    return when {
        ageSeconds < 3L -> "마지막 위치 수신 방금 전"
        ageSeconds < 60L -> "마지막 위치 수신 ${ageSeconds}초 전"
        ageSeconds < 3_600L -> {
            val minutes = ageSeconds / 60L
            val seconds = ageSeconds % 60L
            if (seconds == 0L) {
                "마지막 위치 수신 ${minutes}분 전"
            } else {
                "마지막 위치 수신 ${minutes}분 ${seconds}초 전"
            }
        }
        else -> "마지막 위치 수신 1시간 이상 전"
    }
}

fun formatLastUpdatedShortLabel(ageSeconds: Long?): String {
    if (ageSeconds == null) return "위치 정보 없음"
    return when {
        ageSeconds < 60L -> "마지막 갱신 ${ageSeconds}초 전"
        ageSeconds < 3_600L -> "마지막 갱신 ${ageSeconds / 60L}분 전"
        else -> "마지막 갱신 1시간 이상 전"
    }
}
