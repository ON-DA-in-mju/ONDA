package com.mju.onda.driver.core

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/** 앱 설치당 1회 발급되는 기기 ID. 계정과 무관하다. */
object DeviceSessionId {
    private const val PREFS = "onda_device_install"
    private const val KEY_ID = "device_id"

    private var prefs: SharedPreferences? = null

    @Volatile
    private var cached: String = ""

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        cached = prefs?.getString(KEY_ID, null).orEmpty()
        if (cached.isBlank()) {
            cached = UUID.randomUUID().toString()
            prefs?.edit()?.putString(KEY_ID, cached)?.apply()
        }
    }

    fun get(): String {
        if (cached.isNotBlank()) return cached
        val stored = prefs?.getString(KEY_ID, null).orEmpty()
        if (stored.isNotBlank()) {
            cached = stored
            return stored
        }
        val created = UUID.randomUUID().toString()
        cached = created
        prefs?.edit()?.putString(KEY_ID, created)?.apply()
        return created
    }
}
