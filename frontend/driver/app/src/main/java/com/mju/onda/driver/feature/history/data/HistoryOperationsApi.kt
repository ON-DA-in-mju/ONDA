package com.mju.onda.driver.feature.history.data

import android.util.Log
import com.mju.onda.driver.core.OndaDates
import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.net.URLEncoder
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * DB `operations` 기반 완료 이력 조회용 API.
 *
 * - 목록: COMPLETED / CANCELLED + started_at/ended_at 존재
 * - 상세: id 단건 조회
 *
 * driver_id 직접 필터 대신 Supabase RLS로 본인 driver 행만 내려오도록 기대한다.
 */
object HistoryOperationsApi {
    private const val TAG = "HistoryOpsApi"

    private fun isConfigured(): Boolean = SupabaseClient.isConfigured && !SupabaseClient.accessToken.isNullOrBlank()

    suspend fun fetchCompletedHistoryForRange(start: LocalDate, end: LocalDate): List<HistoryRecord> =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) {
                Log.w(TAG, "skip fetch: supabase not ready")
                return@withContext emptyList()
            }

            val select = listOf(
                "id",
                "operation_date",
                "status",
                "started_at",
                "ended_at",
                "origin_stop:origin_stop_id(stop_name)",
                "destination_stop:destination_stop_id(stop_name)",
                "buses:bus_id(bus_name,vehicle_number)",
                "schedules:schedule_id(departure_time,routes:route_id(route_name))",
            ).joinToString(",")

            // nullslast 는 소문자여야 함 (nullsLast → PGRST100으로 목록이 항상 비게 됨)
            val path =
                "/rest/v1/operations?select=${URLEncoder.encode(select, "UTF-8")}" +
                    "&operation_date=gte.$start" +
                    "&operation_date=lte.$end" +
                    "&status=in.(COMPLETED,CANCELLED)" +
                    "&started_at=not.is.null" +
                    "&ended_at=not.is.null" +
                    "&order=ended_at.desc.nullslast"

            val result = SupabaseClient.request(method = "GET", path = path, authed = true)
            if (result.code !in 200..299) {
                Log.w(TAG, "list failed HTTP ${result.code}: ${result.body.take(240)}")
                return@withContext emptyList()
            }

            parseHistoryList(result.body).also {
                Log.d(TAG, "loaded ${it.size} history rows ($start..$end)")
            }
        }

    suspend fun fetchHistoryDetailById(operationId: String): HistoryRecord? =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) return@withContext null
            if (operationId.isBlank() || operationId.startsWith("runtime-")) return@withContext null

            val select = listOf(
                "id",
                "operation_date",
                "status",
                "started_at",
                "ended_at",
                "origin_stop:origin_stop_id(stop_name)",
                "destination_stop:destination_stop_id(stop_name)",
                "buses:bus_id(bus_name,vehicle_number)",
                "schedules:schedule_id(departure_time,routes:route_id(route_name))",
            ).joinToString(",")

            val path =
                "/rest/v1/operations?select=${URLEncoder.encode(select, "UTF-8")}" +
                    "&id=eq.${URLEncoder.encode(operationId, "UTF-8")}" +
                    "&limit=1"

            val result = SupabaseClient.request(method = "GET", path = path, authed = true)
            if (result.code !in 200..299) {
                Log.w(TAG, "detail failed HTTP ${result.code}: ${result.body.take(240)}")
                return@withContext null
            }

            val arr = JSONArray(result.body)
            if (arr.length() == 0) return@withContext null
            return@withContext mapRow(arr.getJSONObject(0))
        }

    private fun parseHistoryList(body: String): List<HistoryRecord> {
        val arr = JSONArray(body)
        val out = ArrayList<HistoryRecord>(arr.length())
        for (i in 0 until arr.length()) {
            val row = arr.getJSONObject(i)
            mapRow(row)?.let { out.add(it) }
        }
        return out
    }

    private fun mapRow(row: JSONObject): HistoryRecord? {
        val id = row.optString("id")
        val operationDateRaw = row.optString("operation_date")
        val statusRaw = row.optString("status")
        val startedAtRaw = row.optString("started_at")
        val endedAtRaw = row.optString("ended_at")

        if (id.isBlank()) return null
        if (operationDateRaw.isBlank()) return null
        if (startedAtRaw.isBlank() || endedAtRaw.isBlank()) return null

        val date = runCatching { LocalDate.parse(operationDateRaw) }.getOrNull() ?: return null
        val startedAtMillis = OperationTripClock.parseInstantMillis(startedAtRaw) ?: return null
        val endedAtMillis = OperationTripClock.parseInstantMillis(endedAtRaw) ?: return null
        if (endedAtMillis < startedAtMillis) return null

        val status = when (statusRaw) {
            "COMPLETED" -> HistoryResultStatus.Completed
            "CANCELLED" -> HistoryResultStatus.AdminEnded
            else -> return null
        }

        val buses = row.optJSONObject("buses")
        val vehicleName = buses?.optString("bus_name").orEmpty()
        val plateNumber = buses?.optString("vehicle_number").orEmpty()

        val schedules = row.optJSONObject("schedules")
        val routes = schedules?.optJSONObject("routes")
        val routeName = routes?.optString("route_name").orEmpty()
        val scheduledDepart = hhmm(schedules?.optString("departure_time"))
        val origin = row.optJSONObject("origin_stop")?.optString("stop_name").orEmpty()
        val destination = row.optJSONObject("destination_stop")?.optString("stop_name").orEmpty()

        val actualDepart = OperationTripClock.formatHm(startedAtMillis)
        val durationLabel = OperationTripClock.formatElapsedMinutes(startedAtMillis, endedAtMillis)
        val timeRange = OperationTripClock.formatTimeRange(startedAtMillis, endedAtMillis)

        return HistoryRecord(
            id = id,
            date = date,
            dateLabel = OndaDates.historyListDateLabel(date),
            routeName = routeName.ifBlank { "-" },
            vehicleName = vehicleName.ifBlank { "-" },
            plateNumber = plateNumber.ifBlank { "-" },
            actualDepart = actualDepart,
            durationLabel = durationLabel,
            timeRange = timeRange,
            status = status,
            origin = origin.ifBlank { "-" },
            destination = destination.ifBlank { "-" },
            scheduledDepart = scheduledDepart.ifBlank { "-" },
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
        )
    }

    private fun hhmm(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return ""
        return raw.take(5)
    }
}
