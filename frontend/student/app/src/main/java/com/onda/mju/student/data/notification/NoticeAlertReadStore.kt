package com.onda.mju.student.data.notification

import android.content.Context

/**
 * 공지(notice-*) 기반 알림의 읽음 상태.
 * 개인 notifications 행은 DB is_read 를 쓰고, 공지 알림은 로컬에 보관한다.
 */
class NoticeAlertReadStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun readIds(): Set<String> =
        prefs.getStringSet(KEY_READ, emptySet())?.let { HashSet(it) }.orEmpty()

    fun markRead(id: String) {
        if (!id.startsWith(NOTICE_PREFIX)) return
        val next = HashSet(readIds()).apply { add(id) }
        prefs.edit().putStringSet(KEY_READ, next).apply()
    }

    fun markAllRead(ids: Collection<String>) {
        val noticeIds = ids.filter { it.startsWith(NOTICE_PREFIX) }
        if (noticeIds.isEmpty()) return
        val next = HashSet(readIds()).apply { addAll(noticeIds) }
        prefs.edit().putStringSet(KEY_READ, next).apply()
    }

    fun prune(activeNoticeAlertIds: Set<String>) {
        val kept = HashSet(readIds().filter { it in activeNoticeAlertIds })
        prefs.edit().putStringSet(KEY_READ, kept).apply()
    }

    companion object {
        const val NOTICE_PREFIX = "notice-"
        private const val PREFS = "onda_student_notice_alert_read"
        private const val KEY_READ = "read_ids"

        fun alertIdForNotice(noticeId: String): String = "$NOTICE_PREFIX$noticeId"
    }
}
