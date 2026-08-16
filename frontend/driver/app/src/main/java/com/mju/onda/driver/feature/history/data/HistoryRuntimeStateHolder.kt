package com.mju.onda.driver.feature.history.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.OndaDates
import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.core.UserScopedPrefs
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder

/**
 * 실제로 종료한 운행을 이력에 반영 (날짜·시각은 기기 기준).
 * 계정별로 분리 저장. 데모 초기화 시 함께 클리어.
 */
object HistoryRuntimeStateHolder {
    private const val PREFS = "onda_operation_history_runtime"
    private const val KEY_COMPLETED = "completed_ops" // "opId:STATUS,..."

    private var prefs: SharedPreferences? = null
    /** operationId → result status */
    private val completed = linkedMapOf<String, HistoryResultStatus>()

    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        completed.clear()
        val raw = prefs?.getString(KEY_COMPLETED, "").orEmpty()
        if (raw.isBlank()) return
        raw.split(",").forEach { token ->
            val parts = token.split(":")
            if (parts.size != 2) return@forEach
            val status = runCatching { HistoryResultStatus.valueOf(parts[1]) }.getOrNull()
                ?: return@forEach
            completed[parts[0]] = status
        }
    }

    fun unbindUser() {
        completed.clear()
        prefs = null
    }

    fun recordNormalEnd(operationId: String) {
        completed[operationId] = HistoryResultStatus.Completed
        persist()
    }

    fun recordAdminEnd(operationId: String) {
        completed[operationId] = HistoryResultStatus.AdminEnded
        persist()
    }

    fun recordInterruptedEnd(operationId: String) {
        completed[operationId] = HistoryResultStatus.Interrupted
        persist()
    }

    fun clearAll() {
        completed.clear()
        persist()
    }

    fun runtimeRecords(): List<HistoryRecord> {
        val seen = linkedSetOf<String>()
        return completed.mapNotNull { (operationId, status) ->
            val record = toHistoryRecord(operationId, status) ?: return@mapNotNull null
            val key = record.id.removePrefix("runtime-")
            if (!seen.add(key)) return@mapNotNull null
            record
        }
    }

    private fun toHistoryRecord(
        operationId: String,
        status: HistoryResultStatus,
    ): HistoryRecord? {
        val op = MockTodayOperations.findById(operationId) ?: return null
        val start = OperationRuntimeStateHolder.startedAtMillis(operationId) ?: return null
        val end = OperationRuntimeStateHolder.endedAtMillis(operationId)
            ?: System.currentTimeMillis()
        val canonicalId = op.dbId.ifBlank { op.id }
        val date = OndaDates.today()
        return HistoryRecord(
            id = "runtime-$canonicalId",
            date = date,
            dateLabel = OndaDates.historyListDateLabel(date),
            routeName = op.routeName,
            vehicleName = op.vehicleName,
            plateNumber = op.plateNumber.trim().ifBlank { "-" },
            actualDepart = OperationTripClock.formatHm(start),
            durationLabel = OperationTripClock.formatElapsedMinutes(start, end),
            timeRange = OperationTripClock.formatTimeRange(start, end),
            status = status,
            origin = op.origin.ifBlank { "-" },
            destination = op.destination.ifBlank { "-" },
            scheduledDepart = op.departTime.ifBlank { "-" },
            startedAtMillis = start,
            endedAtMillis = end,
        )
    }

    private fun persist() {
        val encoded = completed.entries.joinToString(",") { "${it.key}:${it.value.name}" }
        prefs?.edit()?.putString(KEY_COMPLETED, encoded)?.apply()
    }
}
