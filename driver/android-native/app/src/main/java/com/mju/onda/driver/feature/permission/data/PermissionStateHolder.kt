package com.mju.onda.driver.feature.permission.data

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mju.onda.driver.core.UserScopedPrefs
import com.mju.onda.driver.core.location.OperationDeviceStatus

/**
 * 권한 상태를 OS에서 읽어 화면 간에 공유한다.
 * [skippedForLater]만 계정별 prefs에 유지하고, 허용 여부는 항상 [syncFromSystem]으로 갱신한다.
 */
object PermissionStateHolder {
    private const val PREFS = "onda_permission_mock"
    private const val KEY_SKIPPED = "skipped"

    private var prefs: SharedPreferences? = null

    @Volatile
    var preciseLocationGranted: Boolean = false
        private set

    @Volatile
    var whenInUseLocationGranted: Boolean = false
        private set

    @Volatile
    var backgroundLocationGranted: Boolean = false
        private set

    @Volatile
    var notificationGranted: Boolean = false
        private set

    @Volatile
    var skippedForLater: Boolean = false
        private set

    /** 운행 시작에 필요한 최소 권한 (사용 중 위치 + 정확한 위치) */
    val hasRequiredPermissions: Boolean
        get() = preciseLocationGranted && whenInUseLocationGranted

    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        skippedForLater = prefs?.getBoolean(KEY_SKIPPED, false) == true
        // 허용 플래그는 Context가 있을 때 syncFromSystem으로 채운다
        preciseLocationGranted = false
        whenInUseLocationGranted = false
        backgroundLocationGranted = false
        notificationGranted = false
    }

    fun unbindUser() {
        preciseLocationGranted = false
        whenInUseLocationGranted = false
        backgroundLocationGranted = false
        notificationGranted = false
        skippedForLater = false
        prefs = null
    }

    /** OS 권한·알림 설정으로 캐시를 갱신한다. */
    fun syncFromSystem(context: Context) {
        val appContext = context.applicationContext
        val fine = hasPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = hasPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        preciseLocationGranted = fine
        whenInUseLocationGranted = fine || coarse
        backgroundLocationGranted = OperationDeviceStatus.hasBackgroundLocationPermission(appContext)
        notificationGranted = areNotificationsEnabled(appContext)
    }

    fun hasRequiredPermissions(context: Context): Boolean {
        syncFromSystem(context)
        return hasRequiredPermissions
    }

    fun markSkippedForLater() {
        skippedForLater = true
        persistSkipped()
    }

    fun clearSkippedForLater() {
        skippedForLater = false
        persistSkipped()
    }

    fun reset() {
        preciseLocationGranted = false
        whenInUseLocationGranted = false
        backgroundLocationGranted = false
        notificationGranted = false
        skippedForLater = false
        prefs?.edit()?.clear()?.apply()
    }

    private fun persistSkipped() {
        prefs?.edit()?.putBoolean(KEY_SKIPPED, skippedForLater)?.apply()
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun areNotificationsEnabled(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        }
        return true
    }
}
