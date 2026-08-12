package com.mju.onda.driver.feature.history.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import java.time.LocalDate

data class HistoryDetailInfo(
    val id: String,
    val routeName: String,
    val vehicleName: String,
    val status: HistoryResultStatus,
    /** 예: 2026.08.01 (토) */
    val dateDisplay: String,
    val scheduledDepart: String,
    val actualStart: String,
    val actualEnd: String,
    /** 예: 00:42 */
    val totalDuration: String,
    val origin: String,
    val destination: String,
    val locationTxStatus: String,
    val finalStatusLabel: String,
)

object MockHistoryDetail {
    const val SCREEN_TITLE = "운행 이력 상세"

    const val LABEL_DATE = "운행 날짜"
    const val LABEL_STATUS = "운행 상태"
    const val LABEL_SCHEDULED = "예정 출발 시간"
    const val LABEL_ACTUAL_START = "실제 시작 시간"
    const val LABEL_ACTUAL_END = "실제 종료 시간"
    const val LABEL_TOTAL = "총 운행 시간"
    const val LABEL_ORIGIN = "출발지"
    const val LABEL_DEST = "도착지"
    const val LABEL_LOCATION_TX = "위치 전송 상태"
    const val LABEL_FINAL = "최종 처리 상태"

    const val LOCATION_OK = "정상"
    const val LOCATION_STOPPED = "중단"

    const val INFO_BANNER =
        "이 기록은 완료된 운행 이력입니다. 운행 내역은 서버에 안전하게 저장되며, 필요 시 관리자 확인이 가능합니다."

    const val HOME_CD = "오늘의 운행 홈"

    fun forRecordId(recordId: String): HistoryDetailInfo {
        val record = findRecord(recordId) ?: defaultRecord()
        return fromRecord(record)
    }

    fun findRecord(recordId: String): HistoryRecord? =
        HistoryRuntimeStateHolder.runtimeRecords().find { it.id == recordId }
            ?: MockOperationHistory.allSeedRecords.find { it.id == recordId }

    fun fromRecord(record: HistoryRecord): HistoryDetailInfo {
        val meta = tripMeta(record)
        val runtimeId = record.id.removePrefix("runtime-").takeIf { record.id.startsWith("runtime-") }
        val startMillis = runtimeId?.let { OperationRuntimeStateHolder.startedAtMillis(it) }
        val endMillis = runtimeId?.let { OperationRuntimeStateHolder.endedAtMillis(it) }
        val actualEnd = when {
            startMillis != null && endMillis != null -> OperationTripClock.formatHm(endMillis)
            else -> record.timeRange.substringAfter("~", "").trim().ifBlank { record.actualDepart }
        }
        val totalDuration = when {
            startMillis != null && endMillis != null ->
                OperationTripClock.formatDurationHm(startMillis, endMillis)
            else -> durationClock(record.durationLabel)
        }
        return HistoryDetailInfo(
            id = record.id,
            routeName = record.routeName,
            vehicleName = record.vehicleName,
            status = record.status,
            dateDisplay = formatDateDisplay(record.date),
            scheduledDepart = meta.scheduledDepart,
            actualStart = record.actualDepart,
            actualEnd = actualEnd,
            totalDuration = totalDuration,
            origin = meta.origin,
            destination = meta.destination,
            locationTxStatus = when (record.status) {
                HistoryResultStatus.Interrupted -> LOCATION_STOPPED
                else -> LOCATION_OK
            },
            finalStatusLabel = MockOperationHistory.statusLabel(record.status),
        )
    }

    private fun formatDateDisplay(date: LocalDate): String {
        val mm = date.monthValue.toString().padStart(2, '0')
        val dd = date.dayOfMonth.toString().padStart(2, '0')
        return "${date.year}.$mm.$dd (${MockOperationHistory.weekdayLabel(date)})"
    }

    private fun durationClock(durationLabel: String): String {
        val mins = durationLabel.removeSuffix("분").trim().toIntOrNull() ?: 0
        return "00:${mins.toString().padStart(2, '0')}"
    }

    private data class TripMeta(
        val scheduledDepart: String,
        val origin: String,
        val destination: String,
    )

    private fun tripMeta(record: HistoryRecord): TripMeta {
        if (record.id.startsWith("runtime-")) {
            val opId = record.id.removePrefix("runtime-")
            val op = MockTodayOperations.assignedOperations.find { it.id == opId }
            if (op != null) {
                return TripMeta(op.departTime, op.origin, op.destination)
            }
        }
        // DB 기반 상세에서는 origin/destination/scheduledDepart를 우선 placeholder로 표시한다.
        // 목록 화면(완료 운행 표시) 우선 완성 후, 필요 시 추가 컬럼/조인을 통해 고도화할 수 있다.
        return TripMeta("-", "-", "-")
    }

    private fun defaultRecord(): HistoryRecord =
        MockOperationHistory.allSeedRecords.first()
}
