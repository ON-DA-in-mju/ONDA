package com.mju.onda.driver.feature.settings.data



import android.content.SharedPreferences

import com.mju.onda.driver.core.UserScopedPrefs



object AlarmSettingsStateHolder {

    private const val PREFS = "onda_alarm_settings"

    private const val KEY_ENABLED = "enabled_map" // "id:true,id:false,..."



    private var prefs: SharedPreferences? = null



    @Volatile

    private var items: List<AlarmSettingItem> = MockAlarmSettings.defaults



    fun bindUser() {

        prefs = UserScopedPrefs.get(PREFS)

        items = loadItems()

    }



    fun unbindUser() {

        items = MockAlarmSettings.defaults

        prefs = null

    }



    fun get(): List<AlarmSettingItem> = items



    fun update(newItems: List<AlarmSettingItem>) {

        items = newItems

        persist(newItems)

    }



    fun clear() {

        items = MockAlarmSettings.defaults

        prefs?.edit()?.clear()?.apply()

    }



    private fun loadItems(): List<AlarmSettingItem> {

        val raw = prefs?.getString(KEY_ENABLED, null)

        if (raw.isNullOrBlank()) return MockAlarmSettings.defaults

        val map = raw.split(",")

            .mapNotNull { token ->

                val parts = token.split(":")

                if (parts.size != 2) return@mapNotNull null

                parts[0] to (parts[1] == "true")

            }

            .toMap()

        if (map.isEmpty()) return MockAlarmSettings.defaults

        return MockAlarmSettings.defaults.map { item ->

            item.copy(enabled = map[item.id] ?: item.enabled)

        }

    }



    private fun persist(list: List<AlarmSettingItem>) {

        val encoded = list.joinToString(",") { "${it.id}:${it.enabled}" }

        prefs?.edit()?.putString(KEY_ENABLED, encoded)?.apply()

    }

}

