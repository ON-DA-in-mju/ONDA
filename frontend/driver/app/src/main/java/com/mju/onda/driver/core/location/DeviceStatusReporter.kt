package com.mju.onda.driver.core.location

import android.content.Context
import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 운행 중 기기/위치 heartbeat를 Supabase `operation_device_status`에 upsert.
 *
 * - gps_enabled: 시스템 위치 서비스 ON/OFF (권한과 무관, 매 heartbeat마다 재계산)
 * - gps_ok: 위치 서비스 ON + 권한 + 최근 정상 fix
 * - updated_at: 네트워크 생존 신호 (fix 없어도 갱신)
 * - last_location_at / last_accuracy: 마지막 정상 위치만 유지 (OFF 시 null로 덮지 않음)
 * - vehicle_locations 적재는 [LiveHeartbeatReporter] / [OperationGpsApi] 담당 (여기서 중복 INSERT 하지 않음)
 *
 * [init]/[start]/[stop] 은 팀원 연동용 API.
 * 실제 주기는 [LiveHeartbeatReporter]가 [upsert]를 호출한다.
 */
object DeviceStatusReporter {
    private const val TAG = "ONDA_DEVICE_STATUS"
    /** 최근 fix를 gps_ok=true로 볼 최대 나이 */
    private const val FIX_FRESH_MS = 60_000L

    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        // Context 는 OperationLocationTracker.appContextOrNull() 로 읽음
    }

    fun start(@Suppress("UNUSED_PARAMETER") operationId: String) {
        // LiveHeartbeatReporter 가 주기 upsert — 중복 루프 방지
    }

    fun stop() {
        // no-op
    }

    suspend fun upsert(
        operationId: String,
        fix: LatestLocationHolder.Fix?,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            Log.d(TAG, "skip: supabase not ready")
            return@withContext false
        }
        val opUuid = OperationGpsApi.resolveOperationUuid(operationId) ?: run {
            Log.w(TAG, "skip: operation unresolved ($operationId)")
            return@withContext false
        }

        val ctx = OperationLocationTracker.appContextOrNull()
        val gpsEnabled = ctx?.let { OperationDeviceStatus.isGpsEnabled(it) } ?: false
        val hasPermission = ctx?.let { OperationDeviceStatus.hasLocationPermission(it) } ?: false
        val matchingFix = fix?.takeIf { it.operationId == operationId || it.operationId.isBlank() }
        val recentOk = matchingFix != null &&
            System.currentTimeMillis() - matchingFix.recordedAtMillis < FIX_FRESH_MS
        val gpsOk = gpsEnabled && hasPermission && recentOk
        val nowIso = Instant.now().toString()
        val lastLocationAt = matchingFix?.let { Instant.ofEpochMilli(it.recordedAtMillis).toString() }
        val lastAccuracy = matchingFix?.accuracy?.toDouble()

        val body = JSONObject().apply {
            put("operation_id", opUuid)
            put("gps_ok", gpsOk)
            put("gps_enabled", gpsEnabled)
            // 마지막 정상 위치만 갱신. 없으면 필드 생략 → merge 시 기존 값 유지
            if (lastLocationAt != null) put("last_location_at", lastLocationAt)
            if (lastAccuracy != null) put("last_accuracy", lastAccuracy)
            put("updated_at", nowIso)
        }

        val result = SupabaseClient.request(
            method = "POST",
            path = "/rest/v1/operation_device_status?on_conflict=operation_id",
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
            "device status operationId=$opUuid, gpsEnabled=$gpsEnabled, gpsOk=$gpsOk, lastLocationAt=${lastLocationAt ?: "-"}",
        )
        true
    }
}
