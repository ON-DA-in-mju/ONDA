package com.onda.mju.student.data.permission

/**
 * Local permission grant snapshot after the system dialogs.
 * Replace with a persisted preference layer later if needed.
 */
data class PermissionGrantState(
    val locationGranted: Boolean,
    val notificationGranted: Boolean,
)
