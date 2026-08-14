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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.onda.mju.student.core.calendar.AcademicCalendar
import com.onda.mju.student.data.mapper.buildStopGuideItems
import com.onda.mju.student.data.mapper.buildStopGuideRoutes
import com.onda.mju.student.data.mapper.buildTimetableRoutes
import com.onda.mju.student.data.mapper.toCommunityComment
import com.onda.mju.student.data.mapper.toCommunityPost
import com.onda.mju.student.data.mapper.toCommunityReport
import com.onda.mju.student.data.mapper.toInsertDto
import com.onda.mju.student.data.mapper.toNoticeItem
import com.onda.mju.student.data.mapper.toNotificationItem
import com.onda.mju.student.data.mapper.toPostInsertDto
import com.onda.mju.student.data.mapper.toPostUpdateDto
import com.onda.mju.student.data.mapper.toRouteUiModels
import com.onda.mju.student.data.mapper.toUpdateDto
import com.onda.mju.student.data.auth.SupabaseAuthRepository
import com.onda.mju.student.data.community.CommunityReadStore
import com.onda.mju.student.data.favorite.FavoritesStore
import com.onda.mju.student.data.notification.NoticeAlertReadStore
import com.onda.mju.student.data.remote.dto.OperationDeviceStatusDto
import com.onda.mju.student.data.remote.dto.OperationDto
import com.onda.mju.student.data.remote.dto.OperationStopProgressDto
import com.onda.mju.student.data.remote.dto.RouteDetailDto
import com.onda.mju.student.data.remote.dto.StopDto
import com.onda.mju.student.data.remote.dto.VehicleLocationDto
import com.onda.mju.student.data.remote.repository.FavoritesRepository
import com.onda.mju.student.data.remote.repository.NoticesRepository
import com.onda.mju.student.data.remote.repository.NotificationsRepository
import com.onda.mju.student.data.remote.repository.OperationDeviceStatusRepository
import com.onda.mju.student.data.remote.repository.OperationRepository
import com.onda.mju.student.data.remote.repository.OperationStopProgressRepository
import com.onda.mju.student.data.remote.repository.ReportCommentsRepository
import com.onda.mju.student.data.remote.repository.ReportReactionsRepository
import com.onda.mju.student.data.remote.repository.ReportViewsRepository
import com.onda.mju.student.data.remote.repository.ReportsRepository
import com.onda.mju.student.data.remote.repository.RouteRepository
import com.onda.mju.student.data.remote.repository.RouteStopsRepository
import com.onda.mju.student.data.remote.repository.ScheduleRepository
import com.onda.mju.student.data.remote.repository.StopRepository
import com.onda.mju.student.data.remote.repository.UserProfileRepository
import com.onda.mju.student.data.remote.repository.VehicleLocationRepository
import com.onda.mju.student.data.route.OperationalRouteResolver
import com.onda.mju.student.data.route.RouteStopCatalog
import com.onda.mju.student.data.route.StudentRouteIds
import com.onda.mju.student.ui.component.StudentBottomNavBar
import com.onda.mju.student.ui.component.StudentBottomTab
import com.onda.mju.student.ui.screen.community.CommunityCreateScreen
import com.onda.mju.student.ui.screen.community.CommunityDetailScreen
import com.onda.mju.student.ui.screen.community.CommunityListScreen
import com.onda.mju.student.ui.screen.community.CommunityComment
import com.onda.mju.student.ui.screen.community.CommunityPost
import com.onda.mju.student.ui.screen.community.CommunityPostCreateScreen
import com.onda.mju.student.ui.screen.community.CommunityPostDetailScreen
import com.onda.mju.student.ui.screen.community.ReportReaction
import com.onda.mju.student.ui.screen.community.sampleCommunityReports
import com.onda.mju.student.ui.screen.community.withAnonymousAuthorLabels
import com.onda.mju.student.ui.screen.community.withToggledReaction
import com.onda.mju.student.data.remote.dto.ReportCommentInsertDto
import com.onda.mju.student.ui.screen.favorite.FavoriteScreen
import com.onda.mju.student.ui.screen.favorite.buildFavoriteStops
import com.onda.mju.student.ui.screen.favorite.toFavoriteRoutes
import com.onda.mju.student.ui.screen.home.StudentHomeScreen
import com.onda.mju.student.ui.screen.legal.LegalDocumentScreen
import com.onda.mju.student.ui.screen.legal.LegalType
import com.onda.mju.student.ui.screen.my.AccountInfoScreen
import com.onda.mju.student.ui.screen.my.ChangePasswordScreen
import com.onda.mju.student.ui.screen.my.EditNameScreen
import com.onda.mju.student.ui.screen.my.FavoriteManageScreen
import com.onda.mju.student.ui.screen.my.LogoutConfirmScreen
import com.onda.mju.student.ui.screen.my.MyHomeScreen
import com.onda.mju.student.ui.screen.my.MyPostDetailScreen
import com.onda.mju.student.ui.screen.my.MyPostsScreen
import com.onda.mju.student.ui.screen.my.MyReportDetailScreen
import com.onda.mju.student.ui.screen.my.MyReportsScreen
import com.onda.mju.student.ui.screen.my.NotificationSettingsScreen
import com.onda.mju.student.ui.screen.notice.NoticeDetailScreen
import com.onda.mju.student.ui.screen.notice.NoticeItem
import com.onda.mju.student.ui.screen.notice.NoticeListScreen
import com.onda.mju.student.ui.screen.notice.StopGuideDetailScreen
import com.onda.mju.student.ui.screen.notice.StopGuideItem
import com.onda.mju.student.ui.screen.notice.StopGuideListScreen
import com.onda.mju.student.ui.screen.notice.StopGuideRouteInfo
import com.onda.mju.student.ui.screen.notice.StopGuideScreen
import com.onda.mju.student.ui.screen.notice.TimetableRoute
import com.onda.mju.student.ui.screen.notice.TimetableScreen
import com.onda.mju.student.ui.screen.notice.emptyStopGuideRoutes
import com.onda.mju.student.ui.screen.notice.emptyTimetableRoutes
import com.onda.mju.student.ui.screen.notification.NotificationDetailScreen
import com.onda.mju.student.ui.screen.notification.NotificationItem
import com.onda.mju.student.ui.screen.notification.NotificationListScreen
import com.onda.mju.student.ui.screen.notification.unreadNotificationCount as countUnreadNotifications
import com.onda.mju.student.ui.screen.route.BusDetailScreen
import com.onda.mju.student.ui.screen.route.buildBusDetailData
import com.onda.mju.student.ui.screen.route.LiveVehicle
import com.onda.mju.student.ui.screen.route.RouteListScreen
import com.onda.mju.student.ui.screen.route.RouteLiveScreen
import com.onda.mju.student.ui.screen.route.RouteUiModel
import com.onda.mju.student.ui.screen.route.StopCoordinateMap
import com.onda.mju.student.ui.screen.route.StopCoordinateResolver
import com.onda.mju.student.ui.screen.route.StopLiveScreen
import com.onda.mju.student.ui.screen.route.VehicleStatus
import com.onda.mju.student.ui.screen.route.liveStopsForDirection
import com.onda.mju.student.ui.screen.route.routeStopConfig
import com.onda.mju.student.ui.screen.route.sampleRouteList
import com.onda.mju.student.ui.screen.route.sampleRouteLive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface MainOverlay {
    data object None : MainOverlay
    data object Favorites : MainOverlay
    data object NotificationList : MainOverlay
    data class NotificationDetail(val id: String, val returnToList: Boolean = true) : MainOverlay

    data class CommunityDetail(val id: String) : MainOverlay
    data object CommunityCreate : MainOverlay
    data class CommunityPostDetail(val id: String) : MainOverlay
    data object CommunityPostCreate : MainOverlay
    data class CommunityPostEdit(val id: String) : MainOverlay

    data class NoticeDetail(val id: String) : MainOverlay
    data class Timetable(val forToday: Boolean = false) : MainOverlay
    data object StopGuide : MainOverlay
    data class StopGuideList(val routeId: String) : MainOverlay
    data class StopGuideDetail(val stopId: String, val returnRouteId: String) : MainOverlay

    data class RouteLive(val routeId: String) : MainOverlay
    data class StopLive(
        val stopId: String,
        val routeId: String? = null,
        val returnRouteId: String? = null,
    ) : MainOverlay
    data class BusDetail(
        val vehicleId: String,
        val returnRouteId: String? = null,
        val returnStopId: String? = null,
    ) : MainOverlay

    data object AccountInfo : MainOverlay
    data object EditName : MainOverlay
    data object ChangePassword : MainOverlay
    data object FavoriteManage : MainOverlay
    data object NotificationSettings : MainOverlay
    data object MyReports : MainOverlay
    data class MyReportDetail(val id: String) : MainOverlay
    data class MyReportEdit(val id: String) : MainOverlay
    data class CommunityReportEdit(val id: String) : MainOverlay
    data object MyPosts : MainOverlay
    data class MyPostDetail(val id: String) : MainOverlay
    data class MyPostEdit(val id: String) : MainOverlay
    data object LogoutConfirm : MainOverlay
    data class Legal(val type: LegalType) : MainOverlay
}

