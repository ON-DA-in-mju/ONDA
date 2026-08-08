package com.mju.onda.driver.core.location

import android.util.Log
import com.mju.onda.driver.BuildConfig
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 운행 중 관리자 웹(`/api/live/heartbeat`)으로 상태·위치를 주기 전송한다.
 */
object LiveHeartbeatReporter {
    private const val TAG = "LiveHeartbeat"
    private const val INTERVAL_MS = 10_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    fun start(operationId: String) {
        loopJob?.cancel()
        loopJob = scope.launch {
            postOnce(operationId, status = "in_progress")
            while (isActive) {
                delay(INTERVAL_MS)
                if (!OperationLocationTracker.isTracking) break
                val id = OperationLocationTracker.activeOperationId ?: operationId
                postOnce(id, status = "in_progress")
            }
        }
    }

    fun stopAndMarkEnded(operationId: String?) {
        loopJob?.cancel()
        loopJob = null
        val id = operationId ?: return
        scope.launch {
            postOnce(id, status = "ended", clearLocation = true)
        }
    }

    private suspend fun postOnce(
        operationId: String,
        status: String,
        clearLocation: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        val base = BuildConfig.ADMIN_DEV_BASE_URL.trimEnd('/')
        if (base.isBlank()) return@withContext

        val driverId = SessionStateHolder.currentUserId ?: return@withContext
        val account = AccountInfoStateHolder.get()
        val op = MockTodayOperations.findById(operationId)
        val fix = if (clearLocation) null else LatestLocationHolder.latest
        val gpsError = status == "in_progress" && fix == null

        var conn: HttpURLConnection? = null
        try {
            val url = URL("$base/api/live/heartbeat")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3_000
                readTimeout = 3_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val body = buildString {
                append('{')
                append("\"driverId\":").append(jsonString(driverId)).append(',')
                append("\"driverName\":").append(jsonString(stripHonorific(account.driverName))).append(',')
                append("\"vehicleName\":").append(jsonString(account.vehicleName)).append(',')
                append("\"routeName\":").append(jsonString(op?.routeName ?: "-")).append(',')
                append("\"operationId\":").append(jsonString(operationId)).append(',')
                append("\"status\":").append(jsonString(status)).append(',')
                append("\"gpsError\":").append(gpsError)
                if (fix != null) {
                    append(",\"lat\":").append(fix.latitude)
                    append(",\"lng\":").append(fix.longitude)
                    append(",\"accuracy\":").append(fix.accuracy)
                } else {
                    append(",\"lat\":null,\"lng\":null,\"accuracy\":null")
                }
                append('}')
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.d(TAG, "heartbeat failed: HTTP $code")
            }
        } catch (e: Exception) {
            Log.d(TAG, "heartbeat skipped: ${e.message}")
        } finally {
            conn?.disconnect()
        }
    }

    private fun stripHonorific(name: String): String =
        name.removeSuffix(" 기사님").removeSuffix("기사님").trim().ifBlank { name }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
