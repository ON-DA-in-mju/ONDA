package com.mju.onda.driver.feature.settings.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs
import org.json.JSONArray
import org.json.JSONObject

enum class SafeStopReviewStatus {
    /** 관리자 확인 대기 */
    Pending,
    /** 관리자 확인 완료 — 계속 운행 / 중단 승인 선택 대기 */
    Confirmed,
    /** 운행 계속 또는 운행 종료 조치까지 완료 */
    ActionCompleted,
    /** 기사가 요청을 취소함 */
    Cancelled,
}

enum class SafeStopOutcome {
    Approved,
    ContinueOperation,
}

object MockSafeStopHistory {
    const val SCREEN_TITLE = "안전 정차 이력"
    const val EMPTY_TITLE = "아직 안전 정차 이력이 없습니다."
    const val EMPTY_SUBTITLE = "운행 중 안전 정차 요청을 보내면\n이곳에 이력이 표시됩니다."
    const val REFRESH_LABEL = "새로고침"
    const val NEW_REQUEST_LABEL = "중단 요청하기"
    const val STATUS_PENDING = "확인 대기"
    const val STATUS_CONFIRMED = "확인 완료"
    const val STATUS_ACTION_COMPLETED = "조치 완료"
    const val STATUS_CANCELLED = "요청 취소"
    const val REFRESH_TOAST = "관리자 처리 상태를 확인했습니다."

    /** 어제 (기기 날짜) */
    val PAST_DATE_LABEL: String
        get() = com.mju.onda.driver.core.OndaDates.monthDayLabel(
            com.mju.onda.driver.core.OndaDates.today().minusDays(1),
        )

    /** 오늘 (기기 날짜) */
    val TODAY_DATE_LABEL: String
        get() = com.mju.onda.driver.core.OndaDates.monthDayLabel()
}

data class SafeStopHistoryItem(
    val id: String,
    val reason: String,
    val requestedAt: String,
    val routeName: String,
    val vehicleName: String,
    /** 예: "8월 7일" — 기본은 오늘 */
    val dateLabel: String = MockSafeStopHistory.TODAY_DATE_LABEL,
    val reviewStatus: SafeStopReviewStatus = SafeStopReviewStatus.Pending,
    val outcome: SafeStopOutcome? = null,
    val operationId: String = "",
)
/**
 * 안전 정차 요청 이력. SharedPreferences에 보관해 앱 재실행 후에도 유지.
 */
object SafeStopHistoryHolder {
    private const val PREFS = "onda_safe_stop_history"
    private const val KEY_ITEMS = "items"
    private const val KEY_SEEDED = "seeded"

    private var prefs: SharedPreferences? = null
    private val items = mutableListOf<SafeStopHistoryItem>()
    private var selectedId: String? = null

    fun bindUser() {
        prefs = UserScopedPrefs.get(PREFS)
        items.clear()
        selectedId = null
        val raw = prefs?.getString(KEY_ITEMS, null)
        if (!raw.isNullOrBlank()) {
            runCatching { decode(raw) }.getOrNull()?.let { items += it }
        }
        dropFakeSeed()
    }

    fun unbindUser() {
        items.clear()
        selectedId = null
        prefs = null
    }

    fun all(): List<SafeStopHistoryItem> = items.toList()

    fun selected(): SafeStopHistoryItem? =
        selectedId?.let { id -> items.find { it.id == id } }

    fun select(id: String) {
        selectedId = id
    }

    fun clearSelection() {
        selectedId = null
    }

    fun add(item: SafeStopHistoryItem) {
        items.add(0, item)
        persist()
    }

    /** 서버가 발급한 uuid로 로컬 이력 id를 맞춘다 (폴링·취소 연동용) */
    fun remapId(oldId: String, newId: String) {
        if (oldId == newId || newId.isBlank()) return
        val index = items.indexOfFirst { it.id == oldId }
        if (index < 0) return
        items[index] = items[index].copy(id = newId)
        if (selectedId == oldId) selectedId = newId
        persist()
    }

