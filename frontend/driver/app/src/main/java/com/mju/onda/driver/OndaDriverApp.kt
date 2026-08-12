package com.mju.onda.driver



import android.app.Application

import com.mju.onda.driver.core.location.OperationLocationTracker

import com.mju.onda.driver.feature.auth.data.SessionStateHolder



class OndaDriverApp : Application() {

    override fun onCreate() {

        super.onCreate()

        com.mju.onda.driver.core.supabase.SupabaseClient.init(this)

        OperationLocationTracker.init(this)

        com.mju.onda.driver.core.location.DeviceStatusReporter.init(this)

        // 로그인 세션이 있으면 해당 계정 prefs를 바인딩한다.

        SessionStateHolder.init(this)

        // 이미 운행 중이면 GPS 수집 재개

        OperationLocationTracker.syncWithRuntime()

    }

}


