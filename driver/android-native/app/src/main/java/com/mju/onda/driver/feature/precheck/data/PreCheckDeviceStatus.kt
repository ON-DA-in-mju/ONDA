package com.mju.onda.driver.feature.precheck.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Battery3Bar
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * 운행 전 점검 항목을 실제 기기·권한 상태로 구성한다.
 * 위치 수집 자체는 운행 시작~종료에만 수행하며,
 * 여기서는 “수집 가능한 준비가 됐는지”를 확인한다.
 */
object PreCheckDeviceStatus {

    private const val TAG = "PreCheckDeviceStatus"

    fun buildItems(context: Context): List<PreCheckItem> {
        return try {
            buildItemsInternal(context.applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, "buildItems failed", t)
            // 크래시 대신 조치 필요 목록으로 폴백
            listOf(
                PreCheckItem(
                    id = "location",
                    icon = Icons.Outlined.LocationOn,
                    label = "위치 권한",
                    detail = "확인 실패",
                    status = CheckStatus.ActionRequired,
                ),
                PreCheckItem(
                    id = "precise",
                    icon = Icons.Outlined.MyLocation,
                    label = "정확한 위치",
                    detail = "확인 실패",
                    status = CheckStatus.ActionRequired,
                ),
                PreCheckItem(
                    id = "background",
                    icon = Icons.Outlined.Layers,
                    label = "백그라운드 위치",
                    detail = "확인 실패",
                    status = CheckStatus.ActionRequired,
                ),
                PreCheckItem(
                    id = "gps",
                    icon = Icons.Outlined.Settings,
                    label = "GPS",
                    detail = "확인 실패",
                    status = CheckStatus.ActionRequired,
                ),
                PreCheckItem(
                    id = "network",
                    icon = Icons.Outlined.Wifi,
                    label = "네트워크",
                    detail = "확인 실패",
                    status = CheckStatus.ActionRequired,
                ),
                serverItem(),
                PreCheckItem(
                    id = "battery",
                    icon = Icons.Outlined.Battery3Bar,
                    label = "배터리",
                    detail = "확인 실패",
                    status = CheckStatus.Caution,
                ),
                PreCheckItem(
                    id = "powersave",
                    icon = Icons.Outlined.EnergySavingsLeaf,
                    label = "절전 모드",
                    detail = "확인 실패",
                    status = CheckStatus.Caution,
                ),
            )
        }
    }

    private fun buildItemsInternal(context: Context): List<PreCheckItem> {
        val fineGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseGranted = hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val backgroundGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            fineGranted || coarseGranted
        }
        val locationEnabled = isLocationEnabled(context)

        val locationItem = when {
            fineGranted || coarseGranted ->
                PreCheckItem("location", Icons.Outlined.LocationOn, "위치 권한", "허용됨", CheckStatus.Normal)
            else ->
                PreCheckItem("location", Icons.Outlined.LocationOn, "위치 권한", "거부됨", CheckStatus.ActionRequired)
        }

        val preciseItem = when {
            fineGranted ->
                PreCheckItem("precise", Icons.Outlined.MyLocation, "정확한 위치", "사용 중", CheckStatus.Normal)
            coarseGranted ->
                PreCheckItem("precise", Icons.Outlined.MyLocation, "정확한 위치", "대략적만", CheckStatus.ActionRequired)
            else ->
                PreCheckItem("precise", Icons.Outlined.MyLocation, "정확한 위치", "미사용", CheckStatus.ActionRequired)
        }

        val backgroundItem = when {
            backgroundGranted ->
                PreCheckItem("background", Icons.Outlined.Layers, "백그라운드 위치", "허용됨", CheckStatus.Normal)
            else ->
                PreCheckItem("background", Icons.Outlined.Layers, "백그라운드 위치", "거부됨", CheckStatus.ActionRequired)
        }

        val gpsItem = if (locationEnabled) {
            PreCheckItem("gps", Icons.Outlined.Settings, "GPS", "켜짐", CheckStatus.Normal)
        } else {
            PreCheckItem("gps", Icons.Outlined.Settings, "GPS", "꺼짐", CheckStatus.ActionRequired)
        }

        return listOf(
            locationItem,
            preciseItem,
            backgroundItem,
            gpsItem,
            networkItem(context),
            serverItem(),
            batteryItem(context),
            powerSaveItem(context),
        )
    }

    private fun networkItem(context: Context): PreCheckItem {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            val connected = caps != null && (
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            if (connected) {
                PreCheckItem("network", Icons.Outlined.Wifi, "네트워크", "연결됨", CheckStatus.Normal)
            } else {
                PreCheckItem("network", Icons.Outlined.Wifi, "네트워크", "끊김", CheckStatus.ActionRequired)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "network check failed", t)
            PreCheckItem("network", Icons.Outlined.Wifi, "네트워크", "끊김", CheckStatus.ActionRequired)
        }
    }

    /** 서버 미연동 MVP — 시연용으로 연결됨 표시 */
    private fun serverItem(): PreCheckItem =
        PreCheckItem("server", Icons.Outlined.CloudQueue, "서버 연결", "연결됨", CheckStatus.Normal)

    /**
     * Android 13+ 에서 registerReceiver(null, …) 는 RECEIVER 플래그 없이
     * SecurityException 으로 앱이 죽을 수 있어 BatteryManager 를 사용한다.
     */
    private fun batteryItem(context: Context): PreCheckItem {
        val pct = try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (t: Throwable) {
            Log.w(TAG, "battery check failed", t)
            -1
        }

        return when {
            pct < 0 -> PreCheckItem(
                "battery",
                Icons.Outlined.Battery3Bar,
                "배터리",
                "확인 불가",
                CheckStatus.Caution,
            )
            pct < 20 -> PreCheckItem(
                "battery",
                Icons.Outlined.Battery3Bar,
                "배터리",
                "$pct%",
                CheckStatus.ActionRequired,
            )
            pct < 30 -> PreCheckItem(
                "battery",
                Icons.Outlined.Battery3Bar,
                "배터리",
                "$pct%",
                CheckStatus.Caution,
            )
            else -> PreCheckItem(
                "battery",
                Icons.Outlined.Battery3Bar,
                "배터리",
                "$pct%",
                CheckStatus.Normal,
            )
        }
    }

    private fun powerSaveItem(context: Context): PreCheckItem {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isPowerSaveMode) {
                PreCheckItem(
                    "powersave",
                    Icons.Outlined.EnergySavingsLeaf,
                    "절전 모드",
                    "사용 중",
                    CheckStatus.ActionRequired,
                )
            } else {
                PreCheckItem(
                    "powersave",
                    Icons.Outlined.EnergySavingsLeaf,
                    "절전 모드",
                    "해제됨",
                    CheckStatus.Normal,
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "power-save check failed", t)
            PreCheckItem(
                "powersave",
                Icons.Outlined.EnergySavingsLeaf,
                "절전 모드",
                "확인 실패",
                CheckStatus.Caution,
            )
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            LocationManagerCompat.isLocationEnabled(lm)
        } catch (t: Throwable) {
            Log.w(TAG, "location enabled check failed", t)
            false
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
