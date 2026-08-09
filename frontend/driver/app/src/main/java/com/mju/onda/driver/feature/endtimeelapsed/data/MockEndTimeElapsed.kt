package com.mju.onda.driver.feature.endtimeelapsed.data

import com.mju.onda.driver.core.OperationTripClock
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import java.util.Calendar

data class EndTimeElapsedInfo(
    val routeName: String,
    val vehicleName: String,
    val scheduledEnd: String,
    val currentTime: String,
    val overtimeLabel: String,
    val lastTransmission: String,
)

object MockEndTimeElapsed {
    const val SCREEN_TITLE = "운행 종료"
    const val HEADLINE = "예정 종료 시간이 지났습니다"
    const val SUBHEAD = "아직 운행 중이신가요?"
    const val BODY =
        "현재도 운행 중이라면 계속 운행을 선택하고,\n운행이 끝났다면 종료해 주세요."

    const val LABEL_ROUTE = "노선"
    const val LABEL_VEHICLE = "차량"
    const val LABEL_SCHEDULED_END = "예정 종료 시간"
    const val LABEL_CURRENT = "현재 시간"
    const val LABEL_OVERTIME = "경과 시간"
    const val LABEL_LAST_TX = "마지막 전송"

    const val CONTINUE_LABEL = "계속 운행 중"
    const val END_LABEL = "운행 종료하기"
    const val FOOTER_INFO = "장시간 종료되지 않을 경우\n관리자 확인 후 강제 종료될 수 있습니다."

    fun forOperationId(
        operationId: String,
        lastTransmission: String = "방금 전",
    ): EndTimeElapsedInfo {
        val op = MockTodayOperations.assignedOperations.find { it.id == operationId }
            ?: MockTodayOperations.assignedOperations.first()
        val now = System.currentTimeMillis()
        val currentHm = OperationTripClock.formatHm(now)
        val overtimeMin = overtimeMinutes(op.expectedEndTime, now)
        return EndTimeElapsedInfo(
            routeName = op.routeName,
            vehicleName = op.vehicleName,
            scheduledEnd = op.expectedEndTime,
            currentTime = currentHm,
            overtimeLabel = if (overtimeMin > 0) "+ ${overtimeMin}분" else "+ 0분",
            lastTransmission = lastTransmission,
        )
    }

    /** 예정 종료(HH:mm) 대비 현재 시각의 초과 분 */
    private fun overtimeMinutes(scheduledEndHm: String, nowMillis: Long): Int {
        val parts = scheduledEndHm.split(":")
        if (parts.size < 2) return 0
        val hour = parts[0].toIntOrNull() ?: return 0
        val minute = parts[1].toIntOrNull() ?: return 0
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = nowMillis - cal.timeInMillis
        if (diff <= 0) return 0
        return (diff / 60_000L).toInt()
    }
}
