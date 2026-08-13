package com.mju.onda.driver.core.location

import android.util.Log
import com.mju.onda.driver.core.calendar.AcademicCalendar
import com.mju.onda.driver.feature.home.data.MockTodayOperations
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.inoperation.data.OperationalRouteResolver
import com.mju.onda.driver.feature.inoperation.data.RouteStop
import com.mju.onda.driver.feature.inoperation.data.RouteStopsApi
import com.mju.onda.driver.feature.inoperation.data.RouteStopsCatalog
import com.mju.onda.driver.feature.inoperation.data.StopProgressTracker
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgress
import com.mju.onda.driver.feature.inoperation.data.StopRouteProgressState
import com.mju.onda.driver.feature.operation.data.MockOperationDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.jvm.Volatile

/**
 * 운행 중 정류장 진행을 화면과 무관하게 유지한다.
 * GPS가 들어오는 동안 인덱스를 전진시키고 DB에 저장한다.
 */
object OperationStopProgressCoordinator {
    private const val TAG = "ONDA_STOP_PROGRESS"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val lock = Any()

    private var attachedOperationId: String = ""
    private var routeName: String = ""
    private var vehicleName: String = ""
    private var stops: List<RouteStop> = emptyList()
    private var tracker: StopProgressTracker = StopProgressTracker()
    private var lastWrittenPassed: Int = Int.MIN_VALUE
    private var lastWrittenArrived: Int = Int.MIN_VALUE
    @Volatile
    private var persistInFlight: Boolean = false
    private var gpsJob: Job? = null

    private val _uiState = MutableStateFlow(
        StopRouteProgress.initial(routeName = "", vehicleName = "", stops = emptyList()),
    )
    val uiState: StateFlow<StopRouteProgressState> = _uiState.asStateFlow()

    private val _loadingStops = MutableStateFlow(false)
    val loadingStops: StateFlow<Boolean> = _loadingStops.asStateFlow()

    fun attach(operationId: String) {
        if (operationId.isBlank()) return
        scope.launch {
            mutex.withLock {
                loadLocked(operationId)
            }
            startGpsLoop()
        }
    }

    fun clear() {
        gpsJob?.cancel()
        gpsJob = null
        scope.launch {
            mutex.withLock {
                synchronized(lock) {
                    attachedOperationId = ""
                    routeName = ""
                    vehicleName = ""
                    stops = emptyList()
                    tracker = StopProgressTracker()
                    lastWrittenPassed = Int.MIN_VALUE
                    lastWrittenArrived = Int.MIN_VALUE
                    persistInFlight = false
                    _uiState.value = StopRouteProgress.initial("", "", emptyList())
                    _loadingStops.value = false
                }
            }
        }
    }

    private suspend fun loadLocked(operationId: String) {
        val alreadyReady = synchronized(lock) {
            attachedOperationId == operationId && stops.isNotEmpty() && routeName.isNotBlank()
        }
        val op = MockTodayOperations.findById(operationId)
        if (alreadyReady) {
            val familyOk = op == null ||
                OperationalRouteResolver.baseRouteFamily(routeName) ==
                    OperationalRouteResolver.baseRouteFamily(op.routeName)
            if (familyOk) {
                refreshLocked()
                return
            }
            Log.w(TAG, "reload: attached route='$routeName' != assignment='${op?.routeName}' op=$operationId")
        }

        val detail = MockOperationDetail.forOperationId(operationId)
        val assignedRoute = op?.routeName?.takeIf { it.isNotBlank() } ?: detail.routeName
        Log.i(
            TAG,
            "load op=$operationId found=${op != null} route='$assignedRoute' dbId=${op?.dbId} id=${op?.id}",
        )
        if (assignedRoute.isBlank()) {
            Log.w(TAG, "load aborted: blank route for op=$operationId — not falling back to 기흥역")
            synchronized(lock) {
                attachedOperationId = operationId
                routeName = ""
                vehicleName = op?.vehicleName.orEmpty()
                stops = emptyList()
                tracker = StopProgressTracker()
            }
            _uiState.value = StopRouteProgress.initial(routeName = "", vehicleName = "", stops = emptyList())
            return
        }
        val departTime = op?.departTime.orEmpty().ifBlank { detail.departTime }
        val resolvedRoute = OperationalRouteResolver.resolveOperationalRouteName(
            routeNameFromAssignment = assignedRoute,
            departureTime = departTime,
            date = AcademicCalendar.todayDateKey(),
        )
        val resolvedVehicle = op?.vehicleName ?: detail.vehicleName
        val catalogStops = RouteStopsCatalog.stopsForRouteName(resolvedRoute)

        synchronized(lock) {
            val routeChanged = attachedOperationId != operationId || routeName != resolvedRoute
            attachedOperationId = operationId
            routeName = resolvedRoute
            vehicleName = resolvedVehicle
            stops = catalogStops
            if (routeChanged) {
                tracker = StopProgressTracker()
                lastWrittenPassed = Int.MIN_VALUE
                lastWrittenArrived = Int.MIN_VALUE
            }
        }

        val saved = runCatching { OperationStopProgressApi.fetch(operationId) }.getOrNull()
        synchronized(lock) {
            if (saved != null) {
                tracker = mergeTracker(tracker, trackerFromSnapshot(stops, saved))
                lastWrittenPassed = tracker.lastPassedIndex
                lastWrittenArrived = tracker.lastArrivedIndex
            }
        }
        refreshLocked()

        _loadingStops.value = true
        val fromDb = runCatching { RouteStopsApi.fetchStopsForRouteName(resolvedRoute) }
            .getOrDefault(emptyList())
        if (fromDb.isNotEmpty()) {
            synchronized(lock) {
                stops = fromDb
                if (saved != null) {
                    tracker = mergeTracker(tracker, trackerFromSnapshot(fromDb, saved))
                    lastWrittenPassed = tracker.lastPassedIndex
                    lastWrittenArrived = tracker.lastArrivedIndex
                }
            }
            refreshLocked()
        }
        _loadingStops.value = false
    }

