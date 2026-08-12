package com.mju.onda.driver.feature.settings.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs
import com.mju.onda.driver.core.supabase.SupabaseClient

object AccountInfoStateHolder {
    private const val PREFS = "onda_account_info"
    private const val KEY_DRIVER_NAME = "driver_name"
    private const val KEY_DRIVER_ID = "driver_id"
    private const val KEY_ORG = "organization"
    private const val KEY_CONTACT = "contact_status"

    private var prefs: SharedPreferences? = null

    @Volatile
    private var current: AccountInfo = MockAccountInfo.info

    fun bindUser(userId: String) {
        prefs = UserScopedPrefs.get(PREFS)
        val savedId = prefs?.getString(KEY_DRIVER_ID, null)
        current = if (!savedId.isNullOrBlank()) {
            AccountInfo(
                driverName = prefs?.getString(KEY_DRIVER_NAME, null)
                    ?: nameFromSupabase(),
                driverId = savedId,
                organization = prefs?.getString(KEY_ORG, null)
                    ?: MockAccountInfo.DEFAULT_ORG,
                contactStatus = prefs?.getString(KEY_CONTACT, null)
                    ?: MockAccountInfo.info.contactStatus,
            )
        } else {
            seedFromLogin(userId).also { persist(it) }
        }
    }

    fun unbindUser() {
        current = MockAccountInfo.info
        prefs = null
    }

    fun get(): AccountInfo = current

    fun update(info: AccountInfo) {
        current = info
        persist(info)
    }

    fun toProfile(): DriverProfile = DriverProfile(
        name = current.driverName,
        organization = current.organization,
    )

    fun clear() {
        current = MockAccountInfo.info
        prefs?.edit()?.clear()?.apply()
    }

    private fun nameFromSupabase(): String {
        val name = SupabaseClient.displayName?.takeIf { it.isNotBlank() }
        return MockAccountInfo.formatDisplayName(name ?: "기사")
    }

    private fun seedFromLogin(userId: String): AccountInfo {
        return AccountInfo(
            driverName = nameFromSupabase(),
            driverId = SupabaseClient.loginId ?: userId,
            organization = MockAccountInfo.DEFAULT_ORG,
            contactStatus = MockAccountInfo.info.contactStatus,
        )
    }

    private fun persist(info: AccountInfo) {
        prefs?.edit()
            ?.putString(KEY_DRIVER_NAME, info.driverName)
            ?.putString(KEY_DRIVER_ID, info.driverId)
            ?.putString(KEY_ORG, info.organization)
            ?.putString(KEY_CONTACT, info.contactStatus)
            ?.apply()
    }
}
