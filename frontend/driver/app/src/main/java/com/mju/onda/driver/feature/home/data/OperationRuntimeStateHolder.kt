package com.mju.onda.driver.feature.home.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs
import com.mju.onda.driver.core.location.LatestLocationHolder
import com.mju.onda.driver.core.system.SystemLogsApi
import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 화면 간 운행 상태·실제 시작/종료 시각 공유.
 * 계정별 SharedPreferences에 저장해 앱 재실행에도 반영.
 */
object OperationRuntimeStateHolder {
    private const val PREFS = "onda_operation_runtime"
    private const val KEY_DAY = "runtime_day"
    private const val KEY_IN_PROGRESS = "in_progress_ids"
    private const val KEY_ENDED = "ended_ids"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_ENDED_AT = "ended_at"

    /** DB sync / system_logs 기록용 (앱 프로세스 동안 유지) */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        resetIfNewDay()
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
            ?: ""

    /** 시작 완료 시 호출. pending이 없으면 활성/기본 배차. */
    fun takePendingStartId(): String {
        val id = resolveFocusedOperationId()
        pendingStartId = null
        return id
    }

    fun startOperation(operationId: String = resolveFocusedOperationId()) {
        if (operationId.isBlank()) return
        if (!canStartOperation(operationId) && !isInProgress(operationId)) {
            android.util.Log.w("OpRuntime", "start blocked (ended or not next) op=$operationId")
            return
        }
        aliasIds(operationId).forEach { endedIds.remove(it) }
        endedAtById.remove(operationId)
        inProgressIds += operationId
        if (operationId !in startedAtById) {
            startedAtById[operationId] = System.currentTimeMillis()
        }
        persist()
        com.mju.onda.driver.core.location.OperationLocationTracker.startForOperation(operationId)
        // system_logs 를 operations PATCH 와 분리 — PATCH 지연/실패해도 로그는 남긴다.
        ioScope.launch {
            android.util.Log.i("OpRuntime", "start → system_logs op=$operationId")
            logStatusChange(operationId, statusLabel = "운행 중", success = true)
        }
        ioScope.launch {
            val ok = runCatching {
                TodayAssignmentsApi.updateStatus(operationId, OperationStatus.InProgress)
            }.getOrDefault(false)
            android.util.Log.i("OpRuntime", "start → operations PATCH dbOk=$ok op=$operationId")
        }
    }

    fun endOperation(operationId: String) {
        // EndProcessing → EndComplete 로 두 번 호출되는 경우 중복 기록 방지
        if (operationId.isNotBlank() && isEnded(operationId) && !isInProgress(operationId)) {
            android.util.Log.i("OpRuntime", "end skipped (already ended) op=$operationId")
            return
        }
        endOperationInternal(operationId, syncDb = true)
    }

    /** 관리자 강제 종료 등: DB는 이미 CANCELLED인 경우 로컬만 종료 */
    fun endOperationLocally(operationId: String) {
        endOperationInternal(operationId, syncDb = false)
    }

    private fun endOperationInternal(operationId: String, syncDb: Boolean) {
        if (operationId.isBlank()) return
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
        if (syncDb) {
            ioScope.launch {
                android.util.Log.i("OpRuntime", "end → system_logs op=$operationId")
                logStatusChange(operationId, statusLabel = "운행 종료", success = true)
            }
            ioScope.launch {
                val ok = runCatching {
                    TodayAssignmentsApi.updateStatus(operationId, OperationStatus.Ended)
                }.getOrDefault(false)
                android.util.Log.i("OpRuntime", "end → operations PATCH dbOk=$ok op=$operationId")
            }
        }
    }

