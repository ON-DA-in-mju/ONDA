package com.mju.onda.driver.feature.home.data

import android.util.Log
import com.mju.onda.driver.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * 관리자 웹(Vite) `/api/assignments`에서 오늘 배정을 조회한다.
 * 서버가 없거나 실패하면 null (호출측에서 mock fallback).
 */
object TodayAssignmentsApi {
    private const val TAG = "TodayAssignmentsApi"
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    sealed class Result {
        data class Ok(val items: List<AssignedOperation>) : Result()
        data object Failed : Result()
    }

    suspend fun fetchForDriver(driverId: String): Result = withContext(Dispatchers.IO) {
        val base = BuildConfig.ADMIN_DEV_BASE_URL.trimEnd('/')
        if (base.isBlank() || driverId.isBlank()) return@withContext Result.Failed

        var conn: HttpURLConnection? = null
        try {
            val date = LocalDate.now().format(dateFmt)
            val url = URL(
                "$base/api/assignments?date=$date&driverId=${java.net.URLEncoder.encode(driverId, "UTF-8")}",
            )
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_000
                readTimeout = 3_000
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.d(TAG, "GET failed: HTTP $code")
                return@withContext Result.Failed
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            Result.Ok(parseAssignments(text))
        } catch (e: Exception) {
            Log.d(TAG, "GET skipped: ${e.message}")
            Result.Failed
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseAssignments(json: String): List<AssignedOperation> {
        val arr = JSONArray(json)
        val out = ArrayList<AssignedOperation>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                AssignedOperation(
                    id = o.getString("id"),
                    routeName = o.optString("routeName", ""),
                    vehicleName = o.optString("vehicleName", ""),
                    departTime = o.optString("departTime", ""),
                    origin = o.optString("origin", ""),
                    destination = o.optString("destination", ""),
                    round = o.optInt("round", 1),
                    expectedEndTime = o.optString("expectedEndTime", ""),
                    status = mapStatus(o.optString("status", "scheduled")),
                ),
            )
        }
        return out.sortedBy { it.departTime }
    }

    private fun mapStatus(raw: String): OperationStatus = when (raw) {
        "waiting" -> OperationStatus.Waiting
        "departing_soon" -> OperationStatus.DepartingSoon
        "in_progress" -> OperationStatus.InProgress
        "ended" -> OperationStatus.Ended
        else -> OperationStatus.Scheduled
    }
}
