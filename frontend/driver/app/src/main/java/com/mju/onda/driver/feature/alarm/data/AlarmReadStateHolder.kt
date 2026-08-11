package com.mju.onda.driver.feature.alarm.data



import android.content.SharedPreferences

import com.mju.onda.driver.core.UserScopedPrefs



/**

 * 알림 읽음 상태. 계정별로 시드 미확인 목록에서 읽은 id를 빼 가며 유지.

 */

object AlarmReadStateHolder {

    private const val PREFS = "onda_alarm_read"

    private const val KEY_READ_IDS = "read_ids"

    private const val KEY_INITIALIZED = "initialized"



    private var prefs: SharedPreferences? = null

    private val readIds = linkedSetOf<String>()



    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        readIds.clear()
        readIds += prefs?.getStringSet(KEY_READ_IDS, emptySet()).orEmpty()
    }



    fun unbindUser() {

        readIds.clear()

        prefs = null

    }



    fun markRead(alarmId: String) {

        if (alarmId.isBlank()) return

        if (!readIds.add(alarmId)) return

        persist()

    }



    fun isUnread(alarmId: String): Boolean = alarmId !in readIds &&
        LocalAlarmStore.getAll().any { it.id == alarmId }

    fun unreadCount(): Int =
        LocalAlarmStore.getAll().count { it.id !in readIds }



    fun hasUnread(): Boolean = unreadCount() > 0



    fun clearAll() {
        readIds.clear()
        prefs?.edit()?.putStringSet(KEY_READ_IDS, emptySet())?.apply()
    }



    private fun persist() {

        prefs?.edit()?.putStringSet(KEY_READ_IDS, readIds.toSet())?.apply()

    }

}

