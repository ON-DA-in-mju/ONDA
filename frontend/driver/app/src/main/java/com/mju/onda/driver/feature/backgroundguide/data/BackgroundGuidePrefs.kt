package com.mju.onda.driver.feature.backgroundguide.data



import android.content.SharedPreferences

import com.mju.onda.driver.core.UserScopedPrefs



/**

 * "다시 보지 않기" 선택 시 복구 화면 안내 블록에서 이 화면을 열지 않음. 계정별 저장.

 */

object BackgroundGuidePrefs {

    private const val PREFS = "onda_background_guide"

    private const val KEY_DONT_SHOW = "dont_show_again"



    private var prefs: SharedPreferences? = null



    @Volatile

    var dontShowAgain: Boolean = false

        private set



    fun bindUser() {

        prefs = UserScopedPrefs.get(PREFS)

        dontShowAgain = prefs?.getBoolean(KEY_DONT_SHOW, false) == true

    }



    fun unbindUser() {

        dontShowAgain = false

        prefs = null

    }



    fun markDontShowAgain() {

        dontShowAgain = true

        prefs?.edit()?.putBoolean(KEY_DONT_SHOW, true)?.commit()

    }



    fun clear() {

        dontShowAgain = false

        prefs?.edit()?.clear()?.apply()

    }

}

