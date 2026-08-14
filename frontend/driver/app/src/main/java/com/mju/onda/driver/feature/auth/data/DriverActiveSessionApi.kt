package com.mju.onda.driver.feature.auth.data

import android.util.Log
import com.mju.onda.driver.core.DeviceSessionId
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 기사 계정당 활성 기기 1대.
 * 테이블이 아직 없으면(팀원 SQL 미실행) 조용히 건너뛴다.
 */
object DriverActiveSessionApi {
    private const val TAG = "DriverSession"
    private const val TABLE = "driver_active_sessions"

    @Volatile
    private var tableMissing: Boolean = false

    suspend fun claimExclusive(): Boolean = withContext(Dispatchers.IO) {
        claimExclusiveBlocking()
    }

    fun claimExclusiveBlocking(): Boolean {
        if (tableMissing) return false
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) return false
        val driverId = SupabaseClient.userUuid?.takeIf { it.isNotBlank() } ?: return false
        val deviceId = DeviceSessionId.get()
        val body = JSONObject()
            .put("driver_id", driverId)
            .put("device_id", deviceId)
            .toString()
        val result = SupabaseClient.request(
            method = "POST",
            path = "/rest/v1/$TABLE?on_conflict=driver_id",
            jsonBody = body,
            authed = true,
            prefer = "resolution=merge-duplicates,return=minimal",
        )
        if (isMissingTable(result.code, result.body)) {
            tableMissing = true
            Log.w(TAG, "table missing — run migrate_driver_active_sessions.sql")
            return false
        }
        if (result.code !in 200..299) {
            Log.w(TAG, "claim HTTP ${result.code}: ${result.body.take(200)}")
            return false
        }
        Log.i(TAG, "claimed session driver=$driverId device=$deviceId")
        return true
    }

    sealed class FetchResult {
        data class Owner(val deviceId: String) : FetchResult()
        data object None : FetchResult()
        data object Unavailable : FetchResult()
    }

    fun fetchOwner(): FetchResult {
        if (tableMissing) return FetchResult.Unavailable
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            return FetchResult.Unavailable
        }
        val driverId = SupabaseClient.userUuid?.takeIf { it.isNotBlank() }
            ?: return FetchResult.Unavailable
        val path = "/rest/v1/$TABLE?driver_id=eq.${URLEncoder.encode(driverId, "UTF-8")}" +
            "&select=device_id&limit=1"
        val result = SupabaseClient.request(method = "GET", path = path, authed = true)
        if (isMissingTable(result.code, result.body)) {
            tableMissing = true
            Log.w(TAG, "table missing — run migrate_driver_active_sessions.sql")
            return FetchResult.Unavailable
        }
        if (result.code !in 200..299) {
            Log.w(TAG, "fetch HTTP ${result.code}: ${result.body.take(160)}")
            return FetchResult.Unavailable
        }
        val arr = runCatching { JSONArray(result.body) }.getOrNull()
            ?: return FetchResult.Unavailable
        if (arr.length() == 0) return FetchResult.None
        val deviceId = arr.getJSONObject(0).optString("device_id")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: return FetchResult.None
        return FetchResult.Owner(deviceId)
    }

    private fun isMissingTable(code: Int, body: String): Boolean {
        if (code == 404 || code == 406) return true
        val lower = body.lowercase()
        return "pgrst205" in lower ||
            "could not find the table" in lower ||
            (code == 400 && "driver_active_sessions" in lower)
    }
}
