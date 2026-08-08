package com.mju.onda.driver.core.location



import android.Manifest

import android.content.Context

import android.content.Intent

import android.content.pm.PackageManager

import android.os.Build

import androidx.core.content.ContextCompat

import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder



/**

 * 운행 중(RUNNING)에만 GPS 수집을 시작하고, 종료 시 즉시 중단한다.

 * 명세: 수집 시점 = 운행 시작, 종료 시점 = 운행 종료.

 */

object OperationLocationTracker {

    @Volatile

    private var appContext: Context? = null



    @Volatile

    var isTracking: Boolean = false

        internal set



    @Volatile

    var activeOperationId: String? = null

        internal set



    fun init(context: Context) {

        appContext = context.applicationContext

    }



    fun hasLocationPermission(context: Context? = appContext): Boolean {

        val ctx = context ?: return false

        val fine = ContextCompat.checkSelfPermission(

            ctx,

            Manifest.permission.ACCESS_FINE_LOCATION,

        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(

            ctx,

            Manifest.permission.ACCESS_COARSE_LOCATION,

        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse

    }



    /** 운행 시작 시 호출 */

    fun startForOperation(operationId: String) {

        val ctx = appContext ?: return

        if (!hasLocationPermission(ctx)) {

            isTracking = false

            activeOperationId = null

            return

        }

        activeOperationId = operationId

        val intent = Intent(ctx, OperationLocationService::class.java).apply {

            action = OperationLocationService.ACTION_START

            putExtra(OperationLocationService.EXTRA_OPERATION_ID, operationId)

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            ContextCompat.startForegroundService(ctx, intent)

        } else {

            ctx.startService(intent)

        }

        isTracking = true

    }



    /** 운행 종료·중단·강제종료·초기화 시 호출 */

    fun stop() {

        val ctx = appContext ?: return

        isTracking = false

        activeOperationId = null

        val intent = Intent(ctx, OperationLocationService::class.java).apply {

            action = OperationLocationService.ACTION_STOP

        }

        // startService로 STOP을 보내 서비스 내부에서 정리 후 stopSelf

        try {

            ctx.startService(intent)

        } catch (_: Exception) {

            ctx.stopService(Intent(ctx, OperationLocationService::class.java))

        }

    }



    /** 앱 재실행·계정 바인딩 후 운행 중이면 재개, 아니면 중단 */

    fun syncWithRuntime() {

        val activeId = OperationRuntimeStateHolder.activeOperationId()

        if (activeId != null) {

            startForOperation(activeId)

        } else {

            stop()

        }

    }

}


