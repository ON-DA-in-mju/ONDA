package com.onda.mju.student.data.auth

import android.content.Context

/**
 * 자동 로그인 선호 설정.
 * 실제 세션은 Supabase Auth가 로컬에 보관하며, 이 플래그가 켜져 있을 때만
 * 앱 시작 시 저장된 세션으로 메인 진입을 시도한다.
 */
class AutoLoginPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    var savedStudentId: String
        get() = prefs.getString(KEY_STUDENT_ID, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_STUDENT_ID, value.trim()).apply()
        }

    fun rememberAfterLogin(studentIdOrEmail: String, enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            savedStudentId = studentIdOrEmail
        } else {
            clearSavedStudentId()
        }
    }

    fun clearSavedStudentId() {
        prefs.edit().remove(KEY_STUDENT_ID).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "onda_student_auto_login"
        const val KEY_ENABLED = "auto_login_enabled"
        const val KEY_STUDENT_ID = "saved_student_id"
    }
}
