package com.mju.onda.driver.feature.alarm.data

import com.mju.onda.driver.core.KoreaTime
import com.mju.onda.driver.feature.home.data.AssignedOperation
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationNoticeMapper
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationStatus
import com.mju.onda.driver.feature.home.data.SeenAssignmentIds
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 로컬 알림을 자동 생성하는 유틸.
 * - 출발 N분 전 → 임박 배너형 알림 (터치 불가)
 * - 출발 시간이 지났는데 미출발 → 미시작 배너형 알림 (터치 불가)
 * - 이전 배차 목록 대비 새로 추가된 배차 → "배차 알림"
 *
 * 중복 방지를 위해 알림 id에 operationId를 포함시킨다.
 */
object AlarmGenerator {

    const val IMMINENT_MINUTES = 5

    private val hmFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val generatedOverdueIds = linkedSetOf<String>()
    private val generatedImminentIds = linkedSetOf<String>()

    fun isBannerAlarmId(id: String): Boolean =
        id.startsWith("imminent-") || id.startsWith("overdue-")

    fun isBannerAlarm(alarm: OperationAlarm): Boolean = isBannerAlarmId(alarm.id)

    enum class DepartureAlertKind {
        None,
        Imminent,
        Overdue,
    }

    /**
     * 홈 상단 배너(DRI-01-02E/F) 상태.
     * 미시작 배차 중 예정시각이 지났으면(동일 시각 포함) Overdue,
     * 아니면 5분 이내 임박이면 Imminent.
     */
    fun resolveDepartureAlertKind(operations: List<AssignedOperation>): DepartureAlertKind {
        val now = KoreaTime.nowTime()
        val candidates = operations
            .filter { isNotStartedCandidate(it) }
            .mapNotNull { op -> parseHm(op.departTime) }
            .sorted()
        if (candidates.isEmpty()) return DepartureAlertKind.None
        if (candidates.any { !now.isBefore(it) }) return DepartureAlertKind.Overdue
        val next = candidates.first()
        val minutesUntil = Duration.between(now, next).toMinutes()
        return if (minutesUntil in 0..IMMINENT_MINUTES) {
            DepartureAlertKind.Imminent
        } else {
            DepartureAlertKind.None
        }
    }

    /** 출발 5분 전(임박) 배너와 동일한 알림을 목록에 남긴다. */
    fun checkDepartureImminent(operations: List<AssignedOperation>) {
        val now = KoreaTime.nowTime()
        for (op in operations) {
            if (!isNotStartedCandidate(op)) continue
            val depart = parseHm(op.departTime) ?: continue
            if (!now.isBefore(depart)) continue
            val minutesUntil = Duration.between(now, depart).toMinutes()
            if (minutesUntil !in 0..IMMINENT_MINUTES) continue

            val alarmId = "imminent-${op.id}"
            upsertBannerAlarm(
                alarmId = alarmId,
                generatedIds = generatedImminentIds,
                title = MockTodayOperations.ALERT_IMMINENT_TITLE,
                body = MockTodayOperations.ALERT_IMMINENT_BODY.replace('\n', ' '),
                timeLabel = now.format(hmFormatter),
            )
        }
    }

    /**
     * 출발 시간이 지났는데 아직 시작하지 않은 배차에 대해
     * 홈 미시작 배너와 동일한 알림을 생성한다.
     */
    fun checkDepartureOverdue(operations: List<AssignedOperation>) {
        val now = KoreaTime.nowTime()
        for (op in operations) {
            if (!isNotStartedCandidate(op)) continue
            val depart = parseHm(op.departTime) ?: continue
            if (now.isBefore(depart)) continue

            val alarmId = "overdue-${op.id}"
            upsertBannerAlarm(
                alarmId = alarmId,
                generatedIds = generatedOverdueIds,
                title = MockTodayOperations.ALERT_OVERDUE_TITLE,
                body = MockTodayOperations.ALERT_OVERDUE_BODY,
                timeLabel = now.format(hmFormatter),
            )
        }
    }