    private fun startGpsLoop() {
        if (gpsJob?.isActive == true) return
        gpsJob = scope.launch {
            LatestLocationHolder.latestFlow.collect { applyFix(it) }
        }
    }

    private fun applyFix(fix: LatestLocationHolder.Fix?) {
        val operationId: String
        val currentStops: List<RouteStop>
        val currentRoute: String
        val currentVehicle: String
        val currentTracker: StopProgressTracker
        synchronized(lock) {
            operationId = attachedOperationId
            currentStops = stops
            currentRoute = routeName
            currentVehicle = vehicleName
            currentTracker = tracker
        }
        if (operationId.isBlank() || currentStops.isEmpty()) return

        val useFix = fix != null && (
            fix.operationId.isBlank() ||
                fix.operationId == operationId ||
                OperationRuntimeStateHolder.isInProgress(operationId)
            )
        val (state, nextTracker) = StopRouteProgress.resolve(
            routeName = currentRoute,
            vehicleName = currentVehicle,
            stops = currentStops,
            lat = if (useFix) fix?.latitude else null,
            lng = if (useFix) fix?.longitude else null,
            tracker = currentTracker,
        )
        val changed: Boolean
        synchronized(lock) {
            if (attachedOperationId != operationId) return
            changed = nextTracker != tracker
            tracker = nextTracker
            _uiState.value = state
        }
        persist(operationId, currentStops, nextTracker)
        if (changed) {
            Log.d(TAG, "tracker operationId=$operationId arrived=${nextTracker.lastArrivedIndex} passed=${nextTracker.lastPassedIndex}")
        }
    }

    private fun refreshLocked() {
        val fix = LatestLocationHolder.latest
        applyFix(fix)
    }

    private fun persist(
        operationId: String,
        currentStops: List<RouteStop>,
        nextTracker: StopProgressTracker,
    ) {
        val passed = nextTracker.lastPassedIndex
        val arrived = nextTracker.lastArrivedIndex
        synchronized(lock) {
            if (passed == lastWrittenPassed && arrived == lastWrittenArrived) return
            if (persistInFlight) return
            persistInFlight = true
        }
        scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                OperationStopProgressApi.upsert(
                    operationId = operationId,
                    arrivedStopId = currentStops.stopIdAt(arrived),
                    passedStopId = currentStops.stopIdAt(passed),
                    arrivedIndex = arrived,
                    passedIndex = passed,
                )
            }.onFailure { e ->
                Log.d(TAG, "upsert skipped: ${e.message}")
            }.getOrDefault(false)
            synchronized(lock) {
                persistInFlight = false
                if (ok) {
                    lastWrittenPassed = passed
                    lastWrittenArrived = arrived
                }
            }
        }
    }

    private fun trackerFromSnapshot(
        currentStops: List<RouteStop>,
        saved: OperationStopProgressApi.Snapshot,
    ): StopProgressTracker {
        val last = currentStops.lastIndex
        if (last < 0) return saved.toTracker()
        val passed = when {
            saved.lastPassedIndex in 0..last -> saved.lastPassedIndex
            !saved.lastPassedStopId.isNullOrBlank() ->
                currentStops.indexOfFirst { it.id == saved.lastPassedStopId }
            else -> -1
        }.coerceIn(-1, last)
        val arrived = when {
            saved.lastArrivedIndex in 0..last -> saved.lastArrivedIndex
            !saved.lastArrivedStopId.isNullOrBlank() ->
                currentStops.indexOfFirst { it.id == saved.lastArrivedStopId }
            else -> -1
        }.coerceIn(-1, last)
        return StopProgressTracker(
            lastPassedIndex = passed,
            lastArrivedIndex = maxOf(arrived, passed),
        )
    }

    private fun mergeTracker(
        local: StopProgressTracker,
        saved: StopProgressTracker,
    ): StopProgressTracker {
        val passed = maxOf(local.lastPassedIndex, saved.lastPassedIndex)
        val arrived = maxOf(local.lastArrivedIndex, saved.lastArrivedIndex, passed)
        return StopProgressTracker(lastPassedIndex = passed, lastArrivedIndex = arrived)
    }

    private fun List<RouteStop>.stopIdAt(index: Int): String? =
        getOrNull(index)?.id
}
