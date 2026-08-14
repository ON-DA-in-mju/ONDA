package com.mju.onda.driver.feature.adminforceend.data

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import com.mju.onda.driver.feature.alarm.data.AlarmCategory
import com.mju.onda.driver.feature.alarm.data.LocalAlarmStore
import com.mju.onda.driver.feature.alarm.data.OperationAlarm
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import java.net.URLEncoder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
import org.json.JSONArray

/**
 * 관리자가 operations.status 를 CANCELLED 로 바꾸면,
 * 운행 중인 기사앱이 감지해 강제 종료 알림을 띄운다.
 */
object AdminForceEndPoller {
    private const val TAG = "AdminForceEndPoller"
    private const val INTERVAL_MS = 8_000L

    private val uuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )
    private val hmFmt = DateTimeFormatter.ofPattern("HH:mm")

    data class PendingForceEnd(
        val operationId: String,
        val reason: String,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null
    private val handled = linkedSetOf<String>()

    private val _pending = MutableSharedFlow<PendingForceEnd>(extraBufferCapacity = 4)
    val pending: SharedFlow<PendingForceEnd> = _pending.asSharedFlow()

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

    fun resetSession() {
        handled.clear()
    }

    suspend fun pollOnce() {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) return
        val activeId = OperationRuntimeStateHolder.activeOperationId() ?: return
        if (activeId in handled) return

        val status = fetchStatus(activeId) ?: return
        if (status != "CANCELLED") return

        handled += activeId
        val reason = "관리자 운행 중단"
        addLocalAlarm(activeId, reason)
        _pending.tryEmit(PendingForceEnd(operationId = activeId, reason = reason))
        Log.d(TAG, "force-end detected for $activeId")
    }

    private fun addLocalAlarm(operationId: String, reason: String) {
        val now = LocalTime.now().format(hmFmt)
        LocalAlarmStore.addAlarm(
            OperationAlarm(
                id = "force-end-$operationId",
                title = "운행 강제 종료",
                body = "관리자에 의해 운행이 곧 종료됩니다. ($reason)",
                timeLabel = now,
                category = AlarmCategory.OperationCancel,
                isUnread = true,
            ),
        )
    }

    private fun fetchStatus(operationId: String): String? {
        val filter = if (uuidRegex.matches(operationId)) {
            "id=eq.${URLEncoder.encode(operationId, "UTF-8")}"
        } else {
            "external_id=eq.${URLEncoder.encode(operationId, "UTF-8")}"
        }
        val result = SupabaseClient.request(
            method = "GET",
            path = "/rest/v1/operations?select=status&$filter&limit=1",
            authed = true,
        )
        if (result.code !in 200..299) {
            Log.w(TAG, "status fetch failed ${result.code}: ${result.body.take(160)}")
            return null
        }
        val arr = JSONArray(result.body)
        if (arr.length() == 0) return null
        return arr.getJSONObject(0).optString("status")
    }
}