    /**
     * 이전 배차 목록과 비교해 새로 추가된 배차에 대해 알림을 생성한다.
     */
    fun checkNewAssignments(
        previous: List<AssignedOperation>,
        current: List<AssignedOperation>,
    ) {
        OperationNoticeMapper.rememberPrevious(previous)
        val prevById = previous.associateBy { it.id }
        val seen = SeenAssignmentIds.all()
        val bootstrapping = seen.isEmpty()
        val timeLabel = KoreaTime.nowHm()
        val newlySeen = mutableListOf<String>()

        for (op in current) {
            val prev = prevById[op.id]
            if (!seen.contains(op.id) && prev == null) {
                if (!bootstrapping) {
                    addOnce(
                        alarmId = "assign-${op.id}",
                        title = "배정 알림",
                        body = "새로운 운행이 배정되었습니다: ${assignmentLabel(op)}",
                        timeLabel = timeLabel,
                        category = AlarmCategory.AssignmentChange,
                    )
                }
                newlySeen += op.id
                continue
            }
            newlySeen += op.id
            if (prev == null) continue
            if (prev.vehicleName != op.vehicleName &&
                (prev.vehicleName.isNotBlank() || op.vehicleName.isNotBlank())
            ) {
                addOnce(
                    alarmId = "vehicle-${op.id}",
                    title = "차량 변경",
                    body = "${assignmentLabel(op)} 차량이 변경되었습니다.",
                    timeLabel = timeLabel,
                    category = AlarmCategory.AssignmentChange,
                )
            }
            if (prev.departTime != op.departTime &&
                (prev.departTime.isNotBlank() || op.departTime.isNotBlank())
            ) {
                addOnce(
                    alarmId = "depart-${op.id}",
                    title = "출발 시간 변경",
                    body = "${assignmentLabel(op)} 출발 시간이 ${op.departTime}으로 변경되었습니다.",
                    timeLabel = timeLabel,
                    category = AlarmCategory.DepartureTimeChange,
                )
            }
            if (prev.routeName != op.routeName ||
                prev.origin != op.origin ||
                prev.destination != op.destination ||
                prev.round != op.round
            ) {
                addOnce(
                    alarmId = "assign-${op.id}",
                    title = "배정 변경",
                    body = "운행 정보가 변경되었습니다: ${assignmentLabel(op)}",
                    timeLabel = timeLabel,
                    category = AlarmCategory.AssignmentChange,
                )
            }
        }
        SeenAssignmentIds.add(newlySeen)
    }

    private fun assignmentLabel(op: AssignedOperation): String =
        buildString {
            if (op.routeName.isNotBlank()) append(op.routeName)
            if (op.vehicleName.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(op.vehicleName)
            }
            if (op.departTime.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(op.departTime)
            }
        }.ifBlank { "새 운행" }

    private fun addOnce(
        alarmId: String,
        title: String,
        body: String,
        timeLabel: String,
        category: AlarmCategory,
    ) {
        if (LocalAlarmStore.getAll().any { it.id == alarmId }) return
        LocalAlarmStore.addAlarm(
            OperationAlarm(
                id = alarmId,
                title = title,
                body = body,
                timeLabel = timeLabel,
                category = category,
                isUnread = true,
            ),
        )
    }

    fun resetSession() {
        generatedOverdueIds.clear()
        generatedImminentIds.clear()
    }

    private fun isNotStartedCandidate(op: AssignedOperation): Boolean {
        val isNotStarted = op.status == OperationStatus.Scheduled ||
            op.status == OperationStatus.Waiting ||
            op.status == OperationStatus.DepartingSoon
        val isActuallyRunning = OperationRuntimeStateHolder.isInProgress(op.id) ||
            OperationRuntimeStateHolder.isEnded(op.id)
        return isNotStarted && !isActuallyRunning && op.departTime.isNotBlank()
    }

    private fun upsertBannerAlarm(
        alarmId: String,
        generatedIds: MutableSet<String>,
        title: String,
        body: String,
        timeLabel: String,
    ) {
        val alarm = OperationAlarm(
            id = alarmId,
            title = title,
            body = body,
            timeLabel = timeLabel,
            category = AlarmCategory.Operation,
            isUnread = true,
        )
        val existing = LocalAlarmStore.getAll().find { it.id == alarmId }
        if (existing != null) {
            generatedIds += alarmId
            // 예전 카피(운행 미시작 확인 등)를 배너 문구로 갱신
            if (existing.title != title || existing.body != body) {
                LocalAlarmStore.upsertAlarm(alarm)
            }
            return
        }
        if (alarmId in generatedIds) return
        generatedIds += alarmId
        LocalAlarmStore.addAlarm(alarm)
    }

    private fun parseHm(raw: String): LocalTime? = try {
        LocalTime.parse(raw.take(5), hmFormatter)
    } catch (_: Exception) {
        null
    }
}
