package com.onda.mju.student.ui.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.util.Log
import com.onda.mju.student.data.mapper.toRouteUiModels
import com.onda.mju.student.data.remote.dto.OperationDto
import com.onda.mju.student.data.remote.dto.VehicleLocationDto
import com.onda.mju.student.data.remote.repository.OperationRepository
import com.onda.mju.student.data.remote.repository.VehicleLocationRepository
import com.onda.mju.student.ui.component.StudentBottomNavBar
import com.onda.mju.student.ui.component.StudentBottomTab
import com.onda.mju.student.ui.screen.community.CommunityCreateScreen
import com.onda.mju.student.ui.screen.community.CommunityDetailScreen
import com.onda.mju.student.ui.screen.community.CommunityListScreen
import com.onda.mju.student.ui.screen.community.sampleCommunityReports
import com.onda.mju.student.ui.screen.favorite.FavoriteScreen
import com.onda.mju.student.ui.screen.home.StudentHomeScreen
import com.onda.mju.student.ui.screen.legal.LegalDocumentScreen
import com.onda.mju.student.ui.screen.legal.LegalType
import com.onda.mju.student.ui.screen.my.AccountInfoScreen
import com.onda.mju.student.ui.screen.my.FavoriteManageScreen
import com.onda.mju.student.ui.screen.my.LogoutConfirmScreen
import com.onda.mju.student.ui.screen.my.MyHomeScreen
import com.onda.mju.student.ui.screen.my.MyReportDetailScreen
import com.onda.mju.student.ui.screen.my.MyReportsScreen
import com.onda.mju.student.ui.screen.my.NotificationSettingsScreen
import com.onda.mju.student.ui.screen.notice.NoticeDetailScreen
import com.onda.mju.student.ui.screen.notice.NoticeListScreen
import com.onda.mju.student.ui.screen.notice.StopGuideDetailScreen
import com.onda.mju.student.ui.screen.notice.StopGuideListScreen
import com.onda.mju.student.ui.screen.notice.StopGuideScreen
import com.onda.mju.student.ui.screen.notice.TimetableScreen
import com.onda.mju.student.ui.screen.notice.sampleNotices
import com.onda.mju.student.ui.screen.notification.NotificationDetailScreen
import com.onda.mju.student.ui.screen.notification.NotificationListScreen
import com.onda.mju.student.ui.screen.notification.latestHomeNotice
import com.onda.mju.student.ui.screen.notification.sampleNotifications
import com.onda.mju.student.ui.screen.notification.unreadNotificationCount as countUnreadNotifications
import com.onda.mju.student.ui.screen.route.BusDetailScreen
import com.onda.mju.student.ui.screen.route.LiveVehicle
import com.onda.mju.student.ui.screen.route.RouteListScreen
import com.onda.mju.student.ui.screen.route.RouteLiveScreen
import com.onda.mju.student.ui.screen.route.RouteUiModel
import com.onda.mju.student.ui.screen.route.StopLiveScreen
import com.onda.mju.student.ui.screen.route.VehicleStatus
import com.onda.mju.student.ui.screen.route.sampleRouteList
import com.onda.mju.student.ui.screen.route.sampleRouteLive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private sealed interface MainOverlay {
    data object None : MainOverlay
    data object Favorites : MainOverlay
    data object NotificationList : MainOverlay
    data class NotificationDetail(val id: Int, val returnToList: Boolean = true) : MainOverlay

    data class CommunityDetail(val id: String) : MainOverlay
    data object CommunityCreate : MainOverlay

    data class NoticeDetail(val id: Int) : MainOverlay
    data object Timetable : MainOverlay
    data object StopGuide : MainOverlay
    data class StopGuideList(val routeId: String) : MainOverlay
    data class StopGuideDetail(val stopId: String, val returnRouteId: String) : MainOverlay

    data class RouteLive(val routeId: String) : MainOverlay
    data class StopLive(val stopId: String, val returnRouteId: String? = null) : MainOverlay
    data class BusDetail(
        val vehicleId: String,
        val returnRouteId: String? = null,
        val returnStopId: String? = null,
    ) : MainOverlay

    data object AccountInfo : MainOverlay
    data object FavoriteManage : MainOverlay
    data object NotificationSettings : MainOverlay
    data object MyReports : MainOverlay
    data class MyReportDetail(val id: String) : MainOverlay
    data class MyReportEdit(val id: String) : MainOverlay
    data object LogoutConfirm : MainOverlay
    data class Legal(val type: LegalType) : MainOverlay
}

