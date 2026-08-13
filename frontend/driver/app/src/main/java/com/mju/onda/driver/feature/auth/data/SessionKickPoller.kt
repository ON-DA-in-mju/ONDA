package com.mju.onda.driver.feature.auth.data

import android.util.Log
import com.mju.onda.driver.core.DeviceSessionId
import com.mju.onda.driver.core.location.OperationLocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 다른 기기가 같은 계정으로 로그인하면 GPS를 끊고 강제 로그아웃한다.
 */
object SessionKickPoller {
    private const val TAG = "SessionKick"
    private const val INTERVAL_MS = 2_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    @Volatile
    private var handlingKick: Boolean = false

    private val _kicked = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val kicked: SharedFlow<Unit> = _kicked.asSharedFlow()

    fun start() {
        if (loopJob?.isActive == true) return
        handlingKick = false
        loopJob = scope.launch {
            pollOnce()
            while (isActive) {
                delay(INTERVAL_MS)
                pollOnce()
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        handlingKick = false
    }

    fun pollOnce() {
        if (handlingKick) return
        if (!SessionStateHolder.isLoggedIn) return
        val mine = DeviceSessionId.get()
        when (val owner = DriverActiveSessionApi.fetchOwner()) {
            DriverActiveSessionApi.FetchResult.Unavailable -> return
            DriverActiveSessionApi.FetchResult.None -> {
                DriverActiveSessionApi.claimExclusiveBlocking()
            }
            is DriverActiveSessionApi.FetchResult.Owner -> {
                if (owner.deviceId == mine) return
                Log.w(TAG, "kicked: owner=${owner.deviceId} mine=$mine")
                applyKick()
            }
        }
    }

    private fun applyKick() {
        if (handlingKick) return
        handlingKick = true
        OperationLocationTracker.stop()
        SessionStateHolder.clear()
        _kicked.tryEmit(Unit)
    }
}
