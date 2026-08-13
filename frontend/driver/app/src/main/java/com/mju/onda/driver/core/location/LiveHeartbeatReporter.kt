package com.mju.onda.driver.core.location

import android.util.Log
import com.mju.onda.driver.BuildConfig
import com.mju.onda.driver.core.system.SystemLogsApi
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
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
    /** vehicle_locations / device_status / admin heartbeat 공통 주기 */
    private const val INTERVAL_MS = 3_000L

    /** opId별로 GPS를 한 번이라도 받은 적 있는지 (출발 직후 미수신은 오류 로그 제외) */
    private val hadFixByOpId: MutableMap<String, Boolean> = mutableMapOf()
    /** opId별로 GPS 에러 전환 시에만 system_logs를 찍기 위한 캐시 */
    private val lastGpsErrorByOpId: MutableMap<String, Boolean> = mutableMapOf()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    fun start(operationId: String) {
        loopJob?.cancel()
        synchronized(hadFixByOpId) { hadFixByOpId.remove(operationId) }
        synchronized(lastGpsErrorByOpId) { lastGpsErrorByOpId.remove(operationId) }
        OperationStopProgressCoordinator.attach(operationId)
        loopJob = scope.launch {
            postOnce(operationId, status = "in_progress")
            while (isActive) {
                delay(INTERVAL_MS)
                // 위치 서비스가 꺼져 FGS가 죽어도(isTracking=false) 운행 중이면 heartbeat 유지.
                // 학생 앱은 updated_at 30초 초과를 "네트워크 이상"으로 본다.
                val id = OperationRuntimeStateHolder.activeOperationId()
                    ?: OperationLocationTracker.activeOperationId
                    ?: operationId
                val stillRunning = OperationRuntimeStateHolder.activeOperationId() != null ||
                    OperationLocationTracker.activeOperationId != null
                if (!stillRunning) break
                // 위치 OFF로 FGS가 죽었어도 heartbeat는 유지. 위치 ON이면 수집만 재개.
                val ctx = OperationLocationTracker.appContextOrNull()
                if (!OperationLocationTracker.isTracking &&
                    ctx != null &&
                    OperationDeviceStatus.isGpsEnabled(ctx)
                ) {
                    OperationLocationTracker.startForOperation(id)
                }
                postOnce(id, status = "in_progress")
            }
        }
    }

    fun stopAndMarkEnded(operationId: String?) {
        loopJob?.cancel()
        loopJob = null
        OperationStopProgressCoordinator.clear()
        val id = operationId ?: return
        synchronized(hadFixByOpId) { hadFixByOpId.remove(id) }
        synchronized(lastGpsErrorByOpId) { lastGpsErrorByOpId.remove(id) }
        scope.launch {
            postOnce(id, status = "ended", clearLocation = true)
        }
    }

    private suspend fun postOnce(
        operationId: String,
        status: String,
        clearLocation: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        val driverId = SessionStateHolder.currentUserId ?: return@withContext
        val account = AccountInfoStateHolder.get()
        val op = MockTodayOperations.findById(operationId)
        val fix = if (clearLocation) null else LatestLocationHolder.latest
        val gpsError = status == "in_progress" && fix == null

        // 출발 직후 GPS 미수신은 정상 대기 → 한 번이라도 수신된 뒤 끊긴 경우만 오류 로그
        if (status == "in_progress" && fix != null) {
            synchronized(hadFixByOpId) { hadFixByOpId[operationId] = true }
            synchronized(lastGpsErrorByOpId) { lastGpsErrorByOpId[operationId] = false }
        } else if (gpsError) {
            val hadFix = synchronized(hadFixByOpId) { hadFixByOpId[operationId] == true }
            val prev = synchronized(lastGpsErrorByOpId) { lastGpsErrorByOpId[operationId] == true }
            if (hadFix && !prev) {
                val vehicle = op?.vehicleName?.takeIf { it.isNotBlank() }
                    ?: account.vehicleName.takeIf { it.isNotBlank() }
                    ?: "미정"
                runCatching {
                    SystemLogsApi.insert(
                        type = "오류 발생",
                        action = "차량 위치 정보 수신 실패 (연결 끊김)",
                        actor = "시스템",
                        target = "차량: $vehicle",
                        result = "경고",
                    )
                }
            }
            synchronized(lastGpsErrorByOpId) { lastGpsErrorByOpId[operationId] = true }
        }

        val base = BuildConfig.ADMIN_DEV_BASE_URL.trimEnd('/')
        if (base.isNotBlank()) {
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

        // 관리자 웹 수신과 별도로 Supabase operation_logs / vehicle_locations에 GPS 적재
        if (status == "in_progress" && fix != null) {
            runCatching {
                OperationGpsApi.report(
                    operationId = operationId,
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracy = fix.accuracy,
                    vehicleName = account.vehicleName,
                    driverName = stripHonorific(account.driverName),
                    recordedAtMillis = fix.recordedAtMillis,
                )
            }.onFailure { e ->
                Log.d(TAG, "gps db write skipped: ${e.message}")
            }
        }

        // 학생 앱용 기기/GPS heartbeat (fix 없어도 updated_at 갱신)
        if (status == "in_progress") {
            runCatching {
                DeviceStatusReporter.upsert(
                    operationId = operationId,
                    fix = fix?.takeIf { it.operationId == operationId || it.operationId.isBlank() },
                )
            }.onFailure { e ->
                Log.d(TAG, "device status upsert skipped: ${e.message}")
            }
        }
    }

    private fun stripHonorific(name: String): String =
        name.removeSuffix(" 기사님").removeSuffix("기사님").trim().ifBlank { name }

    private fun jsonString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
