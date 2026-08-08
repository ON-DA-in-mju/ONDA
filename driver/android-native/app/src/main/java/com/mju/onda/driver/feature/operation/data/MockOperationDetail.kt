package com.mju.onda.driver.feature.operation.data

import com.mju.onda.driver.feature.home.data.AssignedOperation
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationStatus

data class OperationDetailInfo(
    val id: String,
    val routeName: String,
    val vehicleName: String,
    val round: Int,
    val departTime: String,
    val expectedEndTime: String,
    val origin: String,
    val destination: String,
    val dateLabel: String,
    val status: OperationStatus,
    val statusLabel: String,
)

object MockOperationDetail {
    const val SCREEN_TITLE = "배정 운행 상세"
    const val ASSIGNED_BADGE = "배정 운행"
    const val ADMIN_NOTE = "관리자가 배정한 운행 정보입니다."
    const val PREPARE_LABEL = "운행 준비하기"
    const val CONTACT_ADMIN_LABEL = "관리자 문의"
    const val CONTACT_ADMIN_TOAST = "관리자에게 문의해 주세요."
    const val PREPARE_PENDING_TOAST = "운행 준비 화면은 다음 단계에서 연결합니다."

    // DRI-01-02B 배정 정보 확인
    const val CONFIRM_TITLE = "배정 정보 확인"
    const val CONFIRM_SUBTITLE = "배정된 운행 정보를 확인해 주세요."
    const val CONFIRM_FOOTER = "정보가 다르면 관리자에게 문의해 주세요"
    const val CONFIRM_DIFF_LABEL = "정보가 다릅니다"
    const val CONFIRM_OK_LABEL = "확인했습니다"

    const val LABEL_ORIGIN_STOP = "출발 정류장"
    const val LABEL_DEST_STOP = "도착 정류장"
    const val LABEL_VEHICLE = "배정 차량"
    const val LABEL_STATUS = "운행 상태"

    /** DRI-01-02A — 09:05 기흥역 통학버스 */
    val giheungDetail = OperationDetailInfo(
        id = "op-0905",
        routeName = "기흥역 통학버스",
        vehicleName = "2호차",
        round = 1,
        departTime = "09:05",
        expectedEndTime = "09:25",
        origin = "채플관 앞",
        destination = "기흥역 5번 출구",
        dateLabel = MockTodayOperations.DATE_LABEL,
        status = OperationStatus.Waiting,
        statusLabel = MockTodayOperations.WAITING_BADGE,
    )

    fun forOperationId(operationId: String): OperationDetailInfo {
        val op = MockTodayOperations.assignedOperations.find { it.id == operationId }
        if (op != null) return fromAssigned(op)
        if (operationId == giheungDetail.id) {
            return fromAssigned(
                AssignedOperation(
                    id = giheungDetail.id,
                    routeName = giheungDetail.routeName,
                    vehicleName = giheungDetail.vehicleName,
                    departTime = giheungDetail.departTime,
                    origin = giheungDetail.origin,
                    destination = giheungDetail.destination,
                    round = giheungDetail.round,
                    expectedEndTime = giheungDetail.expectedEndTime,
                    status = OperationStatus.Scheduled,
                ),
            )
        }
        return giheungDetail
    }

    private fun fromAssigned(op: AssignedOperation): OperationDetailInfo {
        val resolved = OperationRuntimeStateHolder.withRuntimeStatus(listOf(op)).first()
        val status = resolved.status
        return OperationDetailInfo(
            id = op.id,
            routeName = op.routeName,
            vehicleName = op.vehicleName,
            round = op.round,
            departTime = op.departTime,
            expectedEndTime = op.expectedEndTime,
            origin = op.origin,
            destination = op.destination,
            dateLabel = MockTodayOperations.DATE_LABEL,
            status = status,
            statusLabel = MockTodayOperations.statusLabel(status),
        )
    }
}