@Composable
fun StudentMainShell(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(StudentBottomTab.Home) }
    var overlay by remember { mutableStateOf<MainOverlay>(MainOverlay.None) }
    val notifications = remember { sampleNotifications() }
    val notices = remember { sampleNotices() }
    var communityReports by remember { mutableStateOf(sampleCommunityReports()) }
    var myReportIds by remember { mutableStateOf(setOf("r1", "r2")) }
    var readReportIds by remember { mutableStateOf(setOf("r3", "r4")) }
    val myReports = remember(communityReports, myReportIds) {
        communityReports.filter { it.id in myReportIds }
    }
    val latestHomeNotice = remember(notifications) { notifications.latestHomeNotice() }
    var unreadIds by remember {
        mutableStateOf(notifications.filter { it.initiallyUnread }.map { it.id }.toSet())
    }
    var markAllDone by remember { mutableStateOf(false) }
    // Keeps unread-only filter across notification detail round-trips.
    var notificationShowUnreadOnly by remember { mutableStateOf(false) }
    // Mock: treat home entry as the last operation-data receive time.
    // When Supabase realtime/operation payloads arrive, set this to System.currentTimeMillis().
    var operationLastUpdatedAtMillis by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }
    var timetableReturnRouteId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val operationRepository = remember { OperationRepository() }
    val vehicleLocationRepository = remember { VehicleLocationRepository() }
    var routes by remember { mutableStateOf<List<RouteUiModel>>(sampleRouteList()) }
    var operations by remember { mutableStateOf<List<OperationDto>>(emptyList()) }
    val operationLocations = remember {
        mutableStateMapOf<String, VehicleLocationDto>()
    }

    // Temporary: verify Supabase operations read path + realtime status updates.
    LaunchedEffect(Unit) {
        val locationJobs = mutableMapOf<String, Job>()

        suspend fun startLocationSubscription(operationId: String) {
            if (locationJobs.containsKey(operationId)) return

            val latestLocation = vehicleLocationRepository.getLatestLocation(operationId)
            if (latestLocation != null) {
                Log.d(
                    "ONDA_SUPABASE",
                    "multi location operationId=${latestLocation.operationId}, " +
                        "latitude=${latestLocation.latitude}, longitude=${latestLocation.longitude}, " +
                        "recordedAt=${latestLocation.recordedAt}",
                )
                operationLocations[operationId] = latestLocation
            } else {
                Log.d("ONDA_SUPABASE", "multi location operationId=$operationId, latest=null")
            }

            locationJobs[operationId] = launch {
                try {
                    vehicleLocationRepository.observeLocations(operationId).collect { location ->
                        Log.d(
                            "ONDA_SUPABASE",
                            "multi location operationId=${location.operationId}, " +
                                "latitude=${location.latitude}, longitude=${location.longitude}, " +
                                "recordedAt=${location.recordedAt}",
                        )
                        operationLocations[operationId] = location
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("ONDA_SUPABASE", "location observe failed operationId=$operationId: ${e.message}", e)
                }
            }
        }

        fun stopLocationSubscription(operationId: String) {
            locationJobs.remove(operationId)?.cancel()
            operationLocations.remove(operationId)
            Log.d("ONDA_SUPABASE", "operation removed from live vehicles operationId=$operationId")
        }

        try {
            val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val fetchedOperations = operationRepository.getOperations()
            Log.d("ONDA_SUPABASE", "operations fetch success=true, count=${fetchedOperations.size}")
            fetchedOperations.firstOrNull()?.let { first ->
                Log.d(
                    "ONDA_SUPABASE",
                    "first operation id=${first.id}, operationDate=${first.operationDate}, status=${first.status}, " +
                        "departureTime=${first.schedule?.departureTime}, routeName=${first.schedule?.route?.routeName}",
                )
            }
            val routeUiModels = fetchedOperations.toRouteUiModels()
            routeUiModels.forEach { route ->
                Log.d(
                    "ONDA_SUPABASE",
                    "routeUi id=${route.id}, name=${route.name}, status=${route.status}, " +
                        "activeVehicleCount=${route.activeVehicleCount}, nextDeparture=${route.nextDeparture}",
                )
            }
            routes = routeUiModels
            operations = fetchedOperations

            val inProgressOperations = fetchedOperations.filter { it.status == "IN_PROGRESS" }
            inProgressOperations.forEach { operation ->
                Log.d(
                    "ONDA_SUPABASE",
                    "operation vehicle operationId=${operation.id}, " +
                        "routeName=${operation.schedule?.route?.routeName}, " +
                        "busId=${operation.busId ?: operation.bus?.id}, " +
                        "busName=${operation.bus?.busName}, " +
                        "vehicleNumber=${operation.bus?.vehicleNumber}, " +
                        "status=${operation.status}",
                )
            }

            inProgressOperations.forEach { operation ->
                startLocationSubscription(operation.id)
            }

            // Realtime: today's operations status changes (start/end).
            // Requires public.operations in supabase_realtime publication.
            launch {
                try {
                    Log.d(
                        "ONDA_SUPABASE",
                        "operations realtime subscribe start date=$today " +
                            "(requires public.operations in supabase_realtime publication)",
                    )
                    operationRepository.observeOperationUpdates(today).collect { update ->
                        val record = update.record
                        val existing = operations.firstOrNull { it.id == record.id }
                        val oldStatus = update.previousStatus ?: existing?.status
                        val newStatus = record.status

                        Log.d(
                            "ONDA_SUPABASE",
                            "operation realtime update operationId=${record.id}, " +
                                "oldStatus=$oldStatus, newStatus=$newStatus",
                        )

                        if (existing == null) {
                            Log.d(
                                "ONDA_SUPABASE",
                                "operation realtime update skipped (not in today list) operationId=${record.id}",
                            )
                            return@collect
                        }

                        val merged = operations.map { op ->
                            if (op.id != record.id) {
                                op
                            } else {
                                op.copy(
                                    operationDate = record.operationDate,
                                    status = record.status,
                                    startedAt = record.startedAt,
                                    endedAt = record.endedAt,
                                    scheduleId = record.scheduleId,
                                    busId = record.busId,
                                )
                            }
                        }
                        operations = merged
                        routes = merged.toRouteUiModels()

                        val wasInProgress = oldStatus == "IN_PROGRESS"
                        val isInProgress = newStatus == "IN_PROGRESS"
                        when {
                            !wasInProgress && isInProgress -> {
                                startLocationSubscription(record.id)
                            }
                            wasInProgress && !isInProgress -> {
                                stopLocationSubscription(record.id)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        "ONDA_SUPABASE",
                        "operations realtime failed (check supabase_realtime publication includes public.operations): ${e.message}",
                        e,
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ONDA_SUPABASE", "operations fetch failed: ${e.message}", e)
        }
    }

    fun showTodo(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun openRouteLive(routeId: String) {
        selectedTab = StudentBottomTab.Route
        overlay = MainOverlay.RouteLive(routeId)
    }

    fun openNotifications() {
        selectedTab = StudentBottomTab.Home
        notificationShowUnreadOnly = false
        overlay = MainOverlay.NotificationList
    }

    fun openNoticeDetailFromAlert(id: Int, returnToList: Boolean) {
        unreadIds = unreadIds - id
        overlay = MainOverlay.NotificationDetail(id = id, returnToList = returnToList)
    }

    fun markAllRead() {
        unreadIds = emptySet()
        markAllDone = true
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "모든 알림을 읽음 처리했습니다.",
                actionLabel = "확인",
                duration = SnackbarDuration.Short,
            )
        }
    }

    val bottomSelectedTab = when (overlay) {
        MainOverlay.None -> selectedTab
        MainOverlay.Favorites,
        MainOverlay.NotificationList,
        is MainOverlay.NotificationDetail,
        -> StudentBottomTab.Home

        is MainOverlay.CommunityDetail,
        MainOverlay.CommunityCreate,
        -> StudentBottomTab.Community

        is MainOverlay.NoticeDetail,
        MainOverlay.StopGuide,
        is MainOverlay.StopGuideList,
        is MainOverlay.StopGuideDetail,
        -> StudentBottomTab.Notice

        MainOverlay.Timetable -> if (timetableReturnRouteId != null) {
            StudentBottomTab.Route
        } else {
            selectedTab
        }

        is MainOverlay.RouteLive,
        is MainOverlay.StopLive,
        is MainOverlay.BusDetail,
        -> StudentBottomTab.Route

        MainOverlay.AccountInfo,
        MainOverlay.FavoriteManage,
        MainOverlay.NotificationSettings,
        MainOverlay.MyReports,
        is MainOverlay.MyReportDetail,
        is MainOverlay.MyReportEdit,
        MainOverlay.LogoutConfirm,
        is MainOverlay.Legal,
        -> StudentBottomTab.My
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            StudentBottomNavBar(
                selectedTab = bottomSelectedTab,
                onTabSelected = { tab ->
                    overlay = MainOverlay.None
                    selectedTab = tab
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            when (val current = overlay) {
                MainOverlay.Favorites -> {
                    FavoriteScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onManageClick = { overlay = MainOverlay.FavoriteManage },
                        onRouteClick = { routeId -> openRouteLive(routeId) },
                        onStopClick = { stopId ->
                            overlay = MainOverlay.StopLive(stopId = stopId)
                        },
                        onNotificationSettingClick = { overlay = MainOverlay.NotificationSettings },
                    )
                }

                MainOverlay.NotificationList -> {
                    NotificationListScreen(
                        notifications = notifications,
                        unreadIds = unreadIds,
                        markAllDone = markAllDone,
                        modifier = Modifier.fillMaxSize(),
                        showUnreadOnly = notificationShowUnreadOnly,
                        onShowUnreadOnlyChange = { notificationShowUnreadOnly = it },
                        onBackClick = {
                            notificationShowUnreadOnly = false
                            overlay = MainOverlay.None
                        },
                        onMarkAllRead = { markAllRead() },
                        onNotificationClick = { id -> openNoticeDetailFromAlert(id, true) },
                    )
                }

                is MainOverlay.NotificationDetail -> {
                    val item = notifications.first { it.id == current.id }
                    NotificationDetailScreen(
                        item = item,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            overlay = if (current.returnToList) {
                                MainOverlay.NotificationList
                            } else {
                                MainOverlay.None
                            }
                        },
                        onRelatedMenuClick = { showTodo("관련 화면은 준비 중입니다.") },
                    )
                }

                is MainOverlay.CommunityDetail -> {
                    val report = communityReports.first { it.id == current.id }
                    CommunityDetailScreen(
                        report = report,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                    )
                }

                MainOverlay.CommunityCreate -> {
                    CommunityCreateScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onNotificationClick = { openNotifications() },
                        onSubmit = { report ->
                            communityReports = listOf(report) + communityReports
                            myReportIds = myReportIds + report.id
                            showTodo("제보가 등록되었습니다.")
                            overlay = MainOverlay.None
                            selectedTab = StudentBottomTab.Community
                        },
                    )
                }

                is MainOverlay.NoticeDetail -> {
                    val item = notices.first { it.id == current.id }
                    NoticeDetailScreen(
                        item = item,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onNotificationClick = { openNotifications() },
                        onAttachmentClick = { showTodo("첨부파일 다운로드는 준비 중입니다.") },
                    )
                }

                MainOverlay.Timetable -> {
                    TimetableScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            val routeId = timetableReturnRouteId
                            timetableReturnRouteId = null
                            overlay = if (routeId != null) {
                                MainOverlay.RouteLive(routeId)
                            } else {
                                MainOverlay.None
                            }
                        },
                    )
                }

                MainOverlay.StopGuide -> {
                    StopGuideScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onRouteClick = { routeId ->
                            overlay = MainOverlay.StopGuideList(routeId)
                        },
                    )
                }

                is MainOverlay.StopGuideList -> {
                    StopGuideListScreen(
                        routeId = current.routeId,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.StopGuide },
                        onStopClick = { stopId ->
                            overlay = MainOverlay.StopGuideDetail(
                                stopId = stopId,
                                returnRouteId = current.routeId,
                            )
                        },
                    )
                }

                is MainOverlay.StopGuideDetail -> {
                    StopGuideDetailScreen(
                        stopId = current.stopId,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            overlay = MainOverlay.StopGuideList(current.returnRouteId)
                        },
                        onOpenMapClick = {
                            showTodo("지도 앱 연동은 준비 중입니다.")
                        },
                        onLiveClick = {
                            // Map guide stop ids to live mock stops where possible.
                            val liveStopId = when (current.stopId) {
                                "luxnine" -> "s3"
                                else -> "s3"
                            }
                            selectedTab = StudentBottomTab.Route
                            overlay = MainOverlay.StopLive(
                                stopId = liveStopId,
                                returnRouteId = when (current.returnRouteId) {
                                    "giheung" -> "giheung"
                                    "myeongji" -> "myeongji_station"
                                    else -> "city_shuttle"
                                },
                            )
                        },
                    )
                }

                is MainOverlay.RouteLive -> {
                    val baseLiveData = sampleRouteLive(current.routeId)
                    val routeUi = routes.firstOrNull { it.id == current.routeId }
                    val routeName = routeUi?.name ?: routeDisplayNameForId(current.routeId)
                    val liveVehicles = operations
                        .asSequence()
                        .filter { it.status == "IN_PROGRESS" }
                        .filter { it.schedule?.route?.routeName == routeName }
                        .map { operation ->
                            val location = operationLocations[operation.id]
                            LiveVehicle(
                                id = operation.id,
                                label = operation.bus?.busName
                                    ?: operation.bus?.vehicleNumber
                                    ?: "운행 차량",
                                status = VehicleStatus.Running,
                                latitude = location?.latitude,
                                longitude = location?.longitude,
                                speed = location?.speed,
                                heading = location?.heading,
                                recordedAt = location?.recordedAt,
                            )
                        }
                        .toList()
                    liveVehicles.forEach { vehicle ->
                        Log.d(
                            "ONDA_SUPABASE",
                            "route vehicle operationId=${vehicle.id}, label=${vehicle.label}, " +
                                "latitude=${vehicle.latitude}, longitude=${vehicle.longitude}, " +
                                "recordedAt=${vehicle.recordedAt}",
                        )
                    }
                    val liveData = baseLiveData.copy(
                        routeName = routeUi?.name ?: baseLiveData.routeName,
                        runningCount = routeUi?.activeVehicleCount ?: 0,
                        nextDeparture = routeUi?.nextDeparture ?: baseLiveData.nextDeparture,
                        vehicles = liveVehicles,
                    )
                    RouteLiveScreen(
                        routeId = current.routeId,
                        modifier = Modifier.fillMaxSize(),
                        liveData = liveData,
                        onBackClick = { overlay = MainOverlay.None },
                        onStopClick = { stopId ->
                            overlay = MainOverlay.StopLive(
                                stopId = stopId,
                                returnRouteId = current.routeId,
                            )
                        },
                        onVehicleClick = { vehicleId ->
                            overlay = MainOverlay.BusDetail(
                                vehicleId = vehicleId,
                                returnRouteId = current.routeId,
                            )
                        },
                        onTimetableClick = {
                            timetableReturnRouteId = current.routeId
                            overlay = MainOverlay.Timetable
                        },
                    )
                }

                is MainOverlay.StopLive -> {
                    StopLiveScreen(
                        stopId = current.stopId,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            overlay = current.returnRouteId?.let { MainOverlay.RouteLive(it) }
                                ?: MainOverlay.None
                        },
                        onVehicleClick = { vehicleId ->
                            overlay = MainOverlay.BusDetail(
                                vehicleId = vehicleId,
                                returnRouteId = current.returnRouteId,
                                returnStopId = current.stopId,
                            )
                        },
                    )
                }

                is MainOverlay.BusDetail -> {
                    BusDetailScreen(
                        vehicleId = current.vehicleId,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            overlay = when {
                                current.returnStopId != null -> MainOverlay.StopLive(
                                    stopId = current.returnStopId,
                                    returnRouteId = current.returnRouteId,
                                )
                                current.returnRouteId != null -> MainOverlay.RouteLive(current.returnRouteId)
                                else -> MainOverlay.None
                            }
                        },
                        onReportClick = {
                            selectedTab = StudentBottomTab.Community
                            overlay = MainOverlay.CommunityCreate
                        },
                        onMoreReportsClick = {
                            selectedTab = StudentBottomTab.Community
                            overlay = MainOverlay.None
                        },
                    )
                }

                MainOverlay.AccountInfo -> {
                    AccountInfoScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onChangePasswordClick = { showTodo("비밀번호 변경은 준비 중입니다.") },
                        onLogoutClick = { overlay = MainOverlay.LogoutConfirm },
                        onDeleteAccountClick = { showTodo("회원 탈퇴는 준비 중입니다.") },
                    )
                }

                MainOverlay.FavoriteManage -> {
                    FavoriteManageScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onSaveClick = {
                            showTodo("즐겨찾기가 저장되었습니다.")
                            overlay = MainOverlay.None
                        },
                    )
                }

                MainOverlay.NotificationSettings -> {
                    NotificationSettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                    )
                }

                MainOverlay.MyReports -> {
                    MyReportsScreen(
                        modifier = Modifier.fillMaxSize(),
                        reports = myReports,
                        onBackClick = { overlay = MainOverlay.None },
                        onReportClick = { id ->
                            overlay = MainOverlay.MyReportDetail(id)
                        },
                    )
                }

                is MainOverlay.MyReportDetail -> {
                    val report = communityReports.firstOrNull { it.id == current.id }
                        ?: communityReports.first()
                    MyReportDetailScreen(
                        report = report,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.MyReports },
                        onEditClick = { overlay = MainOverlay.MyReportEdit(report.id) },
                        onDeleteClick = {
                            communityReports = communityReports.filterNot { it.id == report.id }
                            myReportIds = myReportIds - report.id
                            showTodo("제보가 삭제되었습니다.")
                            overlay = MainOverlay.MyReports
                        },
                    )
                }

                is MainOverlay.MyReportEdit -> {
                    val report = communityReports.firstOrNull { it.id == current.id }
                        ?: communityReports.first()
                    CommunityCreateScreen(
                        modifier = Modifier.fillMaxSize(),
                        initialReport = report,
                        onBackClick = { overlay = MainOverlay.MyReportDetail(report.id) },
                        onSubmit = { updated ->
                            communityReports = communityReports.map {
                                if (it.id == updated.id) updated else it
                            }
                            showTodo("제보가 수정되었습니다.")
                            overlay = MainOverlay.MyReportDetail(updated.id)
                        },
                    )
                }

                MainOverlay.LogoutConfirm -> {
                    LogoutConfirmScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onConfirmLogout = onLogout,
                        onCancelClick = { overlay = MainOverlay.None },
                    )
                }

                is MainOverlay.Legal -> {
                    LegalDocumentScreen(
                        type = current.type,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                    )
                }

                MainOverlay.None -> {
                    when (selectedTab) {
                        StudentBottomTab.Home -> {
                            StudentHomeScreen(
                                modifier = Modifier.fillMaxSize(),
                                noticeBannerTitle = latestHomeNotice?.title
                                    ?: "등록된 공지가 없습니다.",
                                unreadNotificationCount = countUnreadNotifications(unreadIds),
                                operationLastUpdatedAtMillis = operationLastUpdatedAtMillis,
                                routes = routes,
                                onNotificationClick = { openNotifications() },
                                onStatusTimetableClick = {
                                    timetableReturnRouteId = null
                                    overlay = MainOverlay.Timetable
                                },
                                onNoticeBannerClick = {
                                    val notice = latestHomeNotice
                                    if (notice != null) {
                                        openNoticeDetailFromAlert(notice.id, false)
                                    } else {
                                        showTodo("등록된 공지가 없습니다.")
                                    }
                                },
                                onFavoriteManageClick = { overlay = MainOverlay.FavoriteManage },
                                onFavoriteClick = { overlay = MainOverlay.Favorites },
                                onRouteShortcutClick = { routeId -> openRouteLive(routeId) },
                                onQuickActionClick = { action ->
                                    when (action) {
                                        "간편 제보" -> {
                                            selectedTab = StudentBottomTab.Community
                                            overlay = MainOverlay.CommunityCreate
                                        }
                                        "공지사항" -> selectedTab = StudentBottomTab.Notice
                                        "전체 시간표" -> {
                                            timetableReturnRouteId = null
                                            overlay = MainOverlay.Timetable
                                        }
                                        else -> showTodo("$action 화면은 준비 중입니다.")
                                    }
                                },
                            )
                        }

                        StudentBottomTab.Route -> {
                            RouteListScreen(
                                modifier = Modifier.fillMaxSize(),
                                routes = routes,
                                onRouteClick = { routeId -> openRouteLive(routeId) },
                                onFavoriteClick = { routeId ->
                                    routes = routes.map { route ->
                                        if (route.id == routeId) {
                                            route.copy(isFavorite = !route.isFavorite)
                                        } else {
                                            route
                                        }
                                    }
                                },
                            )
                        }

                        StudentBottomTab.Community -> {
                            CommunityListScreen(
                                modifier = Modifier.fillMaxSize(),
                                reports = communityReports,
                                readIds = readReportIds,
                                onReportClick = { id ->
                                    readReportIds = readReportIds + id
                                    overlay = MainOverlay.CommunityDetail(id)
                                },
                                onCreateClick = {
                                    overlay = MainOverlay.CommunityCreate
                                },
                            )
                        }

                        StudentBottomTab.Notice -> {
                            NoticeListScreen(
                                modifier = Modifier.fillMaxSize(),
                                notices = notices,
                                onNoticeClick = { id ->
                                    overlay = MainOverlay.NoticeDetail(id)
                                },
                                onTimetableClick = { overlay = MainOverlay.Timetable },
                                onStopGuideClick = { overlay = MainOverlay.StopGuide },
                            )
                        }

                        StudentBottomTab.My -> {
                            MyHomeScreen(
                                modifier = Modifier.fillMaxSize(),
                                onAccountClick = { overlay = MainOverlay.AccountInfo },
                                onFavoriteManageClick = { overlay = MainOverlay.FavoriteManage },
                                onNotificationSettingClick = {
                                    overlay = MainOverlay.NotificationSettings
                                },
                                onMyReportsClick = { overlay = MainOverlay.MyReports },
                                onPrivacyClick = {
                                    overlay = MainOverlay.Legal(LegalType.Privacy)
                                },
                                onTermsClick = {
                                    overlay = MainOverlay.Legal(LegalType.Terms)
                                },
                                onLogoutClick = { overlay = MainOverlay.LogoutConfirm },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Matches RouteOperationMapper route id ↔ DB route_name mapping. */
private fun routeDisplayNameForId(routeId: String): String = when (routeId) {
    "giheung" -> "기흥역 통학버스"
    "myeongji_station" -> "명지대역 셔틀"
    "city_shuttle" -> "시내 셔틀"
    else -> routeId
}
