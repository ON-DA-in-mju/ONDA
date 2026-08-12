package com.mju.onda.driver.core.location



import android.app.Notification

import android.app.NotificationChannel

import android.app.NotificationManager

import android.app.PendingIntent

import android.app.Service

import android.content.Intent

import android.content.pm.ServiceInfo

import android.os.Build

import android.os.IBinder

import android.os.Looper

import android.os.SystemClock

import android.util.Log

import java.time.Instant

import androidx.core.app.NotificationCompat

import com.google.android.gms.location.LocationCallback

import com.google.android.gms.location.LocationRequest

import com.google.android.gms.location.LocationResult

import com.google.android.gms.location.LocationServices

import com.google.android.gms.location.Priority

import com.mju.onda.driver.MainActivity



/**

 * 운행 중에만 동작하는 위치 Foreground Service.

 * 5초 간격으로 GPS를 수집하며, STOP 시 즉시 해제한다.

 */

class OperationLocationService : Service() {



    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var operationId: String = ""



    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult) {

            val location = result.lastLocation ?: return

            val systemNow = System.currentTimeMillis()

            val locationTime = location.time

            val elapsedAgeMs = if (location.elapsedRealtimeNanos > 0L) {

                ((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L)

                    .coerceAtLeast(0L)

            } else {

                -1L

            }

            // recorded_at = 실제 측정 시각 (location.time 왜곡 시 elapsedRealtime 나이로 보정)

            val measuredAtMillis = if (elapsedAgeMs >= 0L) {

                systemNow - elapsedAgeMs

            } else {

                locationTime.takeIf { it > 0L } ?: systemNow

            }

            LatestLocationHolder.update(

                operationId = operationId,

                latitude = location.latitude,

                longitude = location.longitude,

                accuracy = location.accuracy,

                recordedAtMillis = measuredAtMillis,

            )

            val ageSec = if (elapsedAgeMs >= 0L) elapsedAgeMs / 1000.0

            else if (locationTime > 0L) (systemNow - locationTime) / 1000.0

            else 0.0

            Log.d(

                TIME_TAG,

                "location callback: lat=${location.latitude} lng=${location.longitude} " +

                    "location.time=$locationTime locationTimeIso=${iso(locationTime)} " +

                    "systemNow=$systemNow systemNowIso=${iso(systemNow)} " +

                    "elapsedAgeMs=$elapsedAgeMs ageSec=$ageSec " +

                    "measuredAtIso=${iso(measuredAtMillis)} batch=${result.locations.size}",

            )

            Log.d(

                TAG,

                "gps op=$operationId lat=${location.latitude} lng=${location.longitude} acc=${location.accuracy}",

            )

        }

    }



    override fun onBind(intent: Intent?): IBinder? = null



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            ACTION_STOP -> {

                stopTrackingInternal()

                stopForeground(STOP_FOREGROUND_REMOVE)

                stopSelf()

                return START_NOT_STICKY

            }

            else -> {

                operationId = intent?.getStringExtra(EXTRA_OPERATION_ID).orEmpty()

                startAsForeground()

                startLocationUpdates()

            }

        }

        return START_STICKY

    }



    override fun onDestroy() {

        stopTrackingInternal()

        super.onDestroy()

    }



    private fun startAsForeground() {

        createChannel()

        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            startForeground(

                NOTIFICATION_ID,

                notification,

                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,

            )

        } else {

            startForeground(NOTIFICATION_ID, notification)

        }

    }



    private fun startLocationUpdates() {

        if (!OperationLocationTracker.hasLocationPermission(this)) {

            Log.w(TAG, "location permission missing — stop service")

            stopTrackingInternal()

            stopForeground(STOP_FOREGROUND_REMOVE)

            stopSelf()

            return

        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)

            .setMinUpdateIntervalMillis(INTERVAL_MS)

            .setMaxUpdateDelayMillis(INTERVAL_MS)

            .build()

        try {

            fusedClient.requestLocationUpdates(

                request,

                locationCallback,

                Looper.getMainLooper(),

            )

            OperationLocationTracker.isTracking = true

        } catch (security: SecurityException) {

            Log.e(TAG, "requestLocationUpdates failed", security)

            stopSelf()

        }

    }



    private fun stopTrackingInternal() {

        try {

            fusedClient.removeLocationUpdates(locationCallback)

        } catch (_: Exception) {

            // ignore

        }

        LatestLocationHolder.clear()

        OperationLocationTracker.isTracking = false

        OperationLocationTracker.activeOperationId = null

    }



    private fun createChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(

            CHANNEL_ID,

            "운행 위치 전송",

            NotificationManager.IMPORTANCE_LOW,

        ).apply {

            description = "운행 중 차량 위치를 전송하는 동안 표시됩니다."

            setShowBadge(false)

        }

        manager.createNotificationChannel(channel)

    }



    private fun buildNotification(): Notification {

        val launchIntent = Intent(this, MainActivity::class.java).apply {

            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        }

        val pending = PendingIntent.getActivity(

            this,

            0,

            launchIntent,

            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,

        )

        return NotificationCompat.Builder(this, CHANNEL_ID)

            .setContentTitle("운행 중 위치 전송")

            .setContentText("운행이 종료되면 위치 수집이 중단됩니다.")

            .setSmallIcon(android.R.drawable.ic_menu_mylocation)

            .setOngoing(true)

            .setContentIntent(pending)

            .setCategory(NotificationCompat.CATEGORY_SERVICE)

            .setPriority(NotificationCompat.PRIORITY_LOW)

            .build()

    }



    companion object {

        const val ACTION_START = "com.mju.onda.driver.location.START"

        const val ACTION_STOP = "com.mju.onda.driver.location.STOP"

        const val EXTRA_OPERATION_ID = "operation_id"



        private const val TAG = "OperationLocation"

        private const val TIME_TAG = "ONDA_LOCATION_TIME"

        private fun iso(epochMs: Long): String =

            if (epochMs > 0L) Instant.ofEpochMilli(epochMs).toString() else "-"

        private const val CHANNEL_ID = "operation_location"

        private const val NOTIFICATION_ID = 1001

        /** Fused GPS 요청 주기 — heartbeat(3초)와 맞춰 최신 fix를 확보 */
        private const val INTERVAL_MS = 3_000L

    }

}


