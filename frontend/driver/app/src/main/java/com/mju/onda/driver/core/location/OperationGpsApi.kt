package com.mju.onda.driver.core.location

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.net.URLEncoder
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 운행 중 GPS를 Supabase `vehicle_locations` + `operation_logs`(LOCATION_UPDATED)에 기록.
 * `operation_logs`는 최대 [MAX_LOGS]건만 유지(오래된 것부터 삭제).
 */
object OperationGpsApi {
    private const val TAG = "OperationGpsApi"
    private const val TIME_TAG = "ONDA_LOCATION_TIME"
    const val MAX_LOGS = 100

    @Volatile
    private var lastSentLat: Double? = null
    @Volatile
    private var lastSentLng: Double? = null

    suspend fun report(
        operationId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float?,
        vehicleName: String? = null,
        driverName: String? = null,
        recordedAtMillis: Long? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            Log.d(TAG, "skip: supabase not ready")
            return@withContext false
        }
        val opUuid = resolveOperationUuid(operationId) ?: run {
            Log.w(TAG, "skip: operation unresolved ($operationId)")
            return@withContext false
        }

        val nowMs = System.currentTimeMillis()
        val nowIso = Instant.ofEpochMilli(nowMs).toString()
        val measuredMs = recordedAtMillis?.takeIf { it > 0L } ?: nowMs
        val measuredAgeMs = (nowMs - measuredMs).coerceAtLeast(0L)
        val ageSec = measuredAgeMs / 1000.0
        // 학생 앱 「마지막 갱신 N초 전」= now - recorded_at.
        // 에뮬/기기 Location.time·시계 오차로 GPS time 이 수십 초 과거일 수 있어
        // vehicle_locations.recorded_at 은 항상 업로드 시각(now)을 쓴다.
        val recordedAt = nowIso
        val sameAsPrevious = lastSentLat == latitude && lastSentLng == longitude
        lastSentLat = latitude
        lastSentLng = longitude
        Log.d(
            TIME_TAG,
            "vehicle_locations insert: recordedAt=$recordedAt now=$nowIso " +
                "gpsTimeAgeSec=$ageSec source=upload_now " +
                "lat=$latitude lng=$longitude sameAsPrevious=$sameAsPrevious",
        )
        val locOk = insertVehicleLocation(opUuid, latitude, longitude, recordedAt)
        val logOk = insertOperationLog(
            opUuid = opUuid,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            vehicleName = vehicleName,
            driverName = driverName,
            recordedAt = nowIso,
        )
        if (logOk) trimOperationLogs()
        locOk || logOk
    }

    private fun insertVehicleLocation(
        opUuid: String,
        latitude: Double,
        longitude: Double,
        recordedAt: String,
    ): Boolean {
        val body = JSONObject().apply {
            put("operation_id", opUuid)
            put("latitude", latitude)
            put("longitude", longitude)
            put("recorded_at", recordedAt)
        }
        val result = SupabaseClient.request(
            method = "POST",
            path = "/rest/v1/vehicle_locations",
            jsonBody = body.toString(),
            authed = true,
        )
        if (result.code !in 200..299) {
            Log.w(TAG, "vehicle_locations HTTP ${result.code}: ${result.body.take(200)}")
            return false
        }
        return true
    }

    private fun insertOperationLog(
        opUuid: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float?,
        vehicleName: String?,
        driverName: String?,
        recordedAt: String,
    ): Boolean {
        val accPart = accuracy?.let { String.format("%.1fm", it) } ?: "-"
        val msg = buildString {
            append("GPS 수신 lat=").append(String.format("%.6f", latitude))
            append(" lng=").append(String.format("%.6f", longitude))
            append(" accuracy=").append(accPart)
            if (!vehicleName.isNullOrBlank()) append(" vehicle=").append(vehicleName)
            if (!driverName.isNullOrBlank()) append(" driver=").append(driverName)
            append(" at=").append(recordedAt)
        }
        val body = JSONObject().apply {
            put("operation_id", opUuid)
            put("event_type", "LOCATION_UPDATED")
            put("log_message", msg)
            put("created_at", recordedAt)
        }
        val result = SupabaseClient.request(
            method = "POST",
            path = "/rest/v1/operation_logs",
            jsonBody = body.toString(),
            authed = true,
        )
        if (result.code !in 200..299) {
            Log.w(TAG, "operation_logs HTTP ${result.code}: ${result.body.take(200)}")
            return false
        }
        return true
    }

    /** 최신 [MAX_LOGS]건만 남기고 나머지 삭제 */
    private fun trimOperationLogs() {
        val list = SupabaseClient.request(
            method = "GET",
            path = "/rest/v1/operation_logs?select=id&order=created_at.desc.nullslast,id.desc&offset=$MAX_LOGS&limit=500",
            authed = true,
        )
        if (list.code !in 200..299) {
            Log.d(TAG, "trim list HTTP ${list.code}")
            return
        }
        val arr = runCatching { JSONArray(list.body) }.getOrNull() ?: return
        if (arr.length() == 0) return
        val ids = buildList {
            for (i in 0 until arr.length()) {
                val id = arr.getJSONObject(i).optString("id")
                if (id.isNotBlank()) add(id)
            }
        }
        if (ids.isEmpty()) return
        val inList = ids.joinToString(",") { enc(it) }
        val del = SupabaseClient.request(
            method = "DELETE",
            path = "/rest/v1/operation_logs?id=in.($inList)",
            authed = true,
        )
        if (del.code !in 200..299) {
            Log.w(TAG, "trim delete HTTP ${del.code}: ${del.body.take(200)}")
        } else {
            Log.d(TAG, "trimmed ${ids.size} old operation_logs")
        }
    }

    fun resolveOperationUuid(operationId: String): String? {
        if (operationId.isBlank()) return null
        val uuidRegex = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        )
        if (uuidRegex.matches(operationId)) return operationId

        val byExt = SupabaseClient.request(
            method = "GET",
            path = "/rest/v1/operations?select=id&external_id=eq.${enc(operationId)}&limit=1",
            authed = true,
        )
        if (byExt.code in 200..299) {
            val arr = JSONArray(byExt.body)
            if (arr.length() > 0) return arr.getJSONObject(0).optString("id").ifBlank { null }
        }
        val byId = SupabaseClient.request(
            method = "GET",
            path = "/rest/v1/operations?select=id&id=eq.${enc(operationId)}&limit=1",
            authed = true,
        )
        if (byId.code in 200..299) {
            val arr = JSONArray(byId.body)
            if (arr.length() > 0) return arr.getJSONObject(0).optString("id").ifBlank { null }
        }
        return null
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
