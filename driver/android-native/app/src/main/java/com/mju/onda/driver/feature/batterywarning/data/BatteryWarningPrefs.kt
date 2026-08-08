package com.mju.onda.driver.feature.batterywarning.data



import android.content.SharedPreferences

import com.mju.onda.driver.core.UserScopedPrefs



/**

 * 충전기 연결 확인 후 복구 화면의 배터리 경고 배너 숨김. 계정별 저장.

 */

object BatteryWarningPrefs {

    private const val PREFS = "onda_battery_warning"

    private const val KEY_RESOLVED = "charger_resolved"



    private var prefs: SharedPreferences? = null



    @Volatile

    var chargerResolved: Boolean = false

        private set



    fun bindUser() {

        prefs = UserScopedPrefs.get(PREFS)

        chargerResolved = prefs?.getBoolean(KEY_RESOLVED, false) == true

    }



    fun unbindUser() {

        chargerResolved = false

        prefs = null

    }



    fun markResolved() {

        chargerResolved = true

        prefs?.edit()?.putBoolean(KEY_RESOLVED, true)?.commit()

    }



    fun clear() {

        chargerResolved = false

        prefs?.edit()?.clear()?.apply()

    }

}

