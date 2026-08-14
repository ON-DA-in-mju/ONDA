package com.mju.onda.driver.feature.startconfirm.data
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.operation.data.MockOperationDetail
import com.mju.onda.driver.feature.operation.data.OperationDetailInfo
object MockStartConfirm {
    const val SCREEN_TITLE = "배정 운행 상세"
    const val LABEL_ROUND = "운행 회차"
    const val LABEL_DEPART_TIME = "출발 예정시간"
    const val LABEL_ORIGIN = "출발지"
    const val LABEL_DESTINATION = "도착지"
    const val LABEL_DURATION = "예상 소요시간"
    const val CONFIRM_TITLE = "운행을 시작하시겠습니까?"
    const val CONFIRM_BODY_LINE1 = "운행을 시작하면 차량 위치가"
    const val CONFIRM_BODY_LINE2 = "학생용 앱과 관리자 화면에"
    const val CONFIRM_BODY_HIGHLIGHT = "실시간으로 전송"
    const val CONFIRM_BODY_LINE3_SUFFIX = "됩니다."
    const val START_LABEL = "운행 시작"
    const val PROCESSING_PENDING_TOAST = "운행 시작 처리 중 화면은 다음 단계에서 연결합니다."
    const val DIALOG_TITLE = "운행 시작 확인"
    const val DIALOG_MESSAGE = "정말 운행을 시작하시겠습니까?"
    const val DIALOG_CONFIRM = "예"
    const val DIALOG_DISMISS = "아니오"
    fun forOperationId(operationId: String): OperationDetailInfo =
        MockOperationDetail.forOperationId(operationId)
    fun durationLabel(info: OperationDetailInfo): String {
        val minutes = MockTodayOperations.estimatedDurationMinutes(
            departTime = info.departTime,
            expectedEndTime = info.expectedEndTime,
        )
        return "약 ${minutes}분"
    }
    fun roundLabel(round: Int): String = "${round}회차"
    fun statusLabel(): String = MockTodayOperations.WAITING_BADGE
}
