package com.mju.onda.driver.feature.auth.data



import android.content.Context

import android.content.SharedPreferences

import com.mju.onda.driver.core.UserScopedPrefs

import com.mju.onda.driver.core.UserScopedState



/**

 * 로그인 세션.

 * - 전역: 자동 로그인 여부, (자동 로그인 시) 복원할 userId

 * - 계정별: 온보딩 완료·배정 로드 여부

 */

object SessionStateHolder {

    private const val PREFS = "onda_session"

    private const val KEY_LOGGED_IN = "logged_in"

    private const val KEY_USER_ID = "user_id"

    private const val KEY_AUTO_LOGIN = "auto_login"



    private const val USER_META = "onda_user_meta"

    private const val KEY_ONBOARDING_DONE = "onboarding_done"

    private const val KEY_ASSIGNMENTS_LOADED = "assignments_loaded"



    private var prefs: SharedPreferences? = null



    @Volatile

    var isLoggedIn: Boolean = false

        private set



    @Volatile

    var currentUserId: String? = null

        private set



    /** 로그인 시 체크한 자동 로그인 설정. 로그아웃해도 유지(다음 로그인 체크박스용). */

    @Volatile

    var autoLoginEnabled: Boolean = false

        private set



    @Volatile

    var onboardingDone: Boolean = false

        private set



    @Volatile

    var assignmentsLoaded: Boolean = false

        private set



    fun init(context: Context) {

        UserScopedPrefs.init(context)

        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        autoLoginEnabled = prefs?.getBoolean(KEY_AUTO_LOGIN, false) == true

        val savedLoggedIn = prefs?.getBoolean(KEY_LOGGED_IN, false) == true

        val savedUserId = prefs?.getString(KEY_USER_ID, null)



        if (autoLoginEnabled && savedLoggedIn && !savedUserId.isNullOrBlank()) {

            currentUserId = savedUserId

            isLoggedIn = true

            UserScopedState.bind(savedUserId)

            loadUserMeta()

        } else {

            // 자동 로그인 미사용이면 재실행 시 로그인 필요

            isLoggedIn = false

            currentUserId = null

            onboardingDone = false

            assignmentsLoaded = false

            prefs?.edit()

                ?.putBoolean(KEY_LOGGED_IN, false)

                ?.remove(KEY_USER_ID)

                ?.apply()

        }

    }



    fun markLoggedIn(userId: String, autoLogin: Boolean) {

        autoLoginEnabled = autoLogin

        currentUserId = userId

        isLoggedIn = true

        val editor = prefs?.edit()

            ?.putBoolean(KEY_AUTO_LOGIN, autoLogin)

        if (autoLogin) {

            editor

                ?.putBoolean(KEY_LOGGED_IN, true)

                ?.putString(KEY_USER_ID, userId)

                ?.apply()

        } else {

            editor

                ?.putBoolean(KEY_LOGGED_IN, false)

                ?.remove(KEY_USER_ID)

                ?.apply()

        }

        UserScopedState.bind(userId)

        loadUserMeta()

    }



    fun markOnboardingDone() {

        onboardingDone = true

        userMetaPrefs()?.edit()?.putBoolean(KEY_ONBOARDING_DONE, true)?.apply()

    }



    fun markAssignmentsLoaded() {

        assignmentsLoaded = true

        userMetaPrefs()?.edit()?.putBoolean(KEY_ASSIGNMENTS_LOADED, true)?.commit()

    }



    /** 로그아웃 — 계정 데이터·자동 로그인 설정은 유지하고 세션만 해제 */

    fun clear() {

        UserScopedState.unbind()

        isLoggedIn = false

        currentUserId = null

        onboardingDone = false

        assignmentsLoaded = false

        com.mju.onda.driver.core.supabase.SupabaseClient.clearSession()

        prefs?.edit()

            ?.putBoolean(KEY_LOGGED_IN, false)

            ?.remove(KEY_USER_ID)

            ?.apply()

    }



    private fun loadUserMeta() {

        val meta = userMetaPrefs()

        onboardingDone = meta?.getBoolean(KEY_ONBOARDING_DONE, false) == true

        assignmentsLoaded = meta?.getBoolean(KEY_ASSIGNMENTS_LOADED, false) == true

    }



    private fun userMetaPrefs(): SharedPreferences? =

        if (UserScopedPrefs.currentUserId != null) UserScopedPrefs.get(USER_META) else null

}