    /** DB 조회 결과를 로컬 이력에 병합 */
    fun mergeRemote(remote: List<SafeStopApi.RemoteRequest>) {
        var changed = 0
        for (r in remote) {
            val index = items.indexOfFirst { it.id == r.id }
            val status = when (r.decision) {
                "continue" -> SafeStopReviewStatus.Confirmed to SafeStopOutcome.ContinueOperation
                "stop" -> SafeStopReviewStatus.Confirmed to SafeStopOutcome.Approved
                "cancelled" -> SafeStopReviewStatus.Cancelled to null
                else -> SafeStopReviewStatus.Pending to null
            }
            if (index < 0) {
                val dateLabel = if (r.date.length >= 10 && r.date[4] == '-') {
                    val p = r.date.split("-")
                    "${p[1].toInt()}월 ${p[2].toInt()}일"
                } else {
                    MockSafeStopHistory.TODAY_DATE_LABEL
                }
                items.add(
                    SafeStopHistoryItem(
                        id = r.id,
                        reason = r.reason.ifBlank { "안전 정차 요청" },
                        requestedAt = r.requestedAt.ifBlank { "--:--" },
                        routeName = r.routeName,
                        vehicleName = r.vehicleName,
                        dateLabel = dateLabel,
                        reviewStatus = status.first,
                        outcome = status.second,
                        operationId = r.operationId,
                    ),
                )
                changed++
            } else {
                val cur = items[index]
                var next = cur
                if (cur.operationId.isBlank() && r.operationId.isNotBlank()) {
                    next = next.copy(operationId = r.operationId)
                }
                if (cur.reviewStatus == SafeStopReviewStatus.Pending &&
                    status.first != SafeStopReviewStatus.Pending
                ) {
                    next = next.copy(
                        reviewStatus = status.first,
                        outcome = status.second,
                        reason = r.reason.ifBlank { cur.reason },
                        routeName = r.routeName.ifBlank { cur.routeName },
                        vehicleName = r.vehicleName.ifBlank { cur.vehicleName },
                    )
                }
                if (next != cur) {
                    items[index] = next
                    changed++
                }
            }
        }
        if (changed > 0) persist()
    }

    /**
     * 관리자/원격 결정 반영.
     * @param remoteById id → "continue" | "stop" | "cancelled"
     */
    fun applyRemoteDecisions(remoteById: Map<String, String>): Int {
        var changed = 0
        for (i in items.indices) {
            val item = items[i]
            if (item.reviewStatus != SafeStopReviewStatus.Pending) continue
            when (remoteById[item.id]) {
                "continue" -> {
                    items[i] = item.copy(
                        reviewStatus = SafeStopReviewStatus.Confirmed,
                        outcome = SafeStopOutcome.ContinueOperation,
                    )
                    changed++
                }
                "stop" -> {
                    items[i] = item.copy(
                        reviewStatus = SafeStopReviewStatus.Confirmed,
                        outcome = SafeStopOutcome.Approved,
                    )
                    changed++
                }
                "cancelled" -> {
                    items[i] = item.copy(
                        reviewStatus = SafeStopReviewStatus.Cancelled,
                        outcome = null,
                    )
                    changed++
                }
            }
        }
        if (changed > 0) persist()
        return changed
    }

    fun cancelById(id: String): Boolean {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return false
        val item = items[index]
        if (item.reviewStatus != SafeStopReviewStatus.Pending) return false
        items[index] = item.copy(
            reviewStatus = SafeStopReviewStatus.Cancelled,
            outcome = null,
        )
        persist()
        return true
    }

    fun markSelectedActionCompleted() {
        val id = selectedId ?: return
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return
        val item = items[index]
        if (item.reviewStatus == SafeStopReviewStatus.ActionCompleted) return
        items[index] = item.copy(reviewStatus = SafeStopReviewStatus.ActionCompleted)
        persist()
    }

