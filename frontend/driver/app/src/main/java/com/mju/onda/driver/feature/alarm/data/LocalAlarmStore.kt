package com.mju.onda.driver.feature.alarm.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs
import org.json.JSONArray
import org.json.JSONObject

/**
 * 로컬에서 생성된 알림을 계정별 SharedPreferences에 저장/로드한다.
 * 알림은 최대 [MAX_ALARMS]건까지 유지하며 초과 시 오래된 것부터 삭제한다.
 */
object LocalAlarmStore {
    private const val PREFS = "onda_local_alarms"
    private const val KEY_JSON = "alarms_json"
    private const val MAX_ALARMS = 50

    @Volatile
    private var cached: MutableList<OperationAlarm>? = null
    private var prefs: SharedPreferences? = null

    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        cached = loadFromPrefs().toMutableList()
    }

    fun unbindUser() {
        cached = null
        prefs = null
    }

    fun getAll(): List<OperationAlarm> = cached ?: emptyList()

    fun addAlarm(alarm: OperationAlarm) {
        val list = cached ?: return
        if (list.any { it.id == alarm.id }) return
        list.add(0, alarm)
        while (list.size > MAX_ALARMS) list.removeAt(list.lastIndex)
        persist(list)
    }

    fun clear() {
        cached?.clear()
        prefs?.edit()?.remove(KEY_JSON)?.apply()
    }

    private fun persist(alarms: List<OperationAlarm>) {
        val arr = JSONArray()
        alarms.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("title", a.title)
                    .put("body", a.body)
                    .put("timeLabel", a.timeLabel)
                    .put("category", a.category.name)
                    .put("isUnread", a.isUnread),
            )
        }
        prefs?.edit()?.putString(KEY_JSON, arr.toString())?.apply()
    }

    private fun loadFromPrefs(): List<OperationAlarm> {
        val raw = prefs?.getString(KEY_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        OperationAlarm(
                            id = o.getString("id"),
                            title = o.optString("title"),
                            body = o.optString("body"),
                            timeLabel = o.optString("timeLabel"),
                            category = runCatching {
                                AlarmCategory.valueOf(o.optString("category", "Operation"))
                            }.getOrDefault(AlarmCategory.Operation),
                            isUnread = o.optBoolean("isUnread", true),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
