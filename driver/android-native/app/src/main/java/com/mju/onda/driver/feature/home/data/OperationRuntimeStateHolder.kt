package com.mju.onda.driver.feature.home.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs

/**
 * 화면 간 운행 상태·실제 시작/종료 시각 공유.
 * 계정별 SharedPreferences에 저장해 앱 재실행에도 반영.
 */
object OperationRuntimeStateHolder {
    private const val PREFS = "onda_operation_runtime"
    private const val KEY_IN_PROGRESS = "in_progress_ids"
    private const val KEY_ENDED = "ended_ids"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_ENDED_AT = "ended_at"

    private var prefs: SharedPreferences? = null
    private val inProgressIds = linkedSetOf<String>()
    private val endedIds = linkedSetOf<String>()
    private val startedAtById = linkedMapOf<String, Long>()
    private val endedAtById = linkedMapOf<String, Long>()

    /** 운행 준비~시작 완료 플로우에서 어떤 배차를 시작할지 보관 */
    private var pendingStartId: String? = null

    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        inProgressIds.clear()
        endedIds.clear()
        startedAtById.clear()
        endedAtById.clear()
        pendingStartId = null
        inProgressIds += prefs?.getStringSet(KEY_IN_PROGRESS, emptySet()).orEmpty()
        endedIds += prefs?.getStringSet(KEY_ENDED, emptySet()).orEmpty()
        startedAtById += decodeMillisMap(prefs?.getString(KEY_STARTED_AT, null))
        endedAtById += decodeMillisMap(prefs?.getString(KEY_ENDED_AT, null))
        com.mju.onda.driver.core.location.OperationLocationTracker.syncWithRuntime()
    }

    fun unbindUser() {
        inProgressIds.clear()
        endedIds.clear()
        startedAtById.clear()
        endedAtById.clear()
        pendingStartId = null
        prefs = null
        com.mju.onda.driver.core.location.OperationLocationTracker.stop()
    }

    fun setPendingStart(operationId: String) {
        pendingStartId = operationId
    }

    fun peekPendingStartId(): String? = pendingStartId

    /**
     * 준비·시작 플로우 / 운행 중 화면에서 보여줄 배차.
     * pending(준비~시작) → 진행 중 → 목록 첫 배차 순.
     */
    fun resolveFocusedOperationId(): String =
        pendingStartId
            ?: activeOperationId()
            ?: MockTodayOperations.assignedOperations.firstOrNull()?.id
            ?: MockTodayOperations.forUser(null).first().id

    /** 시작 완료 시 호출. pending이 없으면 활성/기본 배차. */
    fun takePendingStartId(): String {
        val id = resolveFocusedOperationId()
        pendingStartId = null
        return id
    }

    fun startOperation(operationId: String = resolveFocusedOperationId()) {
        endedIds.remove(operationId)
        endedAtById.remove(operationId)
        inProgressIds += operationId
        if (operationId !in startedAtById) {
            startedAtById[operationId] = System.currentTimeMillis()
        }
        persist()
        com.mju.onda.driver.core.location.OperationLocationTracker.startForOperation(operationId)
    }

    fun endOperation(operationId: String) {
        inProgressIds.remove(operationId)
        endedIds += operationId
        if (operationId !in startedAtById) {
            startedAtById[operationId] = System.currentTimeMillis()
        }
        if (operationId !in endedAtById) {
            endedAtById[operationId] = System.currentTimeMillis()
        }
        persist()
        if (!hasActiveOperation()) {
            com.mju.onda.driver.core.location.OperationLocationTracker.stop()
        }
    }

    fun clearAll() {
        inProgressIds.clear()
        endedIds.clear()
        startedAtById.clear()
        endedAtById.clear()
        pendingStartId = null
        persist()
        com.mju.onda.driver.core.location.OperationLocationTracker.stop()
    }

    fun isInProgress(operationId: String): Boolean = operationId in inProgressIds

    fun isEnded(operationId: String): Boolean = operationId in endedIds

    fun hasActiveOperation(): Boolean = inProgressIds.isNotEmpty()

    fun activeOperationId(): String? = inProgressIds.firstOrNull()

    fun startedAtMillis(operationId: String): Long? =
        startedAtById[operationId]?.takeIf { it > 0L }

    fun endedAtMillis(operationId: String): Long? =
        endedAtById[operationId]?.takeIf { it > 0L }

    /**
     * 진행 중인데 시작 시각이 없으면(구버전 상태) 지금 시각으로 보정.
     * 없으면 0.
     */
    fun ensureStartedAt(operationId: String): Long {
        startedAtById[operationId]?.takeIf { it > 0L }?.let { return it }
        if (isInProgress(operationId) || pendingStartId == operationId) {
            val now = System.currentTimeMillis()
            startedAtById[operationId] = now
            persist()
            return now
        }
        return 0L
    }

    /**
     * 배차 순서상 앞선 운행이 모두 종료된 경우에만 시작 가능.
     * 이미 종료됐거나 다른 운행이 진행 중이면 불가.
     */
    fun canStartOperation(operationId: String): Boolean {
        if (isEnded(operationId)) return false
        if (hasActiveOperation()) return false
        val ordered = MockTodayOperations.assignedOperations
        val index = ordered.indexOfFirst { it.id == operationId }
        if (index < 0) return false
        return ordered.take(index).all { isEnded(it.id) }
    }

    fun withRuntimeStatus(operations: List<AssignedOperation>): List<AssignedOperation> =
        operations.map { op ->
            val baseStatus = MockTodayOperations.assignedOperations
                .find { it.id == op.id }?.status
                ?: op.status
            when {
                isInProgress(op.id) -> op.copy(status = OperationStatus.InProgress)
                isEnded(op.id) -> op.copy(status = OperationStatus.Ended)
                else -> op.copy(status = baseStatus)
            }
        }

    private fun persist() {
        prefs?.edit()
            ?.putStringSet(KEY_IN_PROGRESS, inProgressIds.toSet())
            ?.putStringSet(KEY_ENDED, endedIds.toSet())
            ?.putString(KEY_STARTED_AT, encodeMillisMap(startedAtById))
            ?.putString(KEY_ENDED_AT, encodeMillisMap(endedAtById))
            ?.apply()
    }

    private fun encodeMillisMap(map: Map<String, Long>): String =
        map.entries.joinToString(";") { "${it.key}=${it.value}" }

    private fun decodeMillisMap(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return buildMap {
            raw.split(";").forEach { token ->
                val parts = token.split("=")
                if (parts.size != 2) return@forEach
                val millis = parts[1].toLongOrNull() ?: return@forEach
                put(parts[0], millis)
            }
        }
    }
}
