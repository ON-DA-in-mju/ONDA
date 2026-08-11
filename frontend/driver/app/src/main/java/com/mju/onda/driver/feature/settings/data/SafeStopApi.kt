package com.mju.onda.driver.feature.settings.data

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.net.URLEncoder
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Supabase `safe_stop_requests` — 안전 정차 요청 전송·조회·취소.
 * (이전 Vite `/api/safe-stop` 의존 제거)
 */
object SafeStopApi {
    private const val TAG = "SafeStopApi"

    data class RemoteRequest(
        val id: String,
        val decision: String,
        val reason: String,
        val requestedAt: String,
        val routeName: String,
        val vehicleName: String,
        val date: String,
    )

    sealed class PostResult {
        data class Ok(val id: String) : PostResult()
        data object Failed : PostResult()
    }

    sealed class FetchResult {
        data class Ok(val items: List<RemoteRequest>) : FetchResult()
        data object Failed : FetchResult()
    }

    suspend fun postRequest(
        id: String,
        driverId: String,
        driverName: String,
        vehicleName: String,
        routeName: String,
        operationId: String,
        reason: String,
        detailReason: String,
        requestedAt: String,
        date: String,
    ): PostResult = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            Log.w(TAG, "Supabase not ready")
            return@withContext PostResult.Failed
        }
        val driverUuid = SupabaseClient.userUuid
        if (driverUuid.isNullOrBlank()) {
            Log.w(TAG, "No user uuid")
            return@withContext PostResult.Failed
        }

        try {
            val opUuid = resolveOperationUuid(operationId)
            val body = JSONObject().apply {
                put("driver_id", driverUuid)
                put("reason", reason)
                put("detail_reason", detailReason)
                put("decision", "pending")
                put("requested_at", Instant.now().toString())
                if (!opUuid.isNullOrBlank()) put("operation_id", opUuid)
            }

            val result = SupabaseClient.request(
                method = "POST",
                path = "/rest/v1/safe_stop_requests",
                jsonBody = body.toString(),
                authed = true,
            )
            if (result.code !in 200..299) {
                Log.w(TAG, "POST failed HTTP ${result.code}: ${result.body.take(240)}")
                return@withContext PostResult.Failed
            }
            val serverId = parseInsertedId(result.body) ?: id
            Log.d(TAG, "posted safe_stop id=$serverId op=$opUuid")
            // keep unused params referenced for call-site compatibility
            Log.d(TAG, "meta driver=$driverId/$driverName $vehicleName $routeName $requestedAt $date")
            PostResult.Ok(serverId)
        } catch (e: Exception) {
            Log.w(TAG, "POST error: ${e.message}")
            PostResult.Failed
        }
    }

    suspend fun cancelRequest(id: String): PostResult = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank() || id.isBlank()) {
            return@withContext PostResult.Failed
        }
        try {
            val patch = JSONObject()
                .put("decision", "cancelled")
                .put("decided_at", Instant.now().toString())
            val result = SupabaseClient.request(
                method = "PATCH",
                path = "/rest/v1/safe_stop_requests?id=eq.${enc(id)}",
                jsonBody = patch.toString(),
                authed = true,
            )
            if (result.code !in 200..299) {
                Log.w(TAG, "cancel failed HTTP ${result.code}: ${result.body.take(200)}")
                return@withContext PostResult.Failed
            }
            PostResult.Ok(id)
        } catch (e: Exception) {
            Log.w(TAG, "cancel error: ${e.message}")
            PostResult.Failed
        }
    }

    /**
     * @param driverId login_id 또는 uuid — 실제 조회는 세션 uuid 우선
     */
    suspend fun fetchForDriver(driverId: String): FetchResult = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            return@withContext FetchResult.Failed
        }
        val uuid = SupabaseClient.userUuid
            ?: driverId.takeIf { it.contains('-') && it.length > 20 }
            ?: return@withContext FetchResult.Failed

        try {
            val select = listOf(
                "id",
                "decision",
                "reason",
                "requested_at",
                "created_at",
                "operations:operation_id(operation_date,buses:bus_id(bus_name),schedules:schedule_id(routes:route_id(route_name)))",
            ).joinToString(",")
            val path =
                "/rest/v1/safe_stop_requests?select=${enc(select)}" +
                    "&driver_id=eq.${enc(uuid)}" +
                    "&order=created_at.desc"

            val result = SupabaseClient.request(method = "GET", path = path, authed = true)
            if (result.code !in 200..299) {
                Log.w(TAG, "GET failed HTTP ${result.code}: ${result.body.take(200)}")
                return@withContext FetchResult.Failed
            }
            FetchResult.Ok(parse(result.body))
        } catch (e: Exception) {
            Log.w(TAG, "GET error: ${e.message}")
            FetchResult.Failed
        }
    }

    private fun resolveOperationUuid(operationId: String): String? {
        if (operationId.isBlank() || operationId == "unknown") return null
        if (operationId.contains('-') && operationId.length >= 32 &&
            !operationId.startsWith("week-") && !operationId.startsWith("op-") &&
            !operationId.startsWith("d02-") && !operationId.startsWith("stop-")
        ) {
            // likely uuid
            return operationId
        }
        // try as uuid anyway if matches uuid pattern
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
        Log.w(TAG, "operation not resolved: $operationId")
        return null
    }

    private fun parseInsertedId(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            if (arr.length() == 0) return null
            return arr.getJSONObject(0).optString("id").ifBlank { null }
        }
        if (trimmed.startsWith("{")) {
            return JSONObject(trimmed).optString("id").ifBlank { null }
        }
        return null
    }

    private fun parse(json: String): List<RemoteRequest> {
        val arr = JSONArray(json)
        val out = ArrayList<RemoteRequest>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ops = o.optJSONObject("operations")
            val buses = ops?.optJSONObject("buses")
            val routes = ops?.optJSONObject("schedules")?.optJSONObject("routes")
            val requestedAt = o.optString("requested_at")
            out.add(
                RemoteRequest(
                    id = o.getString("id"),
                    decision = o.optString("decision", "pending"),
                    reason = o.optString("reason", ""),
                    requestedAt = toHhmm(requestedAt),
                    routeName = routes?.optString("route_name").orEmpty(),
                    vehicleName = buses?.optString("bus_name").orEmpty(),
                    date = ops?.optString("operation_date").orEmpty(),
                ),
            )
        }
        return out
    }

    private fun toHhmm(iso: String): String {
        if (iso.length >= 16 && iso[10] == 'T') {
            // 2026-08-11T07:06:17... → use local-ish HH:mm from string (UTC) ok for display
            return iso.substring(11, 16)
        }
        return iso.take(5)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