    /**
     * 같은 배차의 정차 요청을 모두 조치 완료로 바꾼다.
     * (한 배차에서 요청을 여러 번 보낸 뒤 한 번만 종료해도 이력이 남기지 않도록)
     */
    fun markDispatchActionCompleted(anchor: SafeStopHistoryItem) {
        markDispatchEnded(
            operationId = anchor.operationId,
            routeName = anchor.routeName,
            vehicleName = anchor.vehicleName,
            dateLabel = anchor.dateLabel,
        )
    }

    fun markDispatchEnded(
        operationId: String,
        routeName: String? = null,
        vehicleName: String? = null,
        dateLabel: String? = null,
    ) {
        var changed = false
        for (i in items.indices) {
            val item = items[i]
            if (item.reviewStatus == SafeStopReviewStatus.Cancelled ||
                item.reviewStatus == SafeStopReviewStatus.ActionCompleted
            ) {
                continue
            }
            if (!sameDispatch(item, operationId, routeName, vehicleName, dateLabel)) continue
            items[i] = item.copy(reviewStatus = SafeStopReviewStatus.ActionCompleted)
            changed = true
        }
        if (changed) persist()
    }

    private fun sameDispatch(
        item: SafeStopHistoryItem,
        operationId: String,
        routeName: String?,
        vehicleName: String?,
        dateLabel: String?,
    ): Boolean {
        if (operationId.isNotBlank() && item.operationId.isNotBlank() &&
            item.operationId == operationId
        ) {
            return true
        }
        if (routeName.isNullOrBlank() || vehicleName.isNullOrBlank()) return false
        val sameRoute = item.routeName == routeName && item.vehicleName == vehicleName
        val sameDay = dateLabel.isNullOrBlank() || item.dateLabel == dateLabel
        return sameRoute && sameDay
    }

    fun clearAll() {
        items.clear()
        selectedId = null
        prefs?.edit()
            ?.remove(KEY_ITEMS)
            ?.apply()
    }

    /** 예전 데모용 가짜 이력(차량 고장)은 더 이상 넣지 않는다. */
    private fun dropFakeSeed() {
        val removed = items.removeAll { it.id == "seed-1" }
        if (removed) persist()
        prefs?.edit()?.remove(KEY_SEEDED)?.apply()
    }

    private fun persist() {
        prefs?.edit()?.putString(KEY_ITEMS, encode(items))?.apply()
    }

    private fun encode(list: List<SafeStopHistoryItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("reason", item.reason)
                    put("requestedAt", item.requestedAt)
                    put("routeName", item.routeName)
                    put("vehicleName", item.vehicleName)
                    put("dateLabel", item.dateLabel)
                    put("reviewStatus", item.reviewStatus.name)
                    put("outcome", item.outcome?.name)
                    put("operationId", item.operationId)
                },
            )
        }
        return arr.toString()
    }

    private fun decode(raw: String): List<SafeStopHistoryItem> {
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val status = runCatching {
                    SafeStopReviewStatus.valueOf(obj.getString("reviewStatus"))
                }.getOrDefault(SafeStopReviewStatus.Pending)
                val outcomeName = obj.optString("outcome", "")
                val outcome = outcomeName.takeIf { it.isNotBlank() }?.let {
                    runCatching { SafeStopOutcome.valueOf(it) }.getOrNull()
                }
                add(
                    SafeStopHistoryItem(
                        id = obj.getString("id"),
                        reason = obj.getString("reason"),
                        requestedAt = obj.getString("requestedAt"),
                        routeName = obj.getString("routeName"),
                        vehicleName = obj.getString("vehicleName"),
                        dateLabel = obj.optString("dateLabel", MockSafeStopHistory.TODAY_DATE_LABEL)
                            .ifBlank { MockSafeStopHistory.TODAY_DATE_LABEL },
                        reviewStatus = status,
                        outcome = outcome,
                        operationId = obj.optString("operationId", ""),
                    ),
                )
            }
        }
    }
}
