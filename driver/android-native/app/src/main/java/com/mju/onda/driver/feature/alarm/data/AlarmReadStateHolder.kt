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

        val initialized = prefs?.getBoolean(KEY_INITIALIZED, false) == true

        if (initialized) {

            readIds += prefs?.getStringSet(KEY_READ_IDS, emptySet()).orEmpty()

        } else {

            readIds += MockOperationAlarms.seedItems

                .filter { !it.isUnread }

                .map { it.id }

            prefs?.edit()

                ?.putBoolean(KEY_INITIALIZED, true)

                ?.putStringSet(KEY_READ_IDS, readIds.toSet())

                ?.apply()

        }

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

        MockOperationAlarms.seedItems.any { it.id == alarmId }



    fun unreadCount(): Int =

        MockOperationAlarms.seedItems.count { it.id !in readIds }



    fun hasUnread(): Boolean = unreadCount() > 0



    fun clearAll() {

        readIds.clear()

        readIds += MockOperationAlarms.seedItems

            .filter { !it.isUnread }

            .map { it.id }

        prefs?.edit()

            ?.putBoolean(KEY_INITIALIZED, true)

            ?.putStringSet(KEY_READ_IDS, readIds.toSet())

            ?.apply()

    }



    private fun persist() {

        prefs?.edit()?.putStringSet(KEY_READ_IDS, readIds.toSet())?.apply()

    }

}

