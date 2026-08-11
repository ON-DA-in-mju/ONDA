package com.mju.onda.driver.feature.settings.data

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 확인 대기 중인 안전 정차 요청을 주기적으로 Supabase와 동기화한다.
 * 관리자 결정이 오면 로컬 이력을 갱신하고 [updates]로 알린다.
 */
object SafeStopDecisionPoller {
    private const val TAG = "SafeStopPoller"
    private const val INTERVAL_MS = 5_000L

    data class Update(
        val requestId: String,
        val decision: String,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    private val _updates = MutableSharedFlow<Update>(extraBufferCapacity = 8)
    val updates: SharedFlow<Update> = _updates.asSharedFlow()

    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                pollOnce()
                delay(INTERVAL_MS)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    suspend fun pollOnce(): List<Update> {
        val driverKey = SupabaseClient.userUuid
            ?: SessionStateHolder.currentUserId
            ?: return emptyList()
        val pendingIds = SafeStopHistoryHolder.all()
            .filter { it.reviewStatus == SafeStopReviewStatus.Pending }
            .map { it.id }
            .toSet()
        if (pendingIds.isEmpty()) return emptyList()

        return when (val result = SafeStopApi.fetchForDriver(driverKey)) {
            is SafeStopApi.FetchResult.Ok -> {
                val decided = result.items
                    .filter { it.id in pendingIds }
                    .filter { it.decision in setOf("continue", "stop", "cancelled") }
                    .associate { it.id to it.decision }
                if (decided.isEmpty()) return emptyList()
                SafeStopHistoryHolder.applyRemoteDecisions(decided)
                val updates = decided.map { (id, decision) -> Update(id, decision) }
                updates.forEach { _updates.tryEmit(it) }
                Log.d(TAG, "applied ${updates.size} decision(s)")
                updates
            }
            SafeStopApi.FetchResult.Failed -> emptyList()
        }
    }
}
