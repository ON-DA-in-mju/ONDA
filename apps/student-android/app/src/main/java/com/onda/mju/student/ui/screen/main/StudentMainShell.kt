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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.onda.mju.student.ui.screen.route.BusDetailScreen
import com.onda.mju.student.ui.screen.route.RouteListScreen
import com.onda.mju.student.ui.screen.route.RouteLiveScreen
import com.onda.mju.student.ui.screen.route.StopLiveScreen
import kotlinx.coroutines.launch

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
    var timetableReturnRouteId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showTodo(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun openRouteLive(routeId: String) {
        selectedTab = StudentBottomTab.Route
        overlay = MainOverlay.RouteLive(routeId)
    }

    fun openNotifications() {
        selectedTab = StudentBottomTab.Home
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
        MainOverlay.Timetable,
        MainOverlay.StopGuide,
        is MainOverlay.StopGuideList,
        is MainOverlay.StopGuideDetail,
        -> if (timetableReturnRouteId != null) {
            StudentBottomTab.Route
        } else {
            StudentBottomTab.Notice
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
                        onBackClick = { overlay = MainOverlay.None },
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
                    RouteLiveScreen(
                        routeId = current.routeId,
                        modifier = Modifier.fillMaxSize(),
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
                                onNotificationClick = { openNotifications() },
                                onStatusTimetableClick = {
                                    selectedTab = StudentBottomTab.Notice
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
                                onRouteShortcutClick = { routeName ->
                                    val routeId = when {
                                        routeName.contains("기흥") -> "giheung"
                                        routeName.contains("명지대") -> "myeongji_station"
                                        else -> "city_shuttle"
                                    }
                                    openRouteLive(routeId)
                                },
                                onQuickActionClick = { action ->
                                    when (action) {
                                        "간편 제보" -> {
                                            selectedTab = StudentBottomTab.Community
                                            overlay = MainOverlay.CommunityCreate
                                        }
                                        "공지사항" -> selectedTab = StudentBottomTab.Notice
                                        "전체 시간표" -> {
                                            selectedTab = StudentBottomTab.Notice
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
                                onRouteClick = { routeId -> openRouteLive(routeId) },
                                onFavoriteClick = { },
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
