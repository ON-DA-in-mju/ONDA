package com.mju.onda.driver.feature.home.data

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.time.LocalDate
import java.time.ZoneId
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
    private val koreaZone = ZoneId.of("Asia/Seoul")

    private val uuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    sealed class Result {
        data class Ok(val items: List<AssignedOperation>) : Result()
        data class Failed(val reason: String) : Result()
    }

    /** 관리자 웹과 동일하게 KST 기준 오늘 날짜 */
    fun todayDateKey(): String = LocalDate.now(koreaZone).format(dateFmt)

    suspend fun fetchForDriver(driverLoginId: String): Result = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            Log.w(TAG, "Supabase not configured")
            return@withContext Result.Failed("Supabase 미설정")
        }
        if (SupabaseClient.accessToken.isNullOrBlank()) {
            Log.w(TAG, "No access token")
            return@withContext Result.Failed("로그인 세션 없음 · 다시 로그인해 주세요")
        }

        try {
            val date = todayDateKey()
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

            // RLS: 본인 driver_id 행만 반환. loginId·uuid는 로깅용.
            val path =
                "/rest/v1/operations?select=${java.net.URLEncoder.encode(select, "UTF-8")}" +
                    "&operation_date=eq.$date" +
                    "&order=expected_end_time.asc.nullslast"

            val result = SupabaseClient.request(method = "GET", path = path, authed = true)
            if (result.code !in 200..299) {
                Log.w(TAG, "GET failed HTTP ${result.code}: ${result.body.take(200)}")
                return@withContext Result.Failed("배차 조회 실패 (${result.code})")
            }
            val items = parseAssignments(result.body)
            Log.d(
                TAG,
                "loaded ${items.size} ops for $driverLoginId date=$date uuid=${SupabaseClient.userUuid}",
            )
            Result.Ok(items)
        } catch (e: Exception) {
            Log.w(TAG, "GET error: ${e.message}")
            Result.Failed(e.message ?: "배차 조회 오류")
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
        "COMPLETED" -> OperationStatus.Ended
        "CANCELLED" -> OperationStatus.Unavailable
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

            val uuid = resolveOperationUuid(operationId)
            val filters = buildList {
                if (!uuid.isNullOrBlank()) {
                    add("id=eq.${java.net.URLEncoder.encode(uuid, "UTF-8")}")
                }
                // uuid 해석 실패·구 external_id 대비
                if (!uuidRegex.matches(operationId)) {
                    add("external_id=eq.${java.net.URLEncoder.encode(operationId, "UTF-8")}")
                } else if (uuid.isNullOrBlank()) {
                    add("id=eq.${java.net.URLEncoder.encode(operationId, "UTF-8")}")
                }
            }.distinct()

            if (filters.isEmpty()) {
                Log.w(TAG, "PATCH status skipped: empty filter for operationId=$operationId")
                return@withContext false
            }

            for (filter in filters) {
                val result = SupabaseClient.request(
                    method = "PATCH",
                    path = "/rest/v1/operations?$filter",
                    jsonBody = patch.toString(),
                    authed = true,
                    // 0건 갱신을 204로 성공 처리하지 않도록 representation 확인
                    prefer = "return=representation",
                )
                if (result.code !in 200..299) {
                    Log.w(
                        TAG,
                        "PATCH status HTTP ${result.code} op=$operationId filter=$filter: ${result.body.take(240)}",
                    )
                    continue
                }
                val updated = runCatching { JSONArray(result.body.ifBlank { "[]" }) }
                    .getOrNull()
                    ?.length()
                    ?: 0
                if (updated > 0) {
                    Log.i(TAG, "PATCH status ok op=$operationId → $dbStatus rows=$updated filter=$filter")
                    return@withContext true
                }
                Log.w(TAG, "PATCH status matched 0 rows op=$operationId filter=$filter")
            }
            false
        }

    /** app 배정 id(external_id) → operations.id(uuid) */
    private fun resolveOperationUuid(operationId: String): String? {
        if (operationId.isBlank()) return null
        if (uuidRegex.matches(operationId)) return operationId
        val enc = java.net.URLEncoder.encode(operationId, "UTF-8")
        val byExt = SupabaseClient.request(
            method = "GET",
            path = "/rest/v1/operations?select=id&external_id=eq.$enc&limit=1",
            authed = true,
        )
        if (byExt.code !in 200..299) {
            Log.w(TAG, "resolve uuid HTTP ${byExt.code}: ${byExt.body.take(160)}")
            return null
        }
        val arr = runCatching { JSONArray(byExt.body) }.getOrNull() ?: return null
        if (arr.length() == 0) return null
        return arr.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
    }
}
