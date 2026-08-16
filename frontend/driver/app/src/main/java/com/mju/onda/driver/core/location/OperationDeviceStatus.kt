package com.mju.onda.driver.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat

/** 운행 시작·상세 상태 등에서 기기/전송 상태를 공통으로 읽는다. */
object OperationDeviceStatus {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return hasLocationPermission(context)
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Android 시스템 위치 서비스(Location) ON/OFF.
     * 위치 권한 여부와 무관하다. `operation_device_status.gps_enabled` 와 동일 의미.
     */
    fun isGpsEnabled(context: Context): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: Throwable) {
            false
        }
    }

    fun formatLastTransmission(fromMillis: Long?): String {
        if (fromMillis == null || fromMillis <= 0L) return "없음"
        val elapsed = System.currentTimeMillis() - fromMillis
        return when {
            elapsed < 15_000L -> "방금 전"
            elapsed < 60_000L -> "${elapsed / 1000}초 전"
            elapsed < 3_600_000L -> "${elapsed / 60_000}분 전"
            else -> "${elapsed / 3_600_000}시간 전"
        }
    }

    data class TransmissionSnapshot(
        val lastTransmissionLabel: String,
        val networkLabel: String,
        val serverLabel: String,
        val locationBadgeLabel: String,
        /** 배지용 짧은 문구 (예: 정상 전송 중 / 전송 중 / 이상) */
        val shortStatusLabel: String,
        val isOk: Boolean,
        val locationOkLabel: String,
    )

    fun transmissionSnapshot(context: Context, operationId: String? = null): TransmissionSnapshot {
        val networkOk = isNetworkConnected(context)
        val tracking = OperationLocationTracker.isTracking &&
            (operationId == null ||
                OperationLocationTracker.activeOperationId == null ||
                OperationLocationTracker.activeOperationId == operationId)
        val heartbeat = LiveHeartbeatReporter.isRunning()
        val fix = LatestLocationHolder.latest?.takeIf {
            operationId == null || it.operationId == operationId
        }
        val recentFix = fix != null &&
            System.currentTimeMillis() - fix.recordedAtMillis < 60_000L
        val sending = networkOk && (tracking || heartbeat)
        val shortStatus = when {
            tracking && networkOk && recentFix -> "정상 전송 중"
            tracking && networkOk -> "GPS 수신 대기"
            heartbeat && networkOk -> "전송 중"
            !networkOk -> "네트워크 끊김"
            else -> "이상"
        }

        return TransmissionSnapshot(
            lastTransmissionLabel = formatLastTransmission(fix?.recordedAtMillis),
            networkLabel = if (networkOk) "연결됨" else "끊김",
            serverLabel = when {
                sending -> "정상"
                !networkOk -> "끊김"
                else -> "대기"
            },
            locationBadgeLabel = "위치 전송 상태 | $shortStatus",
            shortStatusLabel = shortStatus,
            isOk = sending,
            locationOkLabel = when {
                tracking && recentFix && networkOk -> "위치 전송 정상"
                tracking && networkOk -> "GPS 수신 대기"
                sending -> "위치 전송 중"
                else -> "위치 전송 이상"
            },
        )
    }

    data class BatterySnapshot(
        val percent: Int,
        val isCharging: Boolean,
        val isPowerSave: Boolean,
    ) {
        /** 복구 화면 경고 배너 표시 기준 */
        val needsWarning: Boolean
            get() = (percent in 0..29 && !isCharging) || isPowerSave
    }

    fun batterySnapshot(context: Context): BatterySnapshot {
        var percent = -1
        var isCharging = false
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            percent = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (_: Throwable) {
            // fall through
        }
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(
                    null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
                )
            }
            if (percent < 0 && intent != null) {
                val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) percent = (level * 100) / scale
            }
            val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = intent?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0
        } catch (_: Throwable) {
            // keep defaults
        }
        val isPowerSave = try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isPowerSaveMode
        } catch (_: Throwable) {
            false
        }
        return BatterySnapshot(
            percent = percent,
            isCharging = isCharging,
            isPowerSave = isPowerSave,
        )
    }
}
