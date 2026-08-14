package com.onda.mju.student.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Local permission grant snapshot after the system dialogs.
 * Replace with a persisted preference layer later if needed.
 */
data class PermissionGrantState(
    val locationGranted: Boolean,
    val notificationGranted: Boolean,
) {
    /** 위치 권한이 있으면 권한 안내 화면을 건너뛰어도 된다. */
    val isReadyForMain: Boolean get() = locationGranted
}

fun currentPermissionGrantState(context: Context): PermissionGrantState {
    val locationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    return PermissionGrantState(
        locationGranted = locationGranted,
        notificationGranted = notificationGranted,
    )
}
