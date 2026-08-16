package com.mju.onda.driver.core.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val FIND_ID = "find_id"
    const val FIND_PASSWORD = "find_password"
    const val LOCATION_CONSENT = "location_consent"
    const val PERMISSION_GUIDE = "permission_guide"
    const val PERMISSION_COMPLETE = "permission_complete"
    const val TODAY_OPERATION = "today_operation"
    const val OPERATION_ALARMS = "operation_alarms"
    const val ASSIGNMENT_CHANGE = "assignment_change?operationId={operationId}"
    const val VEHICLE_CHANGE = "vehicle_change?operationId={operationId}"
    const val DEPARTURE_TIME_CHANGE = "departure_time_change?operationId={operationId}"
    const val OPERATION_CANCEL = "operation_cancel?operationId={operationId}"
    const val OPERATION_DETAIL = "operation_detail/{operationId}"
    const val PRE_OPERATION_CHECK = "pre_operation_check"
    const val PRE_CHECK_COMPLETE = "pre_check_complete"
    const val START_CONFIRM = "start_confirm"
    const val START_PROCESSING = "start_processing"
    const val START_COMPLETE = "start_complete"
    // 운행 중 최소 화면은 제거됨 — 상세 상태로 통합
    const val IN_OPERATION_DETAIL_STATUS = "in_operation_detail_status/{operationId}"
    const val STOP_ROUTE_PROGRESS = "stop_route_progress/{operationId}"
    const val OPERATION_RECOVERY = "operation_recovery"
    const val BACKGROUND_GUIDE = "background_guide"
    const val BATTERY_WARNING = "battery_warning"
    const val END_OPERATION_CONFIRM = "end_operation_confirm/{operationId}"
    const val END_OPERATION_PROCESSING = "end_operation_processing/{operationId}"
    const val END_OPERATION_COMPLETE = "end_operation_complete/{operationId}"
    const val END_TIME_ELAPSED = "end_time_elapsed/{operationId}"
    const val ADMIN_FORCE_END = "admin_force_end/{operationId}"
    const val OPERATION_HISTORY = "operation_history"
    const val OPERATION_HISTORY_DETAIL = "operation_history_detail/{recordId}"
    const val DRIVER_SETTINGS = "driver_settings"
    const val LOGOUT_CONFIRM = "logout_confirm"
    const val LOGOUT_RESTRICTED = "logout_restricted"
    const val ACCOUNT_INFO = "account_info"
    const val DEVICE_PERMISSION = "device_permission"
    const val ALARM_SETTINGS = "alarm_settings"
    const val LOCATION_CONSENT_MANAGE = "location_consent_manage"
    const val CONTACT_ADMIN = "contact_admin"
    const val SAFE_STOP_CONFIRM = "safe_stop_confirm"
    const val SAFE_STOP_HISTORY = "safe_stop_history"
    const val STOP_REASON_SELECT = "stop_reason_select"
    const val STOP_REQUEST_DETAIL = "stop_request_detail/{reason}"
    const val STOP_REQUEST_CONFIRM = "stop_request_confirm"
    const val STOP_REQUEST_RECEIVED = "stop_request_received"
    const val STOP_APPROVED = "stop_approved"
    const val CONTINUE_OPERATION = "continue_operation"
    const val INTERRUPTED_END_PROCESSING = "interrupted_end_processing/{operationId}"
    const val INTERRUPTED_END_COMPLETE = "interrupted_end_complete/{operationId}"

    fun assignmentChange(operationId: String): String =
        "assignment_change?operationId=${Uri.encode(operationId)}"
    fun vehicleChange(operationId: String): String =
        "vehicle_change?operationId=${Uri.encode(operationId)}"
    fun departureTimeChange(operationId: String): String =
        "departure_time_change?operationId=${Uri.encode(operationId)}"
    fun operationCancel(operationId: String): String =
        "operation_cancel?operationId=${Uri.encode(operationId)}"
    fun operationDetail(operationId: String): String =
        "operation_detail/${Uri.encode(operationId)}"
    fun inOperationDetailStatus(operationId: String): String =
        "in_operation_detail_status/${Uri.encode(operationId)}"
    fun stopRouteProgress(operationId: String): String = "stop_route_progress/${Uri.encode(operationId)}"
    fun endOperationConfirm(operationId: String): String = "end_operation_confirm/${Uri.encode(operationId)}"
    fun endOperationProcessing(operationId: String): String =
        "end_operation_processing/${Uri.encode(operationId)}"
    fun endOperationComplete(operationId: String): String = "end_operation_complete/${Uri.encode(operationId)}"
    fun endTimeElapsed(operationId: String): String = "end_time_elapsed/${Uri.encode(operationId)}"
    fun adminForceEnd(operationId: String): String = "admin_force_end/${Uri.encode(operationId)}"
    fun interruptedEndProcessing(operationId: String): String =
        "interrupted_end_processing/${Uri.encode(operationId)}"
    fun interruptedEndComplete(operationId: String): String =
        "interrupted_end_complete/${Uri.encode(operationId)}"
    fun operationHistoryDetail(recordId: String): String = "operation_history_detail/$recordId"
    fun stopRequestDetail(reason: String): String =
        "stop_request_detail/${Uri.encode(reason)}"
}
