package com.mju.onda.driver.feature.settings.data

import android.util.Log
import com.mju.onda.driver.BuildConfig
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 관리자 웹(Vite) `/api/safe-stop` — 안전 정차 요청 전송·조회.
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
        data object Ok : PostResult()
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
        val base = BuildConfig.ADMIN_DEV_BASE_URL.trimEnd('/')
        if (base.isBlank()) return@withContext PostResult.Failed

        var conn: HttpURLConnection? = null
        try {
            val url = URL("$base/api/safe-stop")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 5_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val body = JSONObject().apply {
                put("id", id)
                put("driverId", driverId)
                put("driverName", driverName)
                put("vehicleName", vehicleName)
                put("routeName", routeName)
                put("operationId", operationId)
                put("reason", reason)
                put("detailReason", detailReason)
                put("requestedAt", requestedAt)
                put("date", date)
            }.toString()
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.d(TAG, "POST failed: HTTP $code")
                return@withContext PostResult.Failed
            }
            PostResult.Ok
        } catch (e: Exception) {
            Log.d(TAG, "POST skipped: ${e.message}")
            PostResult.Failed
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun cancelRequest(id: String): PostResult = withContext(Dispatchers.IO) {
        val base = BuildConfig.ADMIN_DEV_BASE_URL.trimEnd('/')
        if (base.isBlank() || id.isBlank()) return@withContext PostResult.Failed

        var conn: HttpURLConnection? = null
        try {
            val url = URL("$base/api/safe-stop/${java.net.URLEncoder.encode(id, "UTF-8")}")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 5_000
                readTimeout = 5_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                it.write("""{"decision":"cancelled"}""")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.d(TAG, "cancel PATCH failed: HTTP $code")
                return@withContext PostResult.Failed
            }
            PostResult.Ok
        } catch (e: Exception) {
            Log.d(TAG, "cancel PATCH skipped: ${e.message}")
            PostResult.Failed
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun fetchForDriver(driverId: String): FetchResult = withContext(Dispatchers.IO) {
        val base = BuildConfig.ADMIN_DEV_BASE_URL.trimEnd('/')
        if (base.isBlank() || driverId.isBlank()) return@withContext FetchResult.Failed

        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "$base/api/safe-stop?driverId=${java.net.URLEncoder.encode(driverId, "UTF-8")}",
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.d(TAG, "GET failed: HTTP $code")
                return@withContext FetchResult.Failed
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            FetchResult.Ok(parse(text))
        } catch (e: Exception) {
            Log.d(TAG, "GET skipped: ${e.message}")
            FetchResult.Failed
        } finally {
            conn?.disconnect()
        }
    }

    private fun parse(json: String): List<RemoteRequest> {
        val arr = JSONArray(json)
        val out = ArrayList<RemoteRequest>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                RemoteRequest(
                    id = o.getString("id"),
                    decision = o.optString("decision", "pending"),
                    reason = o.optString("reason", ""),
                    requestedAt = o.optString("requestedAt", ""),
                    routeName = o.optString("routeName", ""),
                    vehicleName = o.optString("vehicleName", ""),
                    date = o.optString("date", ""),
                ),
            )
        }
        return out
    }
}
