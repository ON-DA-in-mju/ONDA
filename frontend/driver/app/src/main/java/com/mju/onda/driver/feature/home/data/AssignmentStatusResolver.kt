package com.mju.onda.driver.feature.home.data

import com.mju.onda.driver.core.KoreaTime
import java.time.Instant

/**
 * 출발 시각 기준 표시 상태.
 * 운행 중/종료가 아니면, 출발 N분 전부터 「곧 출발」, 그 외 「운행 예정」.
 */
object AssignmentStatusResolver {
    const val DEPARTING_SOON_MINUTES = 10

    fun resolve(operation: AssignedOperation, nowMillis: Long = System.currentTimeMillis()): OperationStatus {
        when (operation.status) {
            OperationStatus.InProgress,
            OperationStatus.Ended,
            OperationStatus.Unavailable,
            OperationStatus.Waiting,
            -> return operation.status
            else -> Unit
        }

        val depart = parseHm(operation.departTime) ?: return OperationStatus.Scheduled
        val now = Instant.ofEpochMilli(nowMillis).atZone(KoreaTime.zone).toLocalTime()
        val nowMinutes = now.hour * 60 + now.minute
        val minutesUntil = depart - nowMinutes
        // 출발 전 0~10분만 「곧 출발」 (이미 지난 시각은 운행 예정 유지)
        return if (minutesUntil in 0..DEPARTING_SOON_MINUTES) {
            OperationStatus.DepartingSoon
        } else {
            OperationStatus.Scheduled
        }
    }

    fun apply(operations: List<AssignedOperation>, nowMillis: Long = System.currentTimeMillis()): List<AssignedOperation> =
        operations.map { op -> op.copy(status = resolve(op, nowMillis)) }

    private fun parseHm(value: String): Int? {
        val parts = value.split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
