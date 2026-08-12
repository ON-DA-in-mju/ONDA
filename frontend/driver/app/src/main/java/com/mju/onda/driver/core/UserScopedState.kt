package com.mju.onda.driver.core

import com.mju.onda.driver.feature.adminforceend.data.AdminForceEndPoller
import com.mju.onda.driver.feature.alarm.data.AlarmGenerator
import com.mju.onda.driver.feature.alarm.data.AlarmReadStateHolder
import com.mju.onda.driver.feature.alarm.data.DriverNoticesPoller
import com.mju.onda.driver.feature.alarm.data.LocalAlarmStore
import com.mju.onda.driver.feature.backgroundguide.data.BackgroundGuidePrefs
import com.mju.onda.driver.feature.batterywarning.data.BatteryWarningPrefs
import com.mju.onda.driver.feature.consent.data.LocationConsentPrefs
import com.mju.onda.driver.feature.history.data.HistoryRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.home.data.TodayAssignmentsHolder
import com.mju.onda.driver.feature.permission.data.PermissionStateHolder
import com.mju.onda.driver.feature.settings.data.AccountInfoStateHolder
import com.mju.onda.driver.feature.settings.data.AlarmSettingsStateHolder
import com.mju.onda.driver.feature.settings.data.SafeStopDecisionPoller
import com.mju.onda.driver.feature.settings.data.SafeStopHistoryHolder

/**
 * 로그인 계정 전환 시 각 상태 홀더를 해당 계정 저장소로 바인딩한다.
 */
object UserScopedState {
    fun bind(userId: String) {
        UserScopedPrefs.bind(userId)
        OperationRuntimeStateHolder.bindUser()
        HistoryRuntimeStateHolder.bindUser()
        LocalAlarmStore.bindUser()
        AlarmReadStateHolder.bindUser()
        AdminForceEndPoller.start()
        DriverNoticesPoller.start()
        SafeStopHistoryHolder.bindUser()
        BatteryWarningPrefs.bindUser()
        BackgroundGuidePrefs.bindUser()
        PermissionStateHolder.bindUser()
        AccountInfoStateHolder.bindUser(userId)
        AlarmSettingsStateHolder.bindUser()
        LocationConsentPrefs.bindUser()
        TodayAssignmentsHolder.bindUser()
        SafeStopDecisionPoller.start()
    }

    fun unbind() {
        SafeStopDecisionPoller.stop()
        AdminForceEndPoller.stop()
        AdminForceEndPoller.resetSession()
        DriverNoticesPoller.stop()
        OperationRuntimeStateHolder.unbindUser()
        HistoryRuntimeStateHolder.unbindUser()
        AlarmReadStateHolder.unbindUser()
        LocalAlarmStore.unbindUser()
        AlarmGenerator.resetSession()
        SafeStopHistoryHolder.unbindUser()
        BatteryWarningPrefs.unbindUser()
        BackgroundGuidePrefs.unbindUser()
        PermissionStateHolder.unbindUser()
        AccountInfoStateHolder.unbindUser()
        AlarmSettingsStateHolder.unbindUser()
        LocationConsentPrefs.unbindUser()
        TodayAssignmentsHolder.unbindUser()
        UserScopedPrefs.unbind()
    }

    /** 현재 계정의 저장 데이터만 초기화 */
    fun clearCurrentUserData() {
        OperationRuntimeStateHolder.clearAll()
        HistoryRuntimeStateHolder.clearAll()
        AlarmReadStateHolder.clearAll()
        LocalAlarmStore.clear()
        SafeStopHistoryHolder.clearAll()
        BatteryWarningPrefs.clear()
        BackgroundGuidePrefs.clear()
        PermissionStateHolder.reset()
        AccountInfoStateHolder.clear()
        AlarmSettingsStateHolder.clear()
        LocationConsentPrefs.clear()
        TodayAssignmentsHolder.clear()
    }
}
