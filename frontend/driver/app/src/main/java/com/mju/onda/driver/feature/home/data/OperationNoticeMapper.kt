package com.mju.onda.driver.feature.home.data

import com.mju.onda.driver.feature.assignment.data.AssignmentChangeInfo
import com.mju.onda.driver.feature.cancel.data.OperationCancelInfo
import com.mju.onda.driver.feature.departure.data.DepartureTimeChangeInfo
import com.mju.onda.driver.feature.vehicle.data.VehicleChangeInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 배정 변경/차량 변경/출발시간 변경/운행 취소 화면에
 * 오늘 배차(DB 캐시)를 채운다.
 */
object OperationNoticeMapper {
    private val previousById = linkedMapOf<String, AssignedOperation>()
    private val timeFmt = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)

    fun rememberPrevious(operations: List<AssignedOperation>) {
        previousById.clear()
        operations.forEach { previousById[it.id] = it }
    }

    fun previousOf(operationId: String): AssignedOperation? {
        if (operationId.isBlank()) return null
        return previousById[operationId]
            ?: previousById.values.find { it.matchesId(operationId) }
    }

    fun currentOf(rawId: String?): AssignedOperation? {
        val id = operationIdFrom(rawId)
        if (id.isNotBlank()) {
            MockTodayOperations.findById(id)?.let { return it }
            return previousOf(id)
        }
        return MockTodayOperations.assignedOperations.firstOrNull {
            it.status != OperationStatus.Ended
        } ?: MockTodayOperations.assignedOperations.firstOrNull()
    }

    fun operationIdFrom(raw: String?): String {
        val value = raw.orEmpty().trim()
        if (value.isBlank() || value == "null") return ""
        val prefixes = listOf("assign-", "vehicle-", "depart-", "force-end-", "runtime-")
        for (prefix in prefixes) {
            if (value.startsWith(prefix)) return value.removePrefix(prefix)
        }
        return value
    }

    fun nowLabel(): String = timeFmt.format(Date())

    fun roundLabel(op: AssignedOperation): String = "${op.round}회차"

    fun assignmentInfo(rawId: String?): AssignmentChangeInfo {
        val op = currentOf(rawId) ?: return emptyAssignment()
        val prev = previousOf(op.id)
        return AssignmentChangeInfo(
            routeName = op.routeName.ifBlank { "-" },
            vehicleName = op.vehicleName.ifBlank { "-" },
            roundLabel = roundLabel(op),
            departTime = op.departTime.ifBlank { "-" },
            isDepartTimeChanged = prev != null && prev.departTime != op.departTime,
            origin = op.origin.ifBlank { "-" },
            destination = op.destination.ifBlank { "-" },
            changeReason = "관리자가 배정 정보를 변경했습니다.",
            changeTime = nowLabel(),
        )
    }

    fun vehicleInfo(rawId: String?): VehicleChangeInfo {
        val op = currentOf(rawId) ?: return emptyVehicle()
        val prev = previousOf(op.id)
        return VehicleChangeInfo(
            beforeVehicle = prev?.vehicleName?.ifBlank { "-" } ?: "-",
            afterVehicle = op.vehicleName.ifBlank { "-" },
            routeName = op.routeName.ifBlank { "-" },
            roundLabel = roundLabel(op),
            scheduledTime = op.departTime.ifBlank { "-" },
            origin = op.origin.ifBlank { "-" },
            destination = op.destination.ifBlank { "-" },
            changeReason = "관리자가 배정 차량을 변경했습니다.",
            changeTime = nowLabel(),
        )
    }

    fun departureInfo(rawId: String?): DepartureTimeChangeInfo {
        val op = currentOf(rawId) ?: return emptyDeparture()
        val prev = previousOf(op.id)
        return DepartureTimeChangeInfo(
            beforeTime = prev?.departTime?.ifBlank { "-" } ?: "-",
            afterTime = op.departTime.ifBlank { "-" },
            routeName = op.routeName.ifBlank { "-" },
            vehicleName = op.vehicleName.ifBlank { "-" },
            roundLabel = roundLabel(op),
            origin = op.origin.ifBlank { "-" },
            destination = op.destination.ifBlank { "-" },
            changeReason = "관리자가 출발 시간을 변경했습니다.",
            changeTime = nowLabel(),
        )
    }

    fun cancelInfo(rawId: String?): OperationCancelInfo {
        val op = currentOf(rawId) ?: return emptyCancel()
        return OperationCancelInfo(
            routeName = op.routeName.ifBlank { "-" },
            vehicleName = op.vehicleName.ifBlank { "-" },
            roundLabel = roundLabel(op),
            departTime = op.departTime.ifBlank { "-" },
            origin = op.origin.ifBlank { "-" },
            destination = op.destination.ifBlank { "-" },
            cancelReason = "관리자에 의해 운행이 취소되었습니다.",
            cancelTime = nowLabel(),
        )
    }

    private fun emptyAssignment() = AssignmentChangeInfo(
        routeName = "-",
        vehicleName = "-",
        roundLabel = "-",
        departTime = "-",
        isDepartTimeChanged = false,
        origin = "-",
        destination = "-",
        changeReason = "-",
        changeTime = "-",
    )

    private fun emptyVehicle() = VehicleChangeInfo(
        beforeVehicle = "-",
        afterVehicle = "-",
        routeName = "-",
        roundLabel = "-",
        scheduledTime = "-",
        origin = "-",
        destination = "-",
        changeReason = "-",
        changeTime = "-",
    )

    private fun emptyDeparture() = DepartureTimeChangeInfo(
        beforeTime = "-",
        afterTime = "-",
        routeName = "-",
        vehicleName = "-",
        roundLabel = "-",
        origin = "-",
        destination = "-",
        changeReason = "-",
        changeTime = "-",
    )

    private fun emptyCancel() = OperationCancelInfo(
        routeName = "-",
        vehicleName = "-",
        roundLabel = "-",
        departTime = "-",
        origin = "-",
        destination = "-",
        cancelReason = "-",
        cancelTime = "-",
    )
}
