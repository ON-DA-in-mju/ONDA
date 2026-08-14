package com.mju.onda.driver.core.system

import android.util.Log
import com.mju.onda.driver.core.supabase.SupabaseClient
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Supabase `system_logs` 기록용 (관리자 웹의 "시스템 기록 목록" 데이터 소스).
 *
 * 기사 역할은 INSERT만 허용되고 SELECT는 관리자만 가능한 경우가 많으므로,
 * POST 시 Prefer: return=minimal 을 사용한다 (return=representation 이면 RLS로 실패할 수 있음).
 */
object SystemLogsApi {
    private const val TAG = "SystemLogsApi"

    /**
     * 운행 시작/종료 상태 변경 기록.
     * - id: DB uuid 자동 생성
     * - ip: 네트워크 IP 대신 당시 GPS(위도,경도)를 저장 (없으면 null)
     */
    suspend fun logOperationStatusChange(
        vehicleName: String,
        statusLabel: String,
        actor: String,
        success: Boolean,
        gpsIp: String? = null,
    ): Boolean {
        val vehicle = vehicleName.trim().ifBlank { "미정" }
        return insert(
            type = "운행 변경",
            action = "${vehicle} 차량의 상태를 '${statusLabel}'(으)로 변경",
            actor = actor,
            ip = gpsIp,
            target = "차량:$vehicle",
            result = if (success) "성공" else "실패",
        )
    }

    suspend fun insert(
        type: String,
        action: String,
        actor: String? = null,
        ip: String? = null,
        target: String? = null,
        result: String,
        loggedAt: String = Instant.now().toString(),
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured || SupabaseClient.accessToken.isNullOrBlank()) {
            Log.w(TAG, "skip insert: supabase not ready (configured=${SupabaseClient.isConfigured}, tokenBlank=${SupabaseClient.accessToken.isNullOrBlank()})")
            return@withContext false
        }

        val uid = SupabaseClient.userUuid
        val body = JSONObject().apply {
            put("type", type)
            put("action", action)
            if (!uid.isNullOrBlank()) put("actor_id", uid)
            put("ip", ip ?: JSONObject.NULL)
            put("target", target ?: JSONObject.NULL)
            put("result", result)
            put("logged_at", loggedAt)
        }.toString()

        // SELECT RLS가 관리자만 허용이면 representation 반환이 실패 → minimal 사용
        val resultHttp = SupabaseClient.request(
            method = "POST",
            path = "/rest/v1/system_logs",
            jsonBody = body,
            authed = true,
            prefer = "return=minimal",
        )

        if (resultHttp.code !in 200..299) {
            Log.w(TAG, "insert failed HTTP ${resultHttp.code}: ${resultHttp.body.take(400)}")
            return@withContext false
        }
        Log.d(TAG, "insert ok type=$type result=$result")
        true
    }
}
