package com.mju.onda.driver.feature.home.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs
import org.json.JSONArray
import org.json.JSONObject

/**
 * 로컬 API에서 받은 오늘 배정(또는 fallback mock)을 현재 계정에 보관한다.
 * MockTodayOperations.assignedOperations가 여기를 우선 참조한다.
 */
object TodayAssignmentsHolder {
    private const val PREFS = "onda_today_assignments"
    private const val KEY_JSON = "assignments_json"

    @Volatile
    private var cached: List<AssignedOperation>? = null

    private var prefs: SharedPreferences? = null

    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        cached = readPrefs()
    }

    fun unbindUser() {
        cached = null
        prefs = null
    }

    fun clear() {
        cached = null
        prefs?.edit()?.clear()?.apply()
    }

    fun set(operations: List<AssignedOperation>) {
        cached = operations
        persist(operations)
    }

    fun getOrNull(): List<AssignedOperation>? = cached

    private fun persist(operations: List<AssignedOperation>) {
        val arr = JSONArray()
        operations.forEach { op ->
            arr.put(
                JSONObject()
                    .put("id", op.id)
                    .put("routeName", op.routeName)
                    .put("vehicleName", op.vehicleName)
                    .put("departTime", op.departTime)
                    .put("origin", op.origin)
                    .put("destination", op.destination)
                    .put("round", op.round)
                    .put("expectedEndTime", op.expectedEndTime)
                    .put("status", op.status.name),
            )
        }
        prefs?.edit()?.putString(KEY_JSON, arr.toString())?.apply()
    }

    private fun readPrefs(): List<AssignedOperation>? {
        val raw = prefs?.getString(KEY_JSON, null) ?: return null
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        AssignedOperation(
                            id = o.getString("id"),
                            routeName = o.optString("routeName"),
                            vehicleName = o.optString("vehicleName"),
                            departTime = o.optString("departTime"),
                            origin = o.optString("origin"),
                            destination = o.optString("destination"),
                            round = o.optInt("round", 1),
                            expectedEndTime = o.optString("expectedEndTime"),
                            status = runCatching {
                                OperationStatus.valueOf(o.optString("status", "Scheduled"))
                            }.getOrDefault(OperationStatus.Scheduled),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
