package com.mju.onda.driver.core.location

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import com.mju.onda.driver.feature.inoperation.data.StopProgressTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 운행 중 정류장 진행을 Supabase `operation_stop_progress`에 저장/복원.
 */
object OperationStopProgressApi {
    private const val TAG = "ONDA_STOP_PROGRESS"
    private val uuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    data class Snapshot(
        val lastArrivedStopId: String?,
        val lastPassedStopId: String?,
        val lastArrivedIndex: Int,
        val lastPassedIndex: Int,
    ) {
        fun toTracker(): StopProgressTracker =
            StopProgressTracker(
                lastPassedIndex = lastPassedIndex,
                lastArrivedIndex = maxOf(lastArrivedIndex, lastPassedIndex),
            )
    }

    suspend fun fetch(operationId: String): Snapshot? = withContext(Dispatchers.IO) {
        val opUuid = OperationGpsApi.resolveOperationUuid(operationId) ?: return@withContext null
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) return@withContext null
        val path = "/rest/v1/operation_stop_progress?operation_id=eq.${enc(opUuid)}" +
            "&select=last_arrived_stop_id,last_passed_stop_id,last_arrived_index,last_passed_index" +
            "&limit=1"
        val result = SupabaseClient.request(method = "GET", path = path, authed = true)
        if (result.code !in 200..299) {
            Log.w(TAG, "fetch HTTP ${result.code}: ${result.body.take(200)}")
            return@withContext null
        }
        val arr = runCatching { JSONArray(result.body) }.getOrNull() ?: return@withContext null
        if (arr.length() == 0) return@withContext null
        val row = arr.getJSONObject(0)
        Snapshot(
            lastArrivedStopId = uuidOrNull(row.optString("last_arrived_stop_id")),
            lastPassedStopId = uuidOrNull(row.optString("last_passed_stop_id")),
            lastArrivedIndex = row.optInt("last_arrived_index", -1),
            lastPassedIndex = row.optInt("last_passed_index", -1),
        )
    }

    suspend fun upsert(
        operationId: String,
        arrivedStopId: String?,
        passedStopId: String?,
        arrivedIndex: Int,
        passedIndex: Int,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            Log.d(TAG, "skip: supabase not ready")
            return@withContext false
        }
        val opUuid = OperationGpsApi.resolveOperationUuid(operationId) ?: run {
            Log.w(TAG, "skip: operation unresolved ($operationId)")
            return@withContext false
        }
        val body = JSONObject().apply {
            put("operation_id", opUuid)
            put("last_arrived_stop_id", uuidOrJsonNull(arrivedStopId))
            put("last_passed_stop_id", uuidOrJsonNull(passedStopId))
            put("last_arrived_index", arrivedIndex)
            put("last_passed_index", passedIndex)
        }
        val result = SupabaseClient.request(
            method = "POST",
            path = "/rest/v1/operation_stop_progress?on_conflict=operation_id",
            jsonBody = body.toString(),
            authed = true,
            prefer = "resolution=merge-duplicates,return=minimal",
        )
        if (result.code !in 200..299) {
            Log.w(TAG, "upsert HTTP ${result.code}: ${result.body.take(240)}")
            return@withContext false
        }
        Log.d(
            TAG,
            "progress operationId=$opUuid arrived=$arrivedIndex passed=$passedIndex",
        )
        true
    }

    private fun uuidOrNull(raw: String): String? =
        raw.takeIf { uuidRegex.matches(it) }

    private fun uuidOrJsonNull(raw: String?): Any =
        raw?.takeIf { uuidRegex.matches(it) } ?: JSONObject.NULL

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
