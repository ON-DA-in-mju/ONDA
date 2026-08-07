package com.mju.onda.driver.feature.settings.data

import android.content.SharedPreferences
import com.mju.onda.driver.core.UserScopedPrefs
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import org.json.JSONArray
import org.json.JSONObject

enum class SafeStopReviewStatus {
    /** 관리자 확인 대기 */
    Pending,
    /** 관리자 확인 완료 — 계속 운행 / 중단 승인 선택 대기 */
    Confirmed,
    /** 운행 계속 또는 운행 종료 조치까지 완료 */
    ActionCompleted,
}

enum class SafeStopOutcome {
    Approved,
    ContinueOperation,
}

object MockSafeStopHistory {
    const val SCREEN_TITLE = "안전 정차 이력"
    const val EMPTY_TITLE = "아직 안전 정차 요청이 없습니다."
    const val EMPTY_SUBTITLE = "운행 중 안전 정차 요청을 보내면\n이곳에 이력이 표시됩니다."
    const val REFRESH_LABEL = "새로고침"
    const val NEW_REQUEST_LABEL = "중단 요청하기"
    const val STATUS_PENDING = "확인 대기"
    const val STATUS_CONFIRMED = "확인 완료"
    const val STATUS_ACTION_COMPLETED = "조치 완료"
    const val REFRESH_TOAST = "관리자 확인 상태를 갱신했습니다."

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
        normalizePastSeed()
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

    fun markAllConfirmed(randomOutcome: () -> SafeStopOutcome) {
        for (i in items.indices) {
            val item = items[i]
            if (item.reviewStatus == SafeStopReviewStatus.Pending) {
                items[i] = item.copy(
                    reviewStatus = SafeStopReviewStatus.Confirmed,
                    outcome = randomOutcome(),
                )
            }
        }
        persist()
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

    fun clearAll() {
        items.clear()
        selectedId = null
        prefs?.edit()
            ?.remove(KEY_ITEMS)
            ?.putBoolean(KEY_SEEDED, false)
            ?.apply()
    }

    /**
     * 비어 있을 때만 어제 날짜 시드(조치 완료 · 계속 운행)를 넣는다.
     * 이미 시드했거나 오늘 이력이 있으면 생략.
     */
    fun ensureSeedIfEmpty() {
        if (items.isNotEmpty()) {
            normalizePastSeed()
            return
        }
        if (prefs?.getBoolean(KEY_SEEDED, false) == true) return
        items += pastSeedItem()
        prefs?.edit()?.putBoolean(KEY_SEEDED, true)?.apply()
        persist()
    }

    /** 구버전 시드(확인 대기 등)를 과거 조치 완료 건으로 맞춤 */
    private fun normalizePastSeed() {
        val index = items.indexOfFirst { it.id == "seed-1" }
        if (index < 0) return
        val desired = pastSeedItem()
        if (items[index] != desired) {
            items[index] = desired
            persist()
        }
        if (prefs?.getBoolean(KEY_SEEDED, false) != true) {
            prefs?.edit()?.putBoolean(KEY_SEEDED, true)?.apply()
        }
    }

    private fun pastSeedItem(): SafeStopHistoryItem {
        val op = MockTodayOperations.assignedOperations.firstOrNull()
        val isDriver02 = SessionStateHolder.currentUserId == "driver02"
        return SafeStopHistoryItem(
            id = "seed-1",
            reason = "차량 고장",
            requestedAt = if (isDriver02) "08:55" else "09:18",
            routeName = op?.routeName ?: "기흥역 통학버스",
            vehicleName = op?.vehicleName ?: "2호차",
            dateLabel = MockSafeStopHistory.PAST_DATE_LABEL,
            reviewStatus = SafeStopReviewStatus.ActionCompleted,
            outcome = SafeStopOutcome.ContinueOperation,
        )
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
                    ),
                )
            }
        }
    }
}
