package com.mju.onda.driver.core.login

import android.util.Log
import com.mju.onda.driver.BuildConfig
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 관리자 웹(Vite dev)의 `/api/login-history`로 로그인 이벤트를 전송한다.
 * 에뮬레이터 기본 호스트: 10.0.2.2 → PC localhost.
 * 관리자 웹이 떠 있지 않으면 조용히 실패한다.
 */
object LoginHistoryReporter {
    private const val TAG = "LoginHistoryReporter"

    suspend fun report(userId: String, name: String) = withContext(Dispatchers.IO) {
        val base = BuildConfig.ADMIN_DEV_BASE_URL.trimEnd('/')
        if (base.isBlank()) return@withContext

        var conn: HttpURLConnection? = null
        try {
            val url = URL("$base/api/login-history")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3_000
                readTimeout = 3_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val body =
                """{"userId":${jsonString(userId)},"name":${jsonString(name)},"source":"driver-app","ip":"10.0.2.2"}"""
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.d(TAG, "login history POST failed: HTTP $code")
            }
        } catch (e: Exception) {
            Log.d(TAG, "login history POST skipped: ${e.message}")
        } finally {
            conn?.disconnect()
        }
    }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
