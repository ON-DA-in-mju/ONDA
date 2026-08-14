package com.onda.mju.student

import android.app.Application
import android.util.Log
import com.naver.maps.map.NaverMapSdk
import com.onda.mju.student.data.remote.SupabaseClientProvider

class StudentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Auth 세션 복원을 앱 기동 직후부터 시작해 스플래시 대기 체감을 줄인다.
        runCatching { SupabaseClientProvider.client }
        val clientId = BuildConfig.NAVER_MAP_CLIENT_ID.trim()
        if (clientId.isEmpty()) {
            Log.w(TAG, "NAVER_MAP_CLIENT_ID is empty. Add it to local.properties and rebuild.")
            return
        }
        NaverMapSdk.getInstance(this).setClient(
            NaverMapSdk.NcpKeyClient(clientId),
        )
        NaverMapSdk.getInstance(this).setOnAuthFailedListener { error ->
            Log.e(TAG, "Naver Map auth failed: ${error.message}", error)
        }
    }

    companion object {
        private const val TAG = "ONDA_NAVER_MAP"
    }
}
