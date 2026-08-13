package com.mju.onda.driver.feature.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mju.onda.driver.feature.history.data.HistoryDateRange
import com.mju.onda.driver.feature.history.data.HistoryPeriodFilter
import com.mju.onda.driver.feature.history.data.HistoryRecord
import com.mju.onda.driver.feature.history.data.HistoryOperationsApi
import com.mju.onda.driver.feature.history.data.HistoryRuntimeStateHolder
import com.mju.onda.driver.feature.history.data.MockOperationHistory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperationHistoryUiState(
    val filter: HistoryPeriodFilter = HistoryPeriodFilter.Last7Days,
    val customRange: HistoryDateRange? = null,
    val records: List<HistoryRecord> = emptyList(),
    val rangeLabel: String = MockOperationHistory.rangeLabel(HistoryPeriodFilter.Last7Days),
    val showPeriodPicker: Boolean = false,
    val draftStart: LocalDate? = null,
    val draftEnd: LocalDate? = null,
    val visibleMonth: LocalDate = MockOperationHistory.MOCK_TODAY.withDayOfMonth(1),
) {
    val count: Int get() = records.size

    val draftRangeLabel: String
        get() {
            val start = draftStart ?: return MockOperationHistory.RANGE_CUSTOM_HINT
            val end = draftEnd ?: return MockOperationHistory.formatDayLabel(start)
            return MockOperationHistory.formatRangeLabel(HistoryDateRange(start, end))
        }
}

sealed interface OperationHistoryEvent {
    data object NavigateBack : OperationHistoryEvent
    data object GoToToday : OperationHistoryEvent
    data object OpenSettings : OperationHistoryEvent
    data object MaxRangeExceeded : OperationHistoryEvent
    data object NeedDateSelection : OperationHistoryEvent
    data class OpenDetail(val recordId: String) : OperationHistoryEvent
}

class OperationHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OperationHistoryUiState())
    val uiState: StateFlow<OperationHistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OperationHistoryEvent>()
    val events: SharedFlow<OperationHistoryEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        val state = _uiState.value
        val range = resolveRangeFor(state.filter, state.customRange)
            ?: return

        viewModelScope.launch {
            val records = loadRecords(range)
            _uiState.update {
                it.copy(
                    records = records,
                    rangeLabel = MockOperationHistory.rangeLabel(it.filter, it.customRange),
                )
            }
        }
    }

    fun onBack() {
        viewModelScope.launch { _events.emit(OperationHistoryEvent.NavigateBack) }
    }

    fun onFilterSelected(filter: HistoryPeriodFilter) {
        if (filter == HistoryPeriodFilter.Custom) {
            openPeriodPicker()
            return
        }
        _uiState.update { it.copy(filter = filter, showPeriodPicker = false) }
        val range = resolveRangeFor(filter, _uiState.value.customRange) ?: return
        viewModelScope.launch {
            val records = loadRecords(range)
            _uiState.update {
                it.copy(
                    records = records,
                    rangeLabel = MockOperationHistory.rangeLabel(filter, it.customRange),
                )
            }
        }
    }

    fun openPeriodPicker() {
        _uiState.update { state ->
            state.copy(
                showPeriodPicker = true,
                draftStart = state.customRange?.start,
                draftEnd = state.customRange?.end,
                visibleMonth = MockOperationHistory.MOCK_TODAY.withDayOfMonth(1),
            )
        }
    }

    fun dismissPeriodPicker() {
        _uiState.update { it.copy(showPeriodPicker = false) }
    }

    fun onDraftDayClick(date: LocalDate) {
        if (!MockOperationHistory.isWithinSelectableBounds(date)) return

        val state = _uiState.value
        val start = state.draftStart
        val end = state.draftEnd

        when {
            // 새 선택 시작 (미선택 또는 이미 구간 완료)
            start == null || end != null -> {
                _uiState.update {
                    it.copy(draftStart = date, draftEnd = null)
                }
            }
            // 시작일보다 이전 → 새 시작일로
            date.isBefore(start) -> {
                _uiState.update {
                    it.copy(draftStart = date, draftEnd = null)
                }
            }
            // 같은 날 → 하루 구간
            date == start -> {
                _uiState.update { it.copy(draftEnd = date) }
            }
            // 최대 조회 기간 초과
            ChronoUnit.DAYS.between(start, date) + 1 > MockOperationHistory.MAX_CUSTOM_DAYS -> {
                viewModelScope.launch {
                    _events.emit(OperationHistoryEvent.MaxRangeExceeded)
                }
            }
            else -> {
                _uiState.update { it.copy(draftEnd = date) }
            }
        }
    }

    fun confirmPeriodPicker() {
        val state = _uiState.value
        val start = state.draftStart
        if (start == null) {
            viewModelScope.launch { _events.emit(OperationHistoryEvent.NeedDateSelection) }
            return
        }
        val end = state.draftEnd ?: start
        if (!MockOperationHistory.isValidCustomRange(start, end)) {
            viewModelScope.launch { _events.emit(OperationHistoryEvent.MaxRangeExceeded) }
            return
        }
        val range = HistoryDateRange(start, end)
        _uiState.update { it.copy(filter = HistoryPeriodFilter.Custom, customRange = range, showPeriodPicker = false) }
        viewModelScope.launch {
            val records = loadRecords(range)
            _uiState.update {
                it.copy(
                    records = records,
                    rangeLabel = MockOperationHistory.formatRangeLabel(range),
                )
            }
        }
    }

    fun onRecordClick(recordId: String) {
        viewModelScope.launch { _events.emit(OperationHistoryEvent.OpenDetail(recordId)) }
    }

    fun onTodayTab() {
        viewModelScope.launch { _events.emit(OperationHistoryEvent.GoToToday) }
    }

    fun onSettingsTab() {
        viewModelScope.launch { _events.emit(OperationHistoryEvent.OpenSettings) }
    }

    private fun resolveRangeFor(filter: HistoryPeriodFilter, customRange: HistoryDateRange?): HistoryDateRange? {
        val today = MockOperationHistory.MOCK_TODAY
        return when (filter) {
            HistoryPeriodFilter.Today -> HistoryDateRange(today, today)
            HistoryPeriodFilter.Last7Days -> HistoryDateRange(today.minusDays(6), today)
            HistoryPeriodFilter.Custom -> customRange
        }
    }

    private suspend fun loadRecords(range: HistoryDateRange): List<HistoryRecord> {
        val apiRecords = HistoryOperationsApi.fetchCompletedHistoryForRange(range.start, range.end)
        val runtime = HistoryRuntimeStateHolder.runtimeRecords().filter { record ->
            !record.date.isBefore(range.start) && !record.date.isAfter(range.end)
        }
        val byId = linkedMapOf<String, HistoryRecord>()
        runtime.forEach { byId[it.id.removePrefix("runtime-")] = it }
        apiRecords.forEach { byId[it.id] = it }
        return byId.values.sortedWith(
            compareByDescending<HistoryRecord> { it.date }
                .thenByDescending { it.actualDepart },
        )
    }
}
