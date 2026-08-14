package com.onda.mju.student.data.community

import android.content.Context

/** 커뮤니티 제보/글 읽음 상태 (앱 재시작 후에도 유지) */
class CommunityReadStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun readIds(): Set<String> =
        prefs.getStringSet(KEY_READ, emptySet())?.let { HashSet(it) }.orEmpty()

    fun markRead(id: String) {
        if (id.isBlank()) return
        val next = HashSet(readIds()).apply { add(id) }
        prefs.edit().putStringSet(KEY_READ, next).apply()
    }

    fun markReadAll(ids: Collection<String>) {
        val cleaned = ids.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return
        val next = HashSet(readIds()).apply { addAll(cleaned) }
        prefs.edit().putStringSet(KEY_READ, next).apply()
    }

    companion object {
        private const val PREFS = "onda_student_community_read"
        private const val KEY_READ = "read_report_ids"
    }
}
