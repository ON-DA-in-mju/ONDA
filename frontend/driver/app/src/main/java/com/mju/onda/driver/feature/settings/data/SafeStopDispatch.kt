package com.mju.onda.driver.feature.settings.data

import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder

/** 안전 정차 이력과 실제 배차를 연결한다. 다른 회차 운행을 잘못 종료하지 않기 위함. */
object SafeStopDispatch {
    fun resolvedOperationId(item: SafeStopHistoryItem?): String {
        if (item == null) return ""
        if (item.operationId.isNotBlank()) return item.operationId
        val matches = MockTodayOperations.assignedOperations.filter {
            it.routeName == item.routeName && it.vehicleName == item.vehicleName
        }
        // 같은 노선·차량이 여러 회차면 추측하지 않는다.
        return if (matches.size == 1) matches.single().id else ""
    }

    fun isLive(item: SafeStopHistoryItem?): Boolean {
        val id = resolvedOperationId(item)
        return OperationRuntimeStateHolder.isLiveOperation(id)
    }
}
