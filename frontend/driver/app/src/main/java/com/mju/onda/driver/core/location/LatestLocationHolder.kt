package com.mju.onda.driver.core.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 운행 중 수집된 최신 위치 (서버 업로드 전 로컬 보관).
 */
object LatestLocationHolder {

    data class Fix(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val recordedAtMillis: Long,
        val operationId: String,
    )

    private val _latest = MutableStateFlow<Fix?>(null)
    val latestFlow: StateFlow<Fix?> = _latest.asStateFlow()

    val latest: Fix?
        get() = _latest.value

    fun update(
        operationId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        recordedAtMillis: Long = System.currentTimeMillis(),
    ) {
        _latest.value = Fix(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            recordedAtMillis = recordedAtMillis,
            operationId = operationId,
        )
    }

    fun clear() {
        _latest.value = null
    }
}