@Composable
fun StudentMainShell(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    val noticeAlertReadStore = remember { NoticeAlertReadStore(context) }
    val communityReadStore = remember { CommunityReadStore(context) }
    val favoritesStore = remember { FavoritesStore(context) }
    val favoritesRepository = remember { FavoritesRepository() }
    var favoriteRouteIds by remember { mutableStateOf(favoritesStore.routeIds()) }
    var favoriteStopIds by remember { mutableStateOf(favoritesStore.stopIds()) }
    var selectedTab by remember { mutableStateOf(StudentBottomTab.Home) }
    var overlay by remember { mutableStateOf<MainOverlay>(MainOverlay.None) }
    var personalNotifications by remember { mutableStateOf(emptyList<NotificationItem>()) }
    var readNoticeAlertIds by remember { mutableStateOf(noticeAlertReadStore.readIds()) }
    var notices by remember { mutableStateOf(emptyList<NoticeItem>()) }
    var noticesLoadError by remember { mutableStateOf<String?>(null) }
    var noticesLoaded by remember { mutableStateOf(false) }
    var communityReports by remember { mutableStateOf(emptyList<com.onda.mju.student.ui.screen.community.CommunityReport>()) }
    var communityPosts by remember { mutableStateOf(emptyList<CommunityPost>()) }
    var postComments by remember { mutableStateOf(emptyList<CommunityComment>()) }
    var myReportIds by remember { mutableStateOf(emptySet<String>()) }
    var myPostIds by remember { mutableStateOf(emptySet<String>()) }
    var readReportIds by remember { mutableStateOf(communityReadStore.readIds()) }
    var reportsLoadError by remember { mutableStateOf<String?>(null) }
    var reportsLoaded by remember { mutableStateOf(false) }
    val myReports = remember(communityReports, myReportIds) {
        communityReports.filter { it.id in myReportIds }
    }
    val myPosts = remember(communityPosts, myPostIds) {
        communityPosts.filter { it.id in myPostIds }
    }
    val latestHomeNotice = remember(notices) { notices.firstOrNull() }
    val notifications = remember(notices, personalNotifications, readNoticeAlertIds) {
        val fromNotices = notices.map { it.toNotificationItem(readNoticeAlertIds) }
        (fromNotices + personalNotifications)
            .sortedByDescending { it.sortInstantMillis }
    }
    // 종 뱃지용 — 읽음 시 즉시 감소 (목록 재조회와 별개로 반영)
    var unreadIds by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(notifications) {
        unreadIds = notifications.filter { it.initiallyUnread }.map { it.id }.toSet()
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
    val stopRepository = remember { StopRepository() }
    val routeRepository = remember { RouteRepository() }
    val routeStopsRepository = remember { RouteStopsRepository() }
    val scheduleRepository = remember { ScheduleRepository() }
    val vehicleLocationRepository = remember { VehicleLocationRepository() }
    val operationDeviceStatusRepository = remember { OperationDeviceStatusRepository() }
    val operationStopProgressRepository = remember { OperationStopProgressRepository() }
    val reportsRepository = remember { ReportsRepository() }
    val reportReactionsRepository = remember { ReportReactionsRepository() }
    val reportCommentsRepository = remember { ReportCommentsRepository() }
    val reportViewsRepository = remember { ReportViewsRepository() }
    val noticesRepository = remember { NoticesRepository() }
    val notificationsRepository = remember { NotificationsRepository() }
    val userProfileRepository = remember { UserProfileRepository() }
    val authRepository = remember { SupabaseAuthRepository() }
    var profileName by remember { mutableStateOf("학생") }
    var profileEmail by remember { mutableStateOf("") }

    suspend fun reloadProfile() {
        try {
            val profile = withContext(Dispatchers.IO) { userProfileRepository.getMine() }
            if (profile != null) {
                profileName = profile.name.ifBlank { "학생" }
                profileEmail = profile.email
                    ?.takeIf { it.isNotBlank() }
                    ?: authRepository.currentEmail().orEmpty()
            } else {
                profileEmail = authRepository.currentEmail().orEmpty()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ONDA_PROFILE", "profile load failed: ${e.message}")
            profileEmail = authRepository.currentEmail().orEmpty()
        }
    }

    suspend fun reloadPersonalNotifications() {
        try {
            val rows = withContext(Dispatchers.IO) { notificationsRepository.listMine() }
            personalNotifications = rows.map { it.toNotificationItem() }
            Log.d("ONDA_ALERTS", "loaded personal notifications=${personalNotifications.size}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ONDA_ALERTS", "personal notifications load failed: ${e.message}")
            // 테이블/RLS 미적용이어도 공지 기반 알림은 동작
        }
    }

    suspend fun reloadNotices(showErrorSnackbar: Boolean = false) {
        try {
            val rows = withContext(Dispatchers.IO) { noticesRepository.listForStudent() }
            notices = rows.map { it.toNoticeItem() }
            noticesLoadError = null
            noticesLoaded = true
            val activeAlertIds = notices.map { NoticeAlertReadStore.alertIdForNotice(it.id) }.toSet()
            noticeAlertReadStore.prune(activeAlertIds)
            // 로컬 읽음 + 방금 읽은 상태 병합 (폴링이 읽음을 되돌리지 않도록)
            readNoticeAlertIds = (noticeAlertReadStore.readIds() + readNoticeAlertIds)
                .filter { it in activeAlertIds }
                .toSet()
            noticeAlertReadStore.markAllRead(readNoticeAlertIds)
            // 상세 화면 보고 있는 공지가 종료/삭제되면 닫기
            val detail = overlay
            if (detail is MainOverlay.NoticeDetail && notices.none { it.id == detail.id }) {
                overlay = MainOverlay.None
            }
            if (detail is MainOverlay.NotificationDetail) {
                val stillThere = notices.any {
                    NoticeAlertReadStore.alertIdForNotice(it.id) == detail.id
                } || personalNotifications.any { it.id == detail.id }
                if (!stillThere && detail.id.startsWith(NoticeAlertReadStore.NOTICE_PREFIX)) {
                    overlay = if (detail.returnToList) MainOverlay.NotificationList else MainOverlay.None
                }
            }
            Log.d("ONDA_NOTICES", "loaded student notices=${notices.size}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ONDA_NOTICES", "load failed: ${e.message}", e)
            if (showErrorSnackbar || !noticesLoaded) {
                noticesLoadError = e.message ?: "공지 목록을 불러오지 못했습니다."
            }
            // Mock 폴백 제거 — DB만 사용 (실패 시 빈 목록 유지)
            if (!noticesLoaded) {
                notices = emptyList()
            }
        }
    }

    suspend fun reloadCommunityPosts() {
        try {
            val rows = withContext(Dispatchers.IO) { reportsRepository.listStudentPosts() }
            val uid = reportsRepository.currentUserId().orEmpty()
            val reactions = withContext(Dispatchers.IO) {
                reportReactionsRepository.listForReports(rows.map { it.id })
            }
            val comments = withContext(Dispatchers.IO) {
                runCatching { reportCommentsRepository.listForReports(rows.map { it.id }) }
                    .getOrDefault(emptyList())
            }
            val likesByReport = reactions
                .filter { it.reaction.equals("LIKE", ignoreCase = true) }
                .groupingBy { it.reportId }
                .eachCount()
            val dislikesByReport = reactions
                .filter { it.reaction.equals("DISLIKE", ignoreCase = true) }
                .groupingBy { it.reportId }
                .eachCount()
            val myReactionByReport = reactions
                .filter { it.userId == uid }
                .associate { it.reportId to ReportReaction.fromDb(it.reaction) }
            val commentCountByReport = comments.groupingBy { it.reportId }.eachCount()
            val mapped = rows.map { row ->
                row.toCommunityPost(
                    likeCount = likesByReport[row.id] ?: 0,
                    dislikeCount = dislikesByReport[row.id] ?: 0,
                    myReaction = myReactionByReport[row.id],
                    commentCount = commentCountByReport[row.id] ?: 0,
                )
            }
            communityPosts = mapped
            myPostIds = mapped.filter { it.userId.isNotBlank() && it.userId == uid }.map { it.id }.toSet()
            communityReadStore.markReadAll(myPostIds)
            readReportIds = communityReadStore.readIds()
            Log.d("ONDA_POSTS", "loaded community posts=${mapped.size}, mine=${myPostIds.size}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ONDA_POSTS", "load failed: ${e.message}", e)
            if (communityPosts.isEmpty()) {
                communityPosts = emptyList()
                myPostIds = emptySet()
            }
        }
    }

    suspend fun reloadPostComments(postId: String) {
        try {
            val uid = reportCommentsRepository.currentUserId()
            val authorId = communityPosts.firstOrNull { it.id == postId }?.userId
                ?: communityReports.firstOrNull { it.id == postId }?.userId
                .orEmpty()
            val rows = withContext(Dispatchers.IO) {
                reportCommentsRepository.listForReport(postId)
            }
            postComments = rows
                .map { it.toCommunityComment(uid) }
                .withAnonymousAuthorLabels(authorId)
            communityPosts = communityPosts.map { post ->
                if (post.id == postId) post.copy(commentCount = rows.size) else post
            }
            communityReports = communityReports.map { report ->
                if (report.id == postId) report.copy(commentCount = rows.size) else report
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ONDA_COMMENTS", "comments load failed: ${e.message}", e)
            postComments = emptyList()
        }
    }

    suspend fun reloadCommunityReports() {
        try {
            val rows = withContext(Dispatchers.IO) { reportsRepository.listStudentReports() }
            val uid = reportsRepository.currentUserId().orEmpty()
            val reportIds = rows.map { it.id }
            val reactions = withContext(Dispatchers.IO) {
                reportReactionsRepository.listForReports(reportIds)
            }
            val comments = withContext(Dispatchers.IO) {
                runCatching { reportCommentsRepository.listForReports(reportIds) }
                    .getOrDefault(emptyList())
            }
            val likesByReport = reactions
                .filter { it.reaction.equals("LIKE", ignoreCase = true) }
                .groupingBy { it.reportId }
                .eachCount()
            val dislikesByReport = reactions
                .filter { it.reaction.equals("DISLIKE", ignoreCase = true) }
                .groupingBy { it.reportId }
                .eachCount()
            val myReactionByReport = reactions
                .filter { it.userId == uid }
                .associate { it.reportId to ReportReaction.fromDb(it.reaction) }
            val commentCountByReport = comments.groupingBy { it.reportId }.eachCount()
            val mapped = rows.map { row ->
                row.toCommunityReport(
                    likeCount = likesByReport[row.id] ?: 0,
                    dislikeCount = dislikesByReport[row.id] ?: 0,
                    myReaction = myReactionByReport[row.id],
                    commentCount = commentCountByReport[row.id] ?: 0,
                )
            }
            communityReports = mapped
            myReportIds = mapped.filter { it.userId.isNotBlank() && it.userId == uid }.map { it.id }.toSet()
            // 내가 올린 제보는 읽음 처리
            communityReadStore.markReadAll(myReportIds)
            readReportIds = communityReadStore.readIds()
            reportsLoadError = null
            reportsLoaded = true
            Log.d("ONDA_REPORTS", "loaded student reports=${mapped.size}, mine=${myReportIds.size}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ONDA_REPORTS", "load failed: ${e.message}", e)
            reportsLoadError = e.message ?: "제보 목록을 불러오지 못했습니다."
            if (!reportsLoaded) {
                communityReports = sampleCommunityReports()
                myReportIds = emptySet()
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadCommunityReports()
        reloadCommunityPosts()
        reloadNotices(showErrorSnackbar = true)
        reloadPersonalNotifications()
        reloadProfile()
        // DB에 저장된 내 조회 기록 → 읽음 상태 복원
        val viewed = withContext(Dispatchers.IO) {
            runCatching { reportViewsRepository.listMyViewedReportIds() }.getOrDefault(emptySet())
        }
        if (viewed.isNotEmpty()) {
            communityReadStore.markReadAll(viewed)
            readReportIds = communityReadStore.readIds()
        }
        // 즐겨찾기: DB 성공 시에만 반영 (실패 시 로컬 유지)
        val remoteFavorites = withContext(Dispatchers.IO) {
            favoritesRepository.listMine()
        }
        if (remoteFavorites != null) {
            favoritesStore.setAll(remoteFavorites.routeIds, remoteFavorites.stopIds)
            favoriteRouteIds = remoteFavorites.routeIds
            favoriteStopIds = remoteFavorites.stopIds
        } else {
            favoriteRouteIds = favoritesStore.routeIds()
            favoriteStopIds = favoritesStore.stopIds()
        }
    }

    // Realtime: notices 변경 시 즉시 DB 재조회 (끊기면 repository 가 재연결)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                noticesRepository.observeChanges().collect {
                    reloadNotices()
                    // 공지 기반 알림 목록도 같이 갱신됨 (notices state 파생)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ONDA_NOTICES", "realtime collect ended, retry: ${e.message}")
                delay(3_000L)
            }
        }
    }

    // Realtime: 개인 notifications
    LaunchedEffect(Unit) {
        while (true) {
            try {
                notificationsRepository.observeChanges().collect {
                    reloadPersonalNotifications()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ONDA_ALERTS", "realtime collect ended, retry: ${e.message}")
                delay(3_000L)
            }
        }
    }

    // 폴링 백업: Realtime 미설정/RLS로 UPDATE 누락돼도 주기적으로 맞춤
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000L)
            reloadNotices()
            reloadPersonalNotifications()
        }
    }

    // 커뮤니티 좋아요 등: Realtime + 탭 진입/주기 재집계 (계정별 합산)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                reportReactionsRepository.observeChanges().collect {
                    Log.d("ONDA_REACTIONS", "realtime → reload community")
                    reloadCommunityReports()
                    reloadCommunityPosts()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ONDA_REACTIONS", "realtime retry: ${e.message}")
                delay(3_000L)
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == StudentBottomTab.Community) {
            reloadCommunityReports()
            reloadCommunityPosts()
        }
        if (selectedTab == StudentBottomTab.Notice) {
            reloadNotices()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != StudentBottomTab.Community) return@LaunchedEffect
        while (true) {
            delay(12_000L)
            reloadCommunityReports()
            reloadCommunityPosts()
        }
    }

    LaunchedEffect(overlay) {
        if (overlay is MainOverlay.NotificationList) {
            reloadNotices()
            reloadPersonalNotifications()
        }
    }

    LaunchedEffect(reportsLoadError) {
        val err = reportsLoadError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = "제보 DB 연동 실패: $err",
            duration = SnackbarDuration.Short,
        )
    }

    LaunchedEffect(noticesLoadError) {
        val err = noticesLoadError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = "공지 DB 연동 실패: $err",
            duration = SnackbarDuration.Short,
        )
    }

    var routes by remember { mutableStateOf<List<RouteUiModel>>(sampleRouteList()) }
    var routeDetails by remember { mutableStateOf<List<RouteDetailDto>>(emptyList()) }
    var allStops by remember { mutableStateOf<List<StopDto>>(emptyList()) }
    var stopCoordinates by remember { mutableStateOf<StopCoordinateMap>(emptyMap()) }
    var routeCatalogRevision by remember { mutableIntStateOf(0) }
    var timetableRoutes by remember { mutableStateOf<List<TimetableRoute>>(emptyTimetableRoutes()) }
    var stopGuideRoutes by remember { mutableStateOf<List<StopGuideRouteInfo>>(emptyStopGuideRoutes()) }
    var stopGuideItems by remember { mutableStateOf<List<StopGuideItem>>(emptyList()) }
    var operations by remember { mutableStateOf<List<OperationDto>>(emptyList()) }
    val operationLocations = remember {
        mutableStateMapOf<String, VehicleLocationDto>()
    }
    val operationDeviceStatuses = remember {
        mutableStateMapOf<String, OperationDeviceStatusDto>()
    }
    val operationStopProgress = remember {
        mutableStateMapOf<String, OperationStopProgressDto>()
    }

    // Temporary: verify Supabase operations read path + realtime status updates.
    LaunchedEffect(Unit) {
        val locationJobs = mutableMapOf<String, Job>()
        val deviceStatusJobs = mutableMapOf<String, Job>()
        val stopProgressJobs = mutableMapOf<String, Job>()

        suspend fun startLocationSubscription(operationId: String) {
            if (locationJobs.containsKey(operationId)) return

            Log.d("ONDA_SUPABASE", "start realtime jobs operationId=$operationId (vehicle_locations)")
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
            Log.d("ONDA_SUPABASE", "stop realtime jobs operationId=$operationId (vehicle_locations)")
            locationJobs.remove(operationId)?.cancel()
            operationLocations.remove(operationId)
            Log.d("ONDA_SUPABASE", "remove operation state operationId=$operationId (locations)")
        }

        suspend fun startDeviceStatusSubscription(operationId: String) {
            if (deviceStatusJobs.containsKey(operationId)) return

            Log.d("ONDA_SUPABASE", "start realtime jobs operationId=$operationId (device_status)")
            val initialStatus = operationDeviceStatusRepository.getStatus(operationId)
            if (initialStatus != null) {
                Log.d(
                    "ONDA_SUPABASE",
                    "device status initial operationId=${initialStatus.operationId}, " +
                        "gpsOk=${initialStatus.gpsOk}, gpsEnabled=${initialStatus.gpsEnabled}, " +
                        "updatedAt=${initialStatus.updatedAt}",
                )
                operationDeviceStatuses[operationId] = initialStatus
            } else {
                Log.d("ONDA_SUPABASE", "device status initial operationId=$operationId, status=null")
            }

            deviceStatusJobs[operationId] = launch {
                try {
                    operationDeviceStatusRepository.observeStatus(operationId).collect { status ->
                        Log.d(
                            "ONDA_SUPABASE",
                            "device status realtime operationId=${status.operationId}, " +
                                "gpsOk=${status.gpsOk}, gpsEnabled=${status.gpsEnabled}, " +
                                "updatedAt=${status.updatedAt}",
                        )
                        operationDeviceStatuses[operationId] = status
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        "ONDA_SUPABASE",
                        "device status observe failed operationId=$operationId " +
                            "(check supabase_realtime publication includes public.operation_device_status): ${e.message}",
                        e,
                    )
                }
            }
        }

        fun stopDeviceStatusSubscription(operationId: String) {
            Log.d("ONDA_SUPABASE", "stop realtime jobs operationId=$operationId (device_status)")
            deviceStatusJobs.remove(operationId)?.cancel()
            operationDeviceStatuses.remove(operationId)
            Log.d("ONDA_SUPABASE", "device status removed operationId=$operationId")
        }

        suspend fun startStopProgressSubscription(operationId: String) {
            if (stopProgressJobs.containsKey(operationId)) return

            Log.d("ONDA_SUPABASE", "start realtime jobs operationId=$operationId (stop_progress)")
            val initialProgress = operationStopProgressRepository.getProgress(operationId)
            if (initialProgress != null) {
                Log.d(
                    "ONDA_SUPABASE",
                    "stop progress initial operationId=${initialProgress.operationId}, " +
                        "arrived=${initialProgress.lastArrivedIndex}, passed=${initialProgress.lastPassedIndex}",
                )
                operationStopProgress[operationId] = initialProgress
            } else {
                Log.d("ONDA_SUPABASE", "stop progress initial operationId=$operationId, progress=null")
            }

            stopProgressJobs[operationId] = launch {
                try {
                    operationStopProgressRepository.observeProgress(operationId).collect { progress ->
                        Log.d(
                            "ONDA_SUPABASE",
                            "stop progress realtime operationId=${progress.operationId}, " +
                                "arrived=${progress.lastArrivedIndex}, passed=${progress.lastPassedIndex}",
                        )
                        operationStopProgress[operationId] = progress
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        "ONDA_SUPABASE",
                        "stop progress observe failed operationId=$operationId " +
                            "(check supabase_realtime publication includes public.operation_stop_progress): ${e.message}",
                        e,
                    )
                }
            }
        }

        fun stopStopProgressSubscription(operationId: String) {
            Log.d("ONDA_SUPABASE", "stop realtime jobs operationId=$operationId (stop_progress)")
            stopProgressJobs.remove(operationId)?.cancel()
            operationStopProgress.remove(operationId)
        }

        /**
         * Authoritative sync: subscribe exactly to current IN_PROGRESS operations.
         * Stops jobs for ended ops and starts jobs for newly started ops.
         */
        suspend fun reconcileInProgressSubscriptions(latest: List<OperationDto>) {
            val activeOps = latest.filter { it.status == "IN_PROGRESS" }
            val activeIds = activeOps.map { it.id }.toSet()
            val previousIds = (locationJobs.keys + deviceStatusJobs.keys + stopProgressJobs.keys).toSet()

            val removedIds = previousIds - activeIds
            val addedIds = activeIds - previousIds

            removedIds.forEach { operationId ->
                val routeName = latest.firstOrNull { it.id == operationId }
                    ?.schedule?.route?.routeName
                    ?: operations.firstOrNull { it.id == operationId }?.schedule?.route?.routeName
                Log.d(
                    "ONDA_SUPABASE",
                    "active operation removed id=$operationId, route=$routeName",
                )
                stopLocationSubscription(operationId)
                stopDeviceStatusSubscription(operationId)
                stopStopProgressSubscription(operationId)
            }

            addedIds.forEach { operationId ->
                val op = activeOps.first { it.id == operationId }
                Log.d(
                    "ONDA_SUPABASE",
                    "active operation added id=$operationId, route=${op.schedule?.route?.routeName}",
                )
                startLocationSubscription(operationId)
                startDeviceStatusSubscription(operationId)
                startStopProgressSubscription(operationId)
            }

            Log.d(
                "ONDA_SUPABASE",
                "current active operation ids=${activeIds.toList()}",
            )
        }

        try {
            try {
                val stops = stopRepository.getAllStops()
                allStops = stops
                stopCoordinates = StopCoordinateResolver.fromStops(stops)
                Log.d("ONDA_SUPABASE", "stops fetch success=true, count=${stops.size}")
            } catch (e: Exception) {
                Log.e("ONDA_SUPABASE", "stops fetch failed: ${e.message}", e)
            }

            var stopsByRouteName = emptyMap<String, List<com.onda.mju.student.data.remote.repository.RouteStopInfo>>()
            try {
                val fetchedRoutes = routeRepository.getActiveRoutes()
                routeDetails = fetchedRoutes
                stopGuideRoutes = buildStopGuideRoutes(fetchedRoutes).ifEmpty { emptyStopGuideRoutes() }
                Log.d("ONDA_SUPABASE", "routes fetch success=true, count=${fetchedRoutes.size}")
            } catch (e: Exception) {
                Log.e("ONDA_SUPABASE", "routes fetch failed: ${e.message}", e)
            }

            try {
                val routeStopRows = routeStopsRepository.getAllRouteStops()
                stopsByRouteName = routeStopRows.groupBy { it.routeName }
                RouteStopCatalog.update(stopsByRouteName, date = AcademicCalendar.todayDateKey())
                routeCatalogRevision = RouteStopCatalog.revision()
                stopGuideItems = buildStopGuideItems(allStops, routeStopRows)
                Log.d("ONDA_SUPABASE", "route_stops fetch success=true, count=${routeStopRows.size}")
            } catch (e: Exception) {
                Log.e("ONDA_SUPABASE", "route_stops fetch failed: ${e.message}", e)
            }

            try {
                val schedules = scheduleRepository.getSchedulesWithRoutes()
                timetableRoutes = buildTimetableRoutes(
                    schedules = schedules,
                    routes = routeDetails,
                    routeStops = stopsByRouteName,
                ).ifEmpty { emptyTimetableRoutes() }
                Log.d("ONDA_SUPABASE", "schedules fetch success=true, count=${schedules.size}")
            } catch (e: Exception) {
                Log.e("ONDA_SUPABASE", "schedules fetch failed: ${e.message}", e)
            }

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
            val routeUiModels = fetchedOperations.toRouteUiModels(routeDetails)
            routeUiModels.forEach { route ->
                Log.d(
                    "ONDA_SUPABASE",
                    "routeUi id=${route.id}, name=${route.name}, status=${route.status}, " +
                        "activeVehicleCount=${route.activeVehicleCount}, nextDeparture=${route.nextDeparture}",
                )
            }
            routes = routeUiModels
            operations = fetchedOperations

            fetchedOperations
                .filter { it.status == "IN_PROGRESS" }
                .forEach { operation ->
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

            reconcileInProgressSubscriptions(fetchedOperations)

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
                            "operation transition id=${record.id}, oldStatus=$oldStatus, newStatus=$newStatus",
                        )

                        // Merge immediately for the updated row (keep nested schedule/bus).
                        val merged = if (existing != null) {
                            operations.map { op ->
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
                        } else {
                            operations
                        }

                        // Authoritative refresh so missed A=COMPLETED / B=IN_PROGRESS
                        // transitions cannot leave stale local IN_PROGRESS state.
                        val refreshed = try {
                            operationRepository.getOperations()
                        } catch (e: Exception) {
                            Log.e(
                                "ONDA_SUPABASE",
                                "operations refresh after realtime failed, using merge: ${e.message}",
                                e,
                            )
                            merged
                        }

                        operations = refreshed
                        routes = refreshed.toRouteUiModels(routeDetails)
                        reconcileInProgressSubscriptions(refreshed)
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

    suspend fun saveFavorites(routeIds: Set<String>, stopIds: Set<String>) {
        favoritesStore.setAll(routeIds, stopIds)
        favoriteRouteIds = routeIds
        favoriteStopIds = stopIds
        val ok = withContext(Dispatchers.IO) {
            favoritesRepository.replaceAll(routeIds, stopIds)
        }
        if (!ok) {
            showTodo("로컬에 저장했습니다. DB 연동은 테이블 적용 후 다시 저장해 주세요.")
        }
    }

    val homeFavoriteRoutes = remember(routes, favoriteRouteIds) {
        val ordered = StudentRouteIds.routeListUiIds.filter { it in favoriteRouteIds } +
            favoriteRouteIds.filter { it !in StudentRouteIds.routeListUiIds.toSet() }
        ordered.mapNotNull { id -> routes.firstOrNull { it.id == id } }
    }

    fun markCommunityRead(id: String) {
        if (id.isBlank()) return
        communityReadStore.markRead(id)
        readReportIds = readReportIds + id
    }

    /** 상세 열람: 읽음 표시 + 고유 조회수 1회 기록 */
    suspend fun recordCommunityView(id: String) {
        markCommunityRead(id)
        val count = withContext(Dispatchers.IO) {
            reportViewsRepository.recordView(id)
        } ?: return
        communityReports = communityReports.map { report ->
            if (report.id == id) report.copy(viewCount = count) else report
        }
        communityPosts = communityPosts.map { post ->
            if (post.id == id) post.copy(viewCount = count) else post
        }
    }

    suspend fun syncReportReactionFromDb(reportId: String, uid: String) {
        val summary = withContext(Dispatchers.IO) {
            reportReactionsRepository.summaryForReport(reportId, uid)
        }
        communityReports = communityReports.map { report ->
            if (report.id != reportId) report
            else report.copy(
                likeCount = summary.likeCount,
                dislikeCount = summary.dislikeCount,
                myReaction = ReportReaction.fromDb(summary.myReaction),
            )
        }
        Log.d(
            "ONDA_REPORTS",
            "reaction sync report=$reportId likes=${summary.likeCount} dislikes=${summary.dislikeCount}",
        )
    }

    suspend fun syncPostReactionFromDb(postId: String, uid: String) {
        val summary = withContext(Dispatchers.IO) {
            reportReactionsRepository.summaryForReport(postId, uid)
        }
        communityPosts = communityPosts.map { post ->
            if (post.id != postId) post
            else post.copy(
                likeCount = summary.likeCount,
                dislikeCount = summary.dislikeCount,
                myReaction = ReportReaction.fromDb(summary.myReaction),
            )
        }
        Log.d(
            "ONDA_POSTS",
            "reaction sync post=$postId likes=${summary.likeCount} dislikes=${summary.dislikeCount}",
        )
    }

    fun toggleReportReaction(reportId: String, target: ReportReaction) {
        val current = communityReports.firstOrNull { it.id == reportId } ?: return
        val previous = current
        val optimistic = current.withToggledReaction(target)
        communityReports = communityReports.map { if (it.id == reportId) optimistic else it }
        scope.launch {
            val uid = reportReactionsRepository.currentUserId()
            if (uid.isNullOrBlank()) {
                communityReports = communityReports.map { if (it.id == reportId) previous else it }
                showTodo("로그인이 필요합니다. 다시 로그인해 주세요.")
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    reportReactionsRepository.setReaction(
                        reportId = reportId,
                        userId = uid,
                        targetReaction = optimistic.myReaction?.dbValue,
                    )
                }
                // DB 집계로 재동기화 (다른 계정 좋아요 포함)
                syncReportReactionFromDb(reportId, uid)
            } catch (e: Exception) {
                Log.e("ONDA_REPORTS", "reaction failed: ${e.message}", e)
                communityReports = communityReports.map { if (it.id == reportId) previous else it }
                showTodo("반응 저장 실패: ${e.message ?: "권한/네트워크 확인"}")
            }
        }
    }

    fun togglePostReaction(postId: String, target: ReportReaction) {
        val current = communityPosts.firstOrNull { it.id == postId } ?: return
        val previous = current
        val optimistic = current.withToggledReaction(target)
        communityPosts = communityPosts.map { if (it.id == postId) optimistic else it }
        scope.launch {
            val uid = reportReactionsRepository.currentUserId()
            if (uid.isNullOrBlank()) {
                communityPosts = communityPosts.map { if (it.id == postId) previous else it }
                showTodo("로그인이 필요합니다. 다시 로그인해 주세요.")
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    reportReactionsRepository.setReaction(
                        reportId = postId,
                        userId = uid,
                        targetReaction = optimistic.myReaction?.dbValue,
                    )
                }
                syncPostReactionFromDb(postId, uid)
            } catch (e: Exception) {
                Log.e("ONDA_POSTS", "reaction failed: ${e.message}", e)
                communityPosts = communityPosts.map { if (it.id == postId) previous else it }
                showTodo("반응 저장 실패: ${e.message ?: "권한/네트워크 확인"}")
            }
        }
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

    fun openNotificationDetail(id: String, returnToList: Boolean) {
        if (id.startsWith(NoticeAlertReadStore.NOTICE_PREFIX)) {
            noticeAlertReadStore.markRead(id)
            readNoticeAlertIds = readNoticeAlertIds + id
        } else {
            personalNotifications = personalNotifications.map {
                if (it.id == id) it.copy(initiallyUnread = false) else it
            }
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { notificationsRepository.markRead(id) }
                }.onFailure {
                    Log.w("ONDA_ALERTS", "markRead failed: ${it.message}")
                }
            }
        }
        unreadIds = unreadIds - id
        markAllDone = unreadIds.isEmpty()
        overlay = MainOverlay.NotificationDetail(id = id, returnToList = returnToList)
    }

    fun markAllRead() {
        val ids = notifications.map { it.id }
        noticeAlertReadStore.markAllRead(ids)
        readNoticeAlertIds = readNoticeAlertIds + ids.filter {
            it.startsWith(NoticeAlertReadStore.NOTICE_PREFIX)
        }
        personalNotifications = personalNotifications.map { it.copy(initiallyUnread = false) }
        unreadIds = emptySet()
        markAllDone = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { notificationsRepository.markAllRead() }
            }
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
        is MainOverlay.CommunityPostDetail,
        MainOverlay.CommunityPostCreate,
        is MainOverlay.CommunityPostEdit,
        is MainOverlay.CommunityReportEdit,
        -> StudentBottomTab.Community

        is MainOverlay.NoticeDetail,
        MainOverlay.StopGuide,
        is MainOverlay.StopGuideList,
        is MainOverlay.StopGuideDetail,
        -> StudentBottomTab.Notice

        is MainOverlay.Timetable -> if (timetableReturnRouteId != null) {
            StudentBottomTab.Route
        } else {
            selectedTab
        }

        is MainOverlay.RouteLive,
        is MainOverlay.StopLive,
        is MainOverlay.BusDetail,
        -> StudentBottomTab.Route

        MainOverlay.AccountInfo,
        MainOverlay.EditName,
        MainOverlay.ChangePassword,
        MainOverlay.FavoriteManage,
        MainOverlay.NotificationSettings,
        MainOverlay.MyReports,
        is MainOverlay.MyReportDetail,
        is MainOverlay.MyReportEdit,
        MainOverlay.MyPosts,
        is MainOverlay.MyPostDetail,
        is MainOverlay.MyPostEdit,
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
                        favoriteRoutes = routes.toFavoriteRoutes(favoriteRouteIds),
                        favoriteStops = buildFavoriteStops(stopGuideItems, favoriteStopIds),
                        onBackClick = { overlay = MainOverlay.None },
                        onManageClick = { overlay = MainOverlay.FavoriteManage },
                        onRouteClick = { routeId -> openRouteLive(routeId) },
                        onStopClick = { stopId ->
                            overlay = MainOverlay.StopLive(
                                stopId = stopId,
                                routeId = null,
                                returnRouteId = null,
                            )
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
                        onNotificationClick = { id -> openNotificationDetail(id, true) },
                    )
                }

                is MainOverlay.NotificationDetail -> {
                    val item = notifications.firstOrNull { it.id == current.id }
                    if (item == null) {
                        LaunchedEffect(current.id) {
                            overlay = if (current.returnToList) {
                                MainOverlay.NotificationList
                            } else {
                                MainOverlay.None
                            }
                        }
                    } else {
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
                            onRelatedMenuClick = {
                                if (current.id.startsWith(NoticeAlertReadStore.NOTICE_PREFIX)) {
                                    val noticeId = current.id.removePrefix(NoticeAlertReadStore.NOTICE_PREFIX)
                                    selectedTab = StudentBottomTab.Notice
                                    overlay = MainOverlay.NoticeDetail(noticeId)
                                } else {
                                    showTodo("관련 화면은 준비 중입니다.")
                                }
                            },
                        )
                    }
                }

                is MainOverlay.CommunityDetail -> {
                    val report = communityReports.firstOrNull { it.id == current.id }
                    if (report == null) {
                        LaunchedEffect(current.id) { overlay = MainOverlay.None }
                    } else {
                        LaunchedEffect(report.id) {
                            reloadPostComments(report.id)
                            recordCommunityView(report.id)
                        }
                        CommunityDetailScreen(
                            report = report,
                            comments = postComments,
                            modifier = Modifier.fillMaxSize(),
                            isMine = run {
                                val uid = reportsRepository.currentUserId()
                                report.id in myReportIds ||
                                    (!uid.isNullOrBlank() && report.userId.isNotBlank() && report.userId == uid)
                            },
                            onBackClick = {
                                postComments = emptyList()
                                overlay = MainOverlay.None
                            },
                            onLikeClick = { toggleReportReaction(report.id, ReportReaction.Like) },
                            onDislikeClick = { toggleReportReaction(report.id, ReportReaction.Dislike) },
                            onEditClick = { overlay = MainOverlay.CommunityReportEdit(report.id) },
                            onSubmitComment = { text ->
                                scope.launch {
                                    val uid = reportCommentsRepository.currentUserId()
                                    if (uid.isNullOrBlank()) {
                                        showTodo("로그인이 필요합니다. 다시 로그인해 주세요.")
                                        return@launch
                                    }
                                    try {
                                        withContext(Dispatchers.IO) {
                                            reportCommentsRepository.create(
                                                ReportCommentInsertDto(
                                                    reportId = report.id,
                                                    userId = uid,
                                                    content = text,
                                                ),
                                            )
                                        }
                                        reloadPostComments(report.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_REPORTS", "comment create failed: ${e.message}", e)
                                        showTodo("댓글 등록 실패: ${e.message ?: "권한/네트워크 확인"}")
                                    }
                                }
                            },
                            onDeleteComment = { commentId ->
                                scope.launch {
                                    postComments = postComments.map { c ->
                                        if (c.id == commentId) c.copy(isDeleted = true) else c
                                    }
                                    try {
                                        withContext(Dispatchers.IO) {
                                            reportCommentsRepository.softDelete(commentId)
                                        }
                                        reloadPostComments(report.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_REPORTS", "comment delete failed: ${e.message}", e)
                                        reloadPostComments(report.id)
                                        showTodo(
                                            "댓글 삭제 실패: ${e.message ?: "migrate_report_comments_soft_delete.sql 실행 여부 확인"}",
                                        )
                                    }
                                }
                            },
                            onEditComment = { commentId, text ->
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            reportCommentsRepository.update(commentId, text)
                                        }
                                        reloadPostComments(report.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_REPORTS", "comment update failed: ${e.message}", e)
                                        showTodo("댓글 수정 실패: ${e.message ?: "권한/네트워크 확인"}")
                                    }
                                }
                            },
                            onDeleteClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { reportsRepository.delete(report.id) }
                                        communityReports = communityReports.filterNot { it.id == report.id }
                                        myReportIds = myReportIds - report.id
                                        postComments = emptyList()
                                        showTodo("제보가 삭제되었습니다.")
                                        overlay = MainOverlay.None
                                    } catch (e: Exception) {
                                        Log.e("ONDA_REPORTS", "delete failed: ${e.message}", e)
                                        showTodo("제보 삭제 실패: ${e.message ?: "권한 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                MainOverlay.CommunityCreate -> {
                    CommunityCreateScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onNotificationClick = { openNotifications() },
                        onSubmit = { report ->
                            scope.launch {
                                val uid = reportsRepository.currentUserId()
                                if (uid.isNullOrBlank()) {
                                    showTodo("로그인이 필요합니다. 다시 로그인해 주세요.")
                                    return@launch
                                }
                                try {
                                    val saved = withContext(Dispatchers.IO) {
                                        reportsRepository.create(report.copy(userId = uid).toInsertDto(uid))
                                    }.toCommunityReport()
                                    communityReports = listOf(saved) + communityReports.filterNot { it.id == saved.id }
                                    myReportIds = myReportIds + saved.id
                                    recordCommunityView(saved.id)
                                    showTodo("제보가 등록되었습니다.")
                                    overlay = MainOverlay.None
                                    selectedTab = StudentBottomTab.Community
                                } catch (e: Exception) {
                                    Log.e("ONDA_REPORTS", "create failed: ${e.message}", e)
                                    showTodo("제보 등록 실패: ${e.message ?: "권한/네트워크 확인"}")
                                }
                            }
                        },
                    )
                }

                is MainOverlay.CommunityPostDetail -> {
                    val post = communityPosts.firstOrNull { it.id == current.id }
                    if (post == null) {
                        overlay = MainOverlay.None
                    } else {
                        LaunchedEffect(post.id) {
                            reloadPostComments(post.id)
                            recordCommunityView(post.id)
                        }
                        CommunityPostDetailScreen(
                            post = post,
                            comments = postComments,
                            modifier = Modifier.fillMaxSize(),
                            isMine = run {
                                val uid = reportsRepository.currentUserId()
                                post.id in myPostIds ||
                                    (!uid.isNullOrBlank() && post.userId.isNotBlank() && post.userId == uid)
                            },
                            onBackClick = {
                                postComments = emptyList()
                                overlay = MainOverlay.None
                            },
                            onEditClick = { overlay = MainOverlay.CommunityPostEdit(post.id) },
                            onLikeClick = { togglePostReaction(post.id, ReportReaction.Like) },
                            onDislikeClick = { togglePostReaction(post.id, ReportReaction.Dislike) },
                            onSubmitComment = { text ->
                                scope.launch {
                                    val uid = reportCommentsRepository.currentUserId()
                                    if (uid.isNullOrBlank()) {
                                        showTodo("로그인이 필요합니다. 다시 로그인해 주세요.")
                                        return@launch
                                    }
                                    try {
                                        withContext(Dispatchers.IO) {
                                            reportCommentsRepository.create(
                                                ReportCommentInsertDto(
                                                    reportId = post.id,
                                                    userId = uid,
                                                    content = text,
                                                ),
                                            )
                                        }
                                        reloadPostComments(post.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_POSTS", "comment create failed: ${e.message}", e)
                                        showTodo("댓글 등록 실패: ${e.message ?: "권한/네트워크 확인"}")
                                    }
                                }
                            },
                            onDeleteComment = { commentId ->
                                scope.launch {
                                    // 즉시 UI 반영 후 DB 저장
                                    postComments = postComments.map { c ->
                                        if (c.id == commentId) c.copy(isDeleted = true) else c
                                    }
                                    try {
                                        withContext(Dispatchers.IO) {
                                            reportCommentsRepository.softDelete(commentId)
                                        }
                                        reloadPostComments(post.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_POSTS", "comment delete failed: ${e.message}", e)
                                        reloadPostComments(post.id)
                                        showTodo(
                                            "댓글 삭제 실패: ${e.message ?: "migrate_report_comments_soft_delete.sql 실행 여부 확인"}",
                                        )
                                    }
                                }
                            },
                            onEditComment = { commentId, text ->
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            reportCommentsRepository.update(commentId, text)
                                        }
                                        reloadPostComments(post.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_POSTS", "comment update failed: ${e.message}", e)
                                        showTodo("댓글 수정 실패: ${e.message ?: "권한/네트워크 확인"}")
                                    }
                                }
                            },
                            onDeleteClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            reportsRepository.delete(post.id)
                                        }
                                        communityPosts = communityPosts.filterNot { it.id == post.id }
                                        myPostIds = myPostIds - post.id
                                        postComments = emptyList()
                                        showTodo("게시글이 삭제되었습니다.")
                                        overlay = MainOverlay.None
                                    } catch (e: Exception) {
                                        Log.e("ONDA_POSTS", "delete failed: ${e.message}", e)
                                        showTodo("삭제 실패: ${e.message ?: "권한/네트워크 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                MainOverlay.CommunityPostCreate -> {
                    CommunityPostCreateScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { overlay = MainOverlay.None },
                        onSubmit = { draft ->
                            scope.launch {
                                val uid = reportsRepository.currentUserId()
                                if (uid.isNullOrBlank()) {
                                    showTodo("로그인이 필요합니다. 다시 로그인해 주세요.")
                                    return@launch
                                }
                                try {
                                    val saved = withContext(Dispatchers.IO) {
                                        reportsRepository.create(draft.toPostInsertDto(uid))
                                    }.toCommunityPost()
                                    communityPosts = listOf(saved) + communityPosts.filterNot { it.id == saved.id }
                                    myPostIds = myPostIds + saved.id
                                    recordCommunityView(saved.id)
                                    showTodo("글이 등록되었습니다.")
                                    overlay = MainOverlay.None
                                    selectedTab = StudentBottomTab.Community
                                } catch (e: Exception) {
                                    Log.e("ONDA_POSTS", "create failed: ${e.message}", e)
                                    showTodo("글 등록 실패: ${e.message ?: "권한/네트워크 확인"}")
                                }
                            }
                        },
                    )
                }

                is MainOverlay.CommunityPostEdit -> {
                    val post = communityPosts.firstOrNull { it.id == current.id }
                    if (post == null) {
                        overlay = MainOverlay.None
                    } else {
                        CommunityPostCreateScreen(
                            modifier = Modifier.fillMaxSize(),
                            initialPost = post,
                            onBackClick = { overlay = MainOverlay.CommunityPostDetail(post.id) },
                            onSubmit = { updated ->
                                scope.launch {
                                    try {
                                        val previous = communityPosts.firstOrNull { it.id == updated.id }
                                        val saved = withContext(Dispatchers.IO) {
                                            reportsRepository.update(updated.id, updated.toPostUpdateDto())
                                        }.toCommunityPost(
                                            likeCount = previous?.likeCount ?: 0,
                                            dislikeCount = previous?.dislikeCount ?: 0,
                                            myReaction = previous?.myReaction,
                                            commentCount = previous?.commentCount ?: 0,
                                        )
                                        communityPosts = communityPosts.map {
                                            if (it.id == saved.id) saved else it
                                        }
                                        showTodo("글이 수정되었습니다.")
                                        overlay = MainOverlay.CommunityPostDetail(saved.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_POSTS", "update failed: ${e.message}", e)
                                        showTodo("수정 실패: ${e.message ?: "권한/네트워크 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                is MainOverlay.NoticeDetail -> {
                    val item = notices.firstOrNull { it.id == current.id }
                    if (item == null) {
                        LaunchedEffect(current.id) { overlay = MainOverlay.None }
                    } else {
                        LaunchedEffect(item.id) {
                            withContext(Dispatchers.IO) {
                                noticesRepository.incrementViewCount(item.id)
                            }
                        }
                        NoticeDetailScreen(
                            item = item,
                            modifier = Modifier.fillMaxSize(),
                            onBackClick = { overlay = MainOverlay.None },
                            onNotificationClick = { openNotifications() },
                            onAttachmentClick = { file ->
                                val url = file.url?.trim().orEmpty()
                                if (url.isBlank()) {
                                    showTodo("첨부파일 주소를 찾을 수 없습니다.")
                                } else {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                        )
                                    }.onFailure {
                                        Log.e("ONDA_NOTICE", "open attachment failed: ${it.message}", it)
                                        showTodo("첨부파일을 열 수 없습니다.")
                                    }
                                }
                            },
                        )
                    }
                }

                is MainOverlay.Timetable -> {
                    TimetableScreen(
                        modifier = Modifier.fillMaxSize(),
                        initialRouteId = timetableReturnRouteId ?: StudentRouteIds.GIHEUNG,
                        routes = timetableRoutes,
                        forToday = current.forToday,
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
                        routes = stopGuideRoutes,
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
                        routes = stopGuideRoutes,
                        stopsCatalog = stopGuideItems,
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
                        stopsCatalog = stopGuideItems,
                        onBackClick = {
                            overlay = MainOverlay.StopGuideList(current.returnRouteId)
                        },
                        onOpenMapClick = {
                            showTodo("지도 앱 연동은 준비 중입니다.")
                        },
                        onLiveClick = {
                            selectedTab = StudentBottomTab.Route
                            val routeId = StudentRouteIds.normalizeUiId(
                                when (current.returnRouteId) {
                                    "giheung" -> StudentRouteIds.GIHEUNG
                                    "myeongji" -> StudentRouteIds.MYEONGJI_STATION
                                    else -> StudentRouteIds.CITY_SHUTTLE
                                },
                            )
                            overlay = MainOverlay.StopLive(
                                stopId = current.stopId,
                                routeId = routeId,
                                returnRouteId = routeId,
                            )
                        },
                    )
                }

                is MainOverlay.RouteLive -> {
                    val stopConfig = routeStopConfig(current.routeId)
                    val baseLiveData = sampleRouteLive(current.routeId).copy(
                        directions = stopConfig.directions,
                        stops = liveStopsForDirection(stopConfig, directionIndex = 0),
                    )
                    val routeUi = routes.firstOrNull { it.id == current.routeId }
                    val routeName = routeUi?.name ?: routeDisplayNameForId(current.routeId)
                    val liveVehicles = liveVehiclesForRoute(
                        routeName = routeName,
                        operations = operations,
                        operationLocations = operationLocations,
                    )
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
                        deviceStatuses = operationDeviceStatuses,
                        stopProgress = operationStopProgress,
                        stopCoordinates = stopCoordinates,
                        routeCatalogRevision = routeCatalogRevision,
                        onBackClick = { overlay = MainOverlay.None },
                        onStopClick = { stopId ->
                            overlay = MainOverlay.StopLive(
                                stopId = stopId,
                                routeId = current.routeId,
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
                            overlay = MainOverlay.Timetable()
                        },
                    )
                }

                is MainOverlay.StopLive -> {
                    val routeId = current.routeId ?: current.returnRouteId
                    val routeName = routeId?.let { id ->
                        routes.firstOrNull { it.id == id }?.name ?: routeDisplayNameForId(id)
                    }
                    val liveVehicles = liveVehiclesForRoute(
                        routeName = routeName,
                        operations = operations,
                        operationLocations = operationLocations,
                    )
                    StopLiveScreen(
                        stopId = current.stopId,
                        routeId = routeId,
                        vehicles = liveVehicles,
                        stopProgress = operationStopProgress,
                        stopCoordinates = stopCoordinates,
                        routeCatalogRevision = routeCatalogRevision,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            overlay = current.returnRouteId?.let { MainOverlay.RouteLive(it) }
                                ?: MainOverlay.None
                        },
                        onVehicleClick = { vehicleId ->
                            overlay = MainOverlay.BusDetail(
                                vehicleId = vehicleId,
                                returnRouteId = current.returnRouteId ?: routeId,
                                returnStopId = current.stopId,
                            )
                        },
                    )
                }

                is MainOverlay.BusDetail -> {
                    val busDetail = buildBusDetailData(
                        operationId = current.vehicleId,
                        operations = operations,
                        location = operationLocations[current.vehicleId],
                        deviceStatus = operationDeviceStatuses[current.vehicleId],
                        stopProgress = operationStopProgress[current.vehicleId],
                        reports = communityReports,
                    )
                    BusDetailScreen(
                        data = busDetail,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            overlay = when {
                                current.returnStopId != null -> MainOverlay.StopLive(
                                    stopId = current.returnStopId,
                                    routeId = current.returnRouteId,
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
                        userName = profileName,
                        userEmail = profileEmail,
                        onBackClick = { overlay = MainOverlay.None },
                        onEditNameClick = { overlay = MainOverlay.EditName },
                        onChangePasswordClick = { overlay = MainOverlay.ChangePassword },
                        onLogoutClick = { overlay = MainOverlay.LogoutConfirm },
                        onDeleteAccountClick = { showTodo("회원 탈퇴는 준비 중입니다.") },
                    )
                }

                MainOverlay.EditName -> {
                    EditNameScreen(
                        modifier = Modifier.fillMaxSize(),
                        initialName = profileName,
                        onBackClick = { overlay = MainOverlay.AccountInfo },
                        onSave = { newName ->
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    userProfileRepository.updateName(newName)
                                }
                            }.fold(
                                onSuccess = { saved ->
                                    profileName = saved.name
                                    showTodo("이름이 변경되었습니다.")
                                    overlay = MainOverlay.AccountInfo
                                    Result.success(Unit)
                                },
                                onFailure = { e ->
                                    Log.e("ONDA_PROFILE", "name update failed: ${e.message}", e)
                                    Result.failure(e)
                                },
                            )
                        },
                    )
                }

                MainOverlay.ChangePassword -> {
                    ChangePasswordScreen(
                        modifier = Modifier.fillMaxSize(),
                        authRepository = authRepository,
                        onBackClick = { overlay = MainOverlay.AccountInfo },
                        onChanged = {
                            showTodo("비밀번호가 변경되었습니다.")
                            overlay = MainOverlay.AccountInfo
                        },
                    )
                }

                MainOverlay.FavoriteManage -> {
                    FavoriteManageScreen(
                        modifier = Modifier.fillMaxSize(),
                        routeItems = routes.map { it.id to it.name },
                        favoriteRouteIds = favoriteRouteIds,
                        onBackClick = { overlay = MainOverlay.None },
                        onSaveClick = { routeIds ->
                            scope.launch {
                                saveFavorites(routeIds, emptySet())
                                showTodo("즐겨찾기가 저장되었습니다.")
                                overlay = MainOverlay.None
                            }
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
                    if (report == null) {
                        LaunchedEffect(current.id) { overlay = MainOverlay.MyReports }
                    } else {
                        LaunchedEffect(report.id) {
                            recordCommunityView(report.id)
                        }
                        MyReportDetailScreen(
                            report = report,
                            modifier = Modifier.fillMaxSize(),
                            onBackClick = { overlay = MainOverlay.MyReports },
                            onEditClick = { overlay = MainOverlay.MyReportEdit(report.id) },
                            onDeleteClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { reportsRepository.delete(report.id) }
                                        communityReports = communityReports.filterNot { it.id == report.id }
                                        myReportIds = myReportIds - report.id
                                        showTodo("제보가 삭제되었습니다.")
                                        overlay = MainOverlay.MyReports
                                    } catch (e: Exception) {
                                        Log.e("ONDA_REPORTS", "delete failed: ${e.message}", e)
                                        showTodo("제보 삭제 실패: ${e.message ?: "권한 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                is MainOverlay.MyReportEdit -> {
                    val report = communityReports.firstOrNull { it.id == current.id }
                    if (report == null) {
                        LaunchedEffect(current.id) { overlay = MainOverlay.MyReports }
                    } else {
                        CommunityCreateScreen(
                            modifier = Modifier.fillMaxSize(),
                            initialReport = report,
                            onBackClick = { overlay = MainOverlay.MyReportDetail(report.id) },
                            onSubmit = { updated ->
                                scope.launch {
                                    try {
                                        val saved = withContext(Dispatchers.IO) {
                                            reportsRepository.update(updated.id, updated.toUpdateDto())
                                        }.toCommunityReport()
                                        communityReports = communityReports.map {
                                            if (it.id == saved.id) saved else it
                                        }
                                        showTodo("제보가 수정되었습니다.")
                                        overlay = MainOverlay.MyReportDetail(saved.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_REPORTS", "update failed: ${e.message}", e)
                                        showTodo("제보 수정 실패: ${e.message ?: "권한 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                is MainOverlay.CommunityReportEdit -> {
                    val report = communityReports.firstOrNull { it.id == current.id }
                    if (report == null) {
                        LaunchedEffect(current.id) { overlay = MainOverlay.None }
                    } else {
                        CommunityCreateScreen(
                            modifier = Modifier.fillMaxSize(),
                            initialReport = report,
                            onBackClick = { overlay = MainOverlay.CommunityDetail(report.id) },
                            onSubmit = { updated ->
                                scope.launch {
                                    try {
                                        val saved = withContext(Dispatchers.IO) {
                                            reportsRepository.update(updated.id, updated.toUpdateDto())
                                        }.toCommunityReport()
                                        communityReports = communityReports.map {
                                            if (it.id == saved.id) saved else it
                                        }
                                        showTodo("제보가 수정되었습니다.")
                                        overlay = MainOverlay.CommunityDetail(saved.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_REPORTS", "update failed: ${e.message}", e)
                                        showTodo("제보 수정 실패: ${e.message ?: "권한 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                MainOverlay.MyPosts -> {
                    MyPostsScreen(
                        modifier = Modifier.fillMaxSize(),
                        posts = myPosts,
                        onBackClick = { overlay = MainOverlay.None },
                        onPostClick = { id ->
                            overlay = MainOverlay.MyPostDetail(id)
                        },
                    )
                }

                is MainOverlay.MyPostDetail -> {
                    val post = communityPosts.firstOrNull { it.id == current.id }
                    if (post == null) {
                        LaunchedEffect(current.id) { overlay = MainOverlay.MyPosts }
                    } else {
                        MyPostDetailScreen(
                            post = post,
                            modifier = Modifier.fillMaxSize(),
                            onBackClick = { overlay = MainOverlay.MyPosts },
                            onEditClick = { overlay = MainOverlay.MyPostEdit(post.id) },
                            onDeleteClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { reportsRepository.delete(post.id) }
                                        communityPosts = communityPosts.filterNot { it.id == post.id }
                                        myPostIds = myPostIds - post.id
                                        showTodo("글이 삭제되었습니다.")
                                        overlay = MainOverlay.MyPosts
                                    } catch (e: Exception) {
                                        Log.e("ONDA_POSTS", "delete failed: ${e.message}", e)
                                        showTodo("글 삭제 실패: ${e.message ?: "권한 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                is MainOverlay.MyPostEdit -> {
                    val post = communityPosts.firstOrNull { it.id == current.id }
                    if (post == null) {
                        LaunchedEffect(current.id) { overlay = MainOverlay.MyPosts }
                    } else {
                        CommunityPostCreateScreen(
                            modifier = Modifier.fillMaxSize(),
                            initialPost = post,
                            onBackClick = { overlay = MainOverlay.MyPostDetail(post.id) },
                            onSubmit = { updated ->
                                scope.launch {
                                    try {
                                        val previous = communityPosts.firstOrNull { it.id == updated.id }
                                        withContext(Dispatchers.IO) {
                                            reportsRepository.update(updated.id, updated.toPostUpdateDto())
                                        }
                                        val saved = updated.copy(
                                            likeCount = previous?.likeCount ?: updated.likeCount,
                                            dislikeCount = previous?.dislikeCount ?: updated.dislikeCount,
                                            myReaction = previous?.myReaction,
                                            commentCount = previous?.commentCount ?: updated.commentCount,
                                        )
                                        communityPosts = communityPosts.map {
                                            if (it.id == saved.id) saved else it
                                        }
                                        showTodo("글이 수정되었습니다.")
                                        overlay = MainOverlay.MyPostDetail(saved.id)
                                    } catch (e: Exception) {
                                        Log.e("ONDA_POSTS", "update failed: ${e.message}", e)
                                        showTodo("글 수정 실패: ${e.message ?: "권한 확인"}")
                                    }
                                }
                            },
                        )
                    }
                }

                MainOverlay.LogoutConfirm -> {
                    LogoutConfirmScreen(
                        modifier = Modifier.fillMaxSize(),
                        userName = profileName,
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
                                scheduleKindLabel = AcademicCalendar.todayScheduleKindLabel(),
                                routes = routes,
                                favoriteRoutes = homeFavoriteRoutes,
                                onNotificationClick = { openNotifications() },
                                onNoticeBannerClick = {
                                    val notice = latestHomeNotice
                                    if (notice != null) {
                                        overlay = MainOverlay.NoticeDetail(notice.id)
                                    } else {
                                        showTodo("등록된 공지가 없습니다.")
                                    }
                                },
                                onFavoriteManageClick = { overlay = MainOverlay.FavoriteManage },
                                onFavoriteClick = { routeId -> openRouteLive(routeId) },
                                onRouteShortcutClick = { routeId -> openRouteLive(routeId) },
                                onQuickActionClick = { action ->
                                    when (action) {
                                        "간편 제보" -> {
                                            selectedTab = StudentBottomTab.Community
                                            overlay = MainOverlay.CommunityCreate
                                        }
                                        "글쓰기" -> {
                                            selectedTab = StudentBottomTab.Community
                                            overlay = MainOverlay.CommunityPostCreate
                                        }
                                        "오늘 시간표", "전체 시간표" -> {
                                            timetableReturnRouteId = null
                                            overlay = MainOverlay.Timetable(
                                                forToday = action == "오늘 시간표",
                                            )
                                        }
                                        "정류장 안내" -> {
                                            selectedTab = StudentBottomTab.Notice
                                            overlay = MainOverlay.StopGuide
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
                            )
                        }

                        StudentBottomTab.Community -> {
                            CommunityListScreen(
                                modifier = Modifier.fillMaxSize(),
                                reports = communityReports,
                                posts = communityPosts,
                                readIds = readReportIds,
                                onReportClick = { id ->
                                    markCommunityRead(id)
                                    overlay = MainOverlay.CommunityDetail(id)
                                },
                                onPostClick = { id ->
                                    markCommunityRead(id)
                                    overlay = MainOverlay.CommunityPostDetail(id)
                                },
                                onCreateReportClick = {
                                    overlay = MainOverlay.CommunityCreate
                                },
                                onCreatePostClick = {
                                    overlay = MainOverlay.CommunityPostCreate
                                },
                                onLikeClick = { id ->
                                    toggleReportReaction(id, ReportReaction.Like)
                                },
                                onDislikeClick = { id ->
                                    toggleReportReaction(id, ReportReaction.Dislike)
                                },
                                onPostLikeClick = { id ->
                                    togglePostReaction(id, ReportReaction.Like)
                                },
                                onPostDislikeClick = { id ->
                                    togglePostReaction(id, ReportReaction.Dislike)
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
                                onTimetableClick = { overlay = MainOverlay.Timetable() },
                                onStopGuideClick = { overlay = MainOverlay.StopGuide },
                            )
                        }

                        StudentBottomTab.My -> {
                            MyHomeScreen(
                                modifier = Modifier.fillMaxSize(),
                                userName = profileName,
                                userEmail = profileEmail.ifBlank { "로그인 계정" },
                                onAccountClick = { overlay = MainOverlay.AccountInfo },
                                onFavoriteManageClick = { overlay = MainOverlay.FavoriteManage },
                                onNotificationSettingClick = {
                                    overlay = MainOverlay.NotificationSettings
                                },
                                onMyReportsClick = { overlay = MainOverlay.MyReports },
                                onMyPostsClick = { overlay = MainOverlay.MyPosts },
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

/** Matches StudentRouteIds route id ↔ DB route_name mapping. */
private fun routeDisplayNameForId(routeId: String): String =
    StudentRouteIds.displayName(routeId)

private fun liveVehiclesForRoute(
    routeName: String?,
    operations: List<OperationDto>,
    operationLocations: Map<String, VehicleLocationDto>,
): List<LiveVehicle> {
    // 시내 평일 / 주말·방학은 서로 다른 노선으로 매칭
    val target = routeName
        ?.takeIf { it.isNotBlank() }
        ?.let { OperationalRouteResolver.canonicalRouteName(it) }
    return operations
        .asSequence()
        .filter { it.status == "IN_PROGRESS" }
        .filter {
            target == null ||
                OperationalRouteResolver.canonicalRouteName(
                    it.schedule?.route?.routeName.orEmpty(),
                ) == target
        }
        .sortedByDescending { it.startedAt.orEmpty() }
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
                scheduledDepartureTime = operation.schedule?.departureTime,
                actualStartedAt = operation.startedAt,
            )
        }
        .toList()
}
