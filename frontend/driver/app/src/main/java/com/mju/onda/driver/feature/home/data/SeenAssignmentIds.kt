package com.mju.onda.driver.feature.home.data

import com.mju.onda.driver.core.UserScopedPrefs

/** 이미 본 오늘 배차 id. 앱을 다시 켜도 '새 배정' 알림을 반복하지 않는다. */
object SeenAssignmentIds {
    private const val PREFS = "onda_seen_assignments"
    private const val KEY = "ids"

    fun all(): Set<String> {
        val raw = UserScopedPrefs.get(PREFS).getString(KEY, "") ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split('\n').filter { it.isNotBlank() }.toSet()
    }

    fun add(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val next = all() + ids.filter { it.isNotBlank() }
        UserScopedPrefs.get(PREFS).edit().putString(KEY, next.joinToString("\n")).apply()
    }

    fun contains(id: String): Boolean = id in all()
}
