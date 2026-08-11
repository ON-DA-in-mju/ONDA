package com.mju.onda.driver.feature.alarm.data

import com.mju.onda.driver.feature.home.data.AssignedOperation
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationStatus
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 로컬 알림을 자동 생성하는 유틸.
 * - 출발 시간이 지났는데 미출발인 배차 → "운행 미시작" 알림
 * - 이전 배차 목록 대비 새로 추가된 배차 → "배차 알림"
 *
 * 중복 방지를 위해 알림 id에 operationId를 포함시킨다.
 */
object AlarmGenerator {

    private val hmFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val generatedOverdueIds = linkedSetOf<String>()

    /**
     * 출발 시간이 지났는데 아직 시작하지 않은 배차에 대해 알림을 생성한다.
     * 이미 생성한 알림(같은 operationId)은 중복 생성하지 않는다.
     */
    fun checkDepartureOverdue(operations: List<AssignedOperation>) {
        val now = LocalTime.now()
        for (op in operations) {
            if (op.departTime.isBlank()) continue
            val depart = parseHm(op.departTime) ?: continue
            val isNotStarted = op.status == OperationStatus.Scheduled ||
                op.status == OperationStatus.Waiting ||
                op.status == OperationStatus.DepartingSoon
            val isActuallyRunning = OperationRuntimeStateHolder.isInProgress(op.id) ||
                OperationRuntimeStateHolder.isEnded(op.id)
            if (!isNotStarted || isActuallyRunning) continue
            if (now.isBefore(depart)) continue

            val alarmId = "overdue-${op.id}"
            if (alarmId in generatedOverdueIds) continue
            if (LocalAlarmStore.getAll().any { it.id == alarmId }) {
                generatedOverdueIds += alarmId
                continue
            }

            generatedOverdueIds += alarmId
            LocalAlarmStore.addAlarm(
                OperationAlarm(
                    id = alarmId,
                    title = "운행 미시작 확인",
                    body = "${op.departTime} 예정 운행(${op.routeName.ifBlank { op.vehicleName }})이 아직 시작되지 않았습니다.",
                    timeLabel = now.format(hmFormatter),
                    category = AlarmCategory.Operation,
                    isUnread = true,
                ),
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
        val prevIds = previous.map { it.id }.toSet()
        val now = LocalTime.now()
        for (op in current) {
            if (op.id in prevIds) continue
            val alarmId = "assign-${op.id}"
            if (LocalAlarmStore.getAll().any { it.id == alarmId }) continue

            val label = buildString {
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

            LocalAlarmStore.addAlarm(
                OperationAlarm(
                    id = alarmId,
                    title = "배정 알림",
                    body = "새로운 운행이 배정되었습니다: $label",
                    timeLabel = now.format(hmFormatter),
                    category = AlarmCategory.AssignmentChange,
                    isUnread = true,
                ),
            )
        }
    }

    fun resetSession() {
        generatedOverdueIds.clear()
    }

    private fun parseHm(raw: String): LocalTime? = try {
        LocalTime.parse(raw.take(5), hmFormatter)
    } catch (_: Exception) {
        null
    }
}
