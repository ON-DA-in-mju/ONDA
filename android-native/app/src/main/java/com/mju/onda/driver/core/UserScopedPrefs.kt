package com.mju.onda.driver.core

import android.content.Context
import android.content.SharedPreferences
import com.mju.onda.driver.data.mock.MockUsers

/**
 * 로그인 계정(userId)별로 SharedPreferences 파일을 분리한다.
 * 예: onda_operation_runtime__driver01
 */
object UserScopedPrefs {
    private var appContext: Context? = null

    @Volatile
    var currentUserId: String? = null
        private set

    private val baseNames = listOf(
        "onda_user_meta",
        "onda_operation_runtime",
        "onda_operation_history_runtime",
        "onda_alarm_read",
        "onda_safe_stop_history",
        "onda_battery_warning",
        "onda_background_guide",
        "onda_permission_mock",
        "onda_account_info",
        "onda_alarm_settings",
        "onda_location_consent",
    )

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun bind(userId: String) {
        currentUserId = userId
    }

    fun unbind() {
        currentUserId = null
    }

    fun get(baseName: String): SharedPreferences {
        val ctx = checkNotNull(appContext) { "UserScopedPrefs.init() required" }
        val id = currentUserId ?: "_guest"
        return ctx.getSharedPreferences(fileName(baseName, id), Context.MODE_PRIVATE)
    }

    fun clearAllForUser(userId: String) {
        val ctx = appContext ?: return
        baseNames.forEach { base ->
            ctx.getSharedPreferences(fileName(base, userId), Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    fun clearAllKnownUsers(extraIds: Collection<String> = emptyList()) {
        val ids = buildSet {
            addAll(MockUsers.drivers.map { it.id })
            addAll(extraIds)
            currentUserId?.let { add(it) }
        }
        ids.forEach { clearAllForUser(it) }
        clearLegacyGlobalPrefs()
    }

    /** 계정 분리 이전 전역 prefs 정리 */
    private fun clearLegacyGlobalPrefs() {
        val ctx = appContext ?: return
        baseNames.forEach { base ->
            ctx.getSharedPreferences(base, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    private fun fileName(baseName: String, userId: String): String = "${baseName}__$userId"
}
