package com.mju.onda.driver.feature.home.data

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Supabase `operations` 에서 오늘 배정을 조회한다.
 * 실패 시 mock으로 대체하지 않는다.
 */
object TodayAssignmentsApi {
    private const val TAG = "TodayAssignmentsApi"
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    sealed class Result {
        data class Ok(val items: List<AssignedOperation>) : Result()
        data object Failed : Result()
    }

    suspend fun fetchForDriver(driverLoginId: String): Result = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            Log.w(TAG, "Supabase not configured")
            return@withContext Result.Failed
        }
        if (SupabaseClient.accessToken.isNullOrBlank()) {
            Log.w(TAG, "No access token")
            return@withContext Result.Failed
        }

        try {
            val date = LocalDate.now().format(dateFmt)
            val select = listOf(
                "id",
                "external_id",
                "operation_date",
                "status",
                "round",
                "origin",
                "destination",
                "expected_end_time",
                "buses:bus_id(bus_name)",
                "schedules:schedule_id(departure_time,routes:route_id(route_name))",
            ).joinToString(",")

            // RLS: 본인 driver_id 행만 반환. loginId는 로깅용.
            val path =
                "/rest/v1/operations?select=${java.net.URLEncoder.encode(select, "UTF-8")}" +
                    "&operation_date=eq.$date" +
                    "&order=expected_end_time.asc"

            val result = SupabaseClient.request(method = "GET", path = path, authed = true)
            if (result.code !in 200..299) {
                Log.w(TAG, "GET failed HTTP ${result.code}: ${result.body.take(200)}")
                return@withContext Result.Failed
            }
            val items = parseAssignments(result.body)
            Log.d(TAG, "loaded ${items.size} ops for $driverLoginId")
            Result.Ok(items)
        } catch (e: Exception) {
            Log.w(TAG, "GET error: ${e.message}")
            Result.Failed
        }
    }

    private fun parseAssignments(json: String): List<AssignedOperation> {
        val arr = JSONArray(json)
        val out = ArrayList<AssignedOperation>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val schedules = o.optJSONObject("schedules")
            val routes = schedules?.optJSONObject("routes")
            val buses = o.optJSONObject("buses")
            val externalId = o.optString("external_id")
            val uuid = o.optString("id")
            out.add(
                AssignedOperation(
                    id = if (externalId.isNotBlank()) externalId else uuid,
                    routeName = routes?.optString("route_name").orEmpty(),
                    vehicleName = buses?.optString("bus_name").orEmpty(),
                    departTime = hhmm(schedules?.optString("departure_time")),
                    origin = o.optString("origin"),
                    destination = o.optString("destination"),
                    round = o.optInt("round", 1),
                    expectedEndTime = hhmm(o.optString("expected_end_time")),
                    status = mapStatus(o.optString("status", "SCHEDULED")),
                ),
            )
        }
        return out.sortedBy { it.departTime }
    }

    private fun hhmm(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.take(5)
    }

    private fun mapStatus(raw: String): OperationStatus = when (raw.uppercase()) {
        "IN_PROGRESS" -> OperationStatus.InProgress
        "COMPLETED", "CANCELLED" -> OperationStatus.Ended
        else -> OperationStatus.Scheduled
    }

    /** 운행 시작/종료 시 DB status 반영 (external_id 또는 uuid) */
    suspend fun updateStatus(operationId: String, status: OperationStatus): Boolean =
        withContext(Dispatchers.IO) {
            if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) return@withContext false
            val dbStatus = when (status) {
                OperationStatus.InProgress -> "IN_PROGRESS"
                OperationStatus.Ended -> "COMPLETED"
                else -> "SCHEDULED"
            }
            val patch = JSONObject().put("status", dbStatus)
            if (status == OperationStatus.InProgress) {
                patch.put("started_at", java.time.Instant.now().toString())
            }
            if (status == OperationStatus.Ended) {
                patch.put("ended_at", java.time.Instant.now().toString())
            }

            val filter = if (operationId.contains('-') && operationId.length > 20) {
                "id=eq.${java.net.URLEncoder.encode(operationId, "UTF-8")}"
            } else {
                "external_id=eq.${java.net.URLEncoder.encode(operationId, "UTF-8")}"
            }
            val result = SupabaseClient.request(
                method = "PATCH",
                path = "/rest/v1/operations?$filter",
                jsonBody = patch.toString(),
                authed = true,
            ).also {
                // Prefer return=minimal
            }
            // PostgREST needs Prefer header for some patches — retry via query if needed
            if (result.code in 200..299) return@withContext true
            Log.w(TAG, "PATCH status failed ${result.code}: ${result.body.take(200)}")
            false
        }
}