    private suspend fun logStatusChange(
        operationId: String,
        statusLabel: String,
        success: Boolean,
    ) {
        val op = MockTodayOperations.findById(operationId)
        val account = AccountInfoStateHolder.get()
        val vehicle = op?.vehicleName?.takeIf { it.isNotBlank() }
            ?: account.vehicleName.takeIf { it.isNotBlank() }
            ?: "미정"
        val actor = account.driverName.takeIf { it.isNotBlank() } ?: "기사님"
        val fix = LatestLocationHolder.latest
        val gpsIp = fix?.let {
            String.format(java.util.Locale.US, "%.6f,%.6f", it.latitude, it.longitude)
        }
        val logged = runCatching {
            SystemLogsApi.logOperationStatusChange(
                vehicleName = vehicle,
                statusLabel = statusLabel,
                actor = actor,
                success = success,
                gpsIp = gpsIp,
            )
        }.onFailure {
            android.util.Log.e("OpRuntime", "system_logs insert exception: ${it.message}", it)
        }.getOrDefault(false)
        if (logged) {
            android.util.Log.i(
                "OpRuntime",
                "system_logs OK op=$operationId status=$statusLabel result=${if (success) "성공" else "실패"} vehicle=$vehicle",
            )
        } else {
            android.util.Log.e(
                "OpRuntime",
                "system_logs FAIL op=$operationId status=$statusLabel dbOk=$success vehicle=$vehicle actor=$actor",
            )
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

    fun isInProgress(operationId: String): Boolean =
        aliasIds(operationId).any { it in inProgressIds }

    fun isEnded(operationId: String): Boolean {
        if (aliasIds(operationId).any { it in endedIds }) return true
        val op = MockTodayOperations.findById(operationId) ?: return false
        return op.status == OperationStatus.Ended
    }

    fun hasActiveOperation(): Boolean = inProgressIds.isNotEmpty()

    fun activeOperationId(): String? = inProgressIds.firstOrNull()

    /**
     * 서버 오늘 배차 동기화 성공 후 호출.
     * 목록에 없는(또는 이미 종료된) 로컬 "운행 중" 유령 상태를 제거해
     * 홈이 비었는데 로그아웃만 막히는 상황을 방지한다.
     */
    fun reconcileWithFetchedAssignments(operations: List<AssignedOperation>) {
        val ids = operations.flatMap { aliasIds(it) }.toSet()
        var changed = false
        val stale = inProgressIds.filter { it !in ids }
        if (stale.isNotEmpty()) {
            inProgressIds.removeAll(stale.toSet())
            changed = true
        }
        operations.forEach { op ->
            val aliases = aliasIds(op)
            if (op.status == OperationStatus.Ended || op.status == OperationStatus.Unavailable) {
                if (inProgressIds.removeAll(aliases)) {
                    changed = true
                }
                if (endedIds.addAll(aliases)) {
                    changed = true
                }
            }
        }
        if (changed) {
            if (!hasActiveOperation()) {
                com.mju.onda.driver.core.location.OperationLocationTracker.stop()
            }
            persist()
            android.util.Log.i(
                "OpRuntime",
                "reconcile: removed stale in-progress, active=${hasActiveOperation()} ops=${operations.size}",
            )
        }
    }

    /** 배차 화면과 불일치하는 운행중 플래그만 강제 해제 (로그아웃 탈출용) */
    fun clearOrphanedActiveOperations() {
        val validIds = MockTodayOperations.assignedOperations.map { it.id }.toSet()
        if (validIds.isEmpty()) {
            if (inProgressIds.isNotEmpty()) {
                inProgressIds.clear()
                persist()
                com.mju.onda.driver.core.location.OperationLocationTracker.stop()
            }
            return
        }
        reconcileWithFetchedAssignments(MockTodayOperations.assignedOperations)
    }

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
        if (isInProgress(operationId)) return false
        val ordered = MockTodayOperations.assignedOperations
        val target = ordered.find { it.matchesId(operationId) } ?: return false
        if (target.status == OperationStatus.Unavailable) return false
        if (target.status == OperationStatus.Ended) return false
        if (hasActiveOperation()) return false
        val index = ordered.indexOfFirst { it.matchesId(operationId) }
        if (index < 0) return false
        // 앞선 운행이 종료됐거나 운행 불가면 다음 배차 시작 가능
        return ordered.take(index).all {
            isEnded(it.id) || it.status == OperationStatus.Unavailable || it.status == OperationStatus.Ended
        }
    }

    fun withRuntimeStatus(operations: List<AssignedOperation>): List<AssignedOperation> {
        resetIfNewDay()
        return operations.map { op ->
            when {
                isInProgress(op.id) -> op.copy(status = OperationStatus.InProgress)
                isEnded(op.id) || op.status == OperationStatus.Ended -> op.copy(status = OperationStatus.Ended)
                op.status == OperationStatus.Unavailable -> op
                else -> op.copy(status = AssignmentStatusResolver.resolve(op))
            }
        }
    }

    /** 날짜가 바뀌면 전날 운행 시작/종료 상태를 지우고 운행 예정부터 다시 시작한다. */
    fun resetIfNewDay() {
        val today = com.mju.onda.driver.core.OndaDates.today().toString()
        val stored = prefs?.getString(KEY_DAY, null)
        if (stored == today) return
        inProgressIds.clear()
        endedIds.clear()
        startedAtById.clear()
        endedAtById.clear()
        pendingStartId = null
        prefs?.edit()?.putString(KEY_DAY, today)?.apply()
        persist()
        com.mju.onda.driver.core.location.OperationLocationTracker.stop()
        TodayAssignmentsHolder.clearForNewDay()
    }

    private fun aliasIds(operationId: String): Set<String> {
        if (operationId.isBlank()) return emptySet()
        val op = MockTodayOperations.findById(operationId)
        return if (op != null) aliasIds(op) else setOf(operationId)
    }

    private fun aliasIds(op: AssignedOperation): Set<String> = buildSet {
        add(op.id)
        if (op.dbId.isNotBlank()) add(op.dbId)
    }

    private fun persist() {
        prefs?.edit()
            ?.putString(KEY_DAY, com.mju.onda.driver.core.OndaDates.today().toString())
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
