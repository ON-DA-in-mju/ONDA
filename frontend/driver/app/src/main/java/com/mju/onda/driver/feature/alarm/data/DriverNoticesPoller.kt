package com.mju.onda.driver.feature.alarm.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 관리자 공지(기사 대상)를 주기적으로 가져와 종 알림 목록([LocalAlarmStore])에 넣는다.
 * 시작일 이전(예약) 공지는 API/클라이언트 필터로 제외한다.
 */
object DriverNoticesPoller {
    private const val TAG = "DriverNoticesPoller"
    private const val INTERVAL_MS = 12_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                pollOnce()
                delay(INTERVAL_MS)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    /** 네트워크는 항상 IO 스레드에서 수행 (메인 호출 시 NetworkOnMainThreadException 방지) */
    suspend fun pollOnce() = withContext(Dispatchers.IO) {
        runCatching {
            val notices = DriverNoticesApi.fetchPublishedForDriver()
            val activeIds = notices.map { "notice-${it.id}" }.toSet()
            // 예약 전이거나 종료된 공지는 로컬 목록에서도 제거
            LocalAlarmStore.getAll()
                .filter { it.id.startsWith("notice-") && it.id !in activeIds }
                .forEach { LocalAlarmStore.removeById(it.id) }
            if (notices.isEmpty()) return@runCatching
            var added = 0
            for (n in notices) {
                val alarmId = "notice-${n.id}"
                val existed = LocalAlarmStore.getAll().any { it.id == alarmId }
                val typeLabel = DriverNoticesApi.typeLabel(n.type)
                val detailContent = n.content
                    .trim()
                    .replace(Regex("[ \\t]+"), " ")
                    .replace(Regex("\\n{3,}"), "\n\n")
                val listContent = detailContent.replace(Regex("\\s+"), " ")
                val listBody = when {
                    listContent.isBlank() -> n.title
                    else -> "${n.title} — $listContent"
                }
                LocalAlarmStore.upsertAlarm(
                    OperationAlarm(
                        id = alarmId,
                        title = typeLabel,
                        body = listBody,
                        timeLabel = DriverNoticesApi.timeLabel(n.startsAt ?: n.createdAt),
                        category = AlarmCategory.Notice,
                        isUnread = true,
                        noticeHeadline = n.title,
                        noticeType = n.type,
                        noticeDateTime = DriverNoticesApi.displayDateTime(n),
                        noticeContent = detailContent.ifBlank { n.title },
                    ),
                )
                if (!existed) added += 1
            }
            if (added > 0) {
                Log.d(TAG, "synced $added notice alarm(s)")
            }
        }.onFailure {
            Log.w(TAG, "poll failed: ${it.message}")
        }
    }
}
