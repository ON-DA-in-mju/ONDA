package com.mju.onda.driver.core.location

import android.content.Context
import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 운행 중 Supabase `operation_device_status` heartbeat + (fix 있을 때) `vehicle_locations` INSERT.
 * Vite LiveHeartbeatReporter 와 병행. 네트워크 실패 시 서버 행은 자연스럽게 stale.
 */
object DeviceStatusReporter {
    private const val TAG = "DeviceStatusReporter"
    private const val INTERVAL_MS = 10_000L
    private const val GPS_OK_WINDOW_MS = 15_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    @Volatile
    private var appContext: Context? = null

    /** external_id 또는 uuid → operations.id (uuid) */
    private val uuidCache = ConcurrentHashMap<String, String>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun start(operationId: String) {
        loopJob?.cancel()
        loopJob = scope.launch {
            upsertOnce(operationId)
            while (isActive) {
                delay(INTERVAL_MS)
                if (!OperationLocationTracker.isTracking) break
                val id = OperationLocationTracker.activeOperationId ?: operationId
                upsertOnce(id)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    private suspend fun upsertOnce(operationKey: String) = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            return@withContext
        }
        val ctx = appContext
        val uuid = resolveOperationUuid(operationKey) ?: run {
            Log.d(TAG, "skip: cannot resolve uuid for $operationKey")
            return@withContext
        }

        val gpsEnabled = ctx != null && OperationDeviceStatus.isGpsEnabled(ctx)
        val fix = LatestLocationHolder.latest?.takeIf { it.operationId == operationKey || it.operationId == uuid }
        val recentFix = fix != null &&
            System.currentTimeMillis() - fix.recordedAtMillis < GPS_OK_WINDOW_MS
        val gpsOk = gpsEnabled && recentFix

        val body = JSONObject()
            .put("operation_id", uuid)
            .put("gps_ok", gpsOk)
            .put("gps_enabled", gpsEnabled)
        if (fix != null) {
            body.put("last_location_at", Instant.ofEpochMilli(fix.recordedAtMillis).toString())
            body.put("last_accuracy", fix.accuracy.toDouble())
        } else {
            body.put("last_location_at", JSONObject.NULL)
            body.put("last_accuracy", JSONObject.NULL)
        }

        try {
            val statusResult = SupabaseClient.request(
                method = "POST",
                path = "/rest/v1/operation_device_status",
                jsonBody = body.toString(),
                authed = true,
                prefer = "resolution=merge-duplicates,return=minimal",
            )
            if (statusResult.code !in 200..299) {
                Log.d(TAG, "status upsert HTTP ${statusResult.code}: ${statusResult.body.take(160)}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "status upsert skipped: ${e.message}")
        }

        // GPS fix 가 있으면 좌표도 함께 INSERT (학생 Realtime용)
        if (fix != null && recentFix) {
            try {
                val loc = JSONObject()
                    .put("operation_id", uuid)
                    .put("latitude", fix.latitude)
                    .put("longitude", fix.longitude)
                    .put("recorded_at", Instant.ofEpochMilli(fix.recordedAtMillis).toString())
                val locResult = SupabaseClient.request(
                    method = "POST",
                    path = "/rest/v1/vehicle_locations",
                    jsonBody = loc.toString(),
                    authed = true,
                    prefer = "return=minimal",
                )
                if (locResult.code !in 200..299) {
                    Log.d(TAG, "location insert HTTP ${locResult.code}: ${locResult.body.take(160)}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "location insert skipped: ${e.message}")
            }
        }
    }

    private fun resolveOperationUuid(operationKey: String): String? {
        uuidCache[operationKey]?.let { return it }
        if (looksLikeUuid(operationKey)) {
            uuidCache[operationKey] = operationKey
            return operationKey
        }
        return try {
            val encoded = java.net.URLEncoder.encode(operationKey, "UTF-8")
            val result = SupabaseClient.request(
                method = "GET",
                path = "/rest/v1/operations?select=id&external_id=eq.$encoded&limit=1",
                authed = true,
            )
            if (result.code !in 200..299) {
                Log.d(TAG, "resolve uuid HTTP ${result.code}")
                return null
            }
            val arr = JSONArray(result.body)
            if (arr.length() == 0) return null
            val id = arr.getJSONObject(0).optString("id")
            if (id.isBlank()) null else {
                uuidCache[operationKey] = id
                id
            }
        } catch (e: Exception) {
            Log.d(TAG, "resolve uuid failed: ${e.message}")
            null
        }
    }

    private fun looksLikeUuid(value: String): Boolean =
        try {
            UUID.fromString(value)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
}
