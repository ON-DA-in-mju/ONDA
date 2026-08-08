package com.mju.onda.driver.core.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mju.onda.driver.feature.adminforceend.ui.AdminForceEndScreen
import com.mju.onda.driver.feature.alarm.ui.OperationAlarmListScreen
import com.mju.onda.driver.feature.assignment.ui.AssignmentChangeScreen
import com.mju.onda.driver.feature.auth.data.SessionStateHolder
import com.mju.onda.driver.feature.backgroundguide.ui.BackgroundGuideScreen
import com.mju.onda.driver.feature.batterywarning.ui.BatteryWarningScreen
import com.mju.onda.driver.feature.auth.ui.LoginScreen
import com.mju.onda.driver.feature.cancel.ui.OperationCancelScreen
import com.mju.onda.driver.feature.consent.ui.LocationConsentScreen
import com.mju.onda.driver.feature.departure.ui.DepartureTimeChangeScreen
import com.mju.onda.driver.feature.endcomplete.ui.EndCompleteScreen
import com.mju.onda.driver.feature.endconfirm.ui.EndOperationConfirmScreen
import com.mju.onda.driver.feature.endprocessing.ui.EndProcessingScreen
import com.mju.onda.driver.feature.endtimeelapsed.ui.EndTimeElapsedScreen
import com.mju.onda.driver.feature.history.ui.HistoryDetailScreen
import com.mju.onda.driver.feature.history.ui.OperationHistoryScreen
import com.mju.onda.driver.feature.home.data.OperationRuntimeStateHolder
import com.mju.onda.driver.feature.home.ui.TodayOperationHomeScreen
import com.mju.onda.driver.feature.inoperation.ui.InOperationDetailStatusScreen
import com.mju.onda.driver.feature.inoperation.ui.InOperationMinimalScreen
import com.mju.onda.driver.feature.operation.ui.OperationDetailScreen
import com.mju.onda.driver.feature.permission.ui.PermissionCompleteScreen
import com.mju.onda.driver.feature.permission.ui.PermissionGuideScreen
import com.mju.onda.driver.feature.precheck.ui.PreCheckCompleteScreen
import com.mju.onda.driver.feature.precheck.ui.PreOperationCheckScreen
import com.mju.onda.driver.feature.recovery.ui.OperationRecoveryScreen
import com.mju.onda.driver.feature.settings.ui.AccountEditScreen
import com.mju.onda.driver.feature.settings.ui.AlarmSettingsScreen
import com.mju.onda.driver.feature.settings.ui.DevicePermissionScreen
import com.mju.onda.driver.feature.settings.ui.ContactAdminScreen
import com.mju.onda.driver.feature.settings.ui.LocationConsentManageScreen
import com.mju.onda.driver.feature.settings.ui.ContinueOperationScreen
import com.mju.onda.driver.feature.settings.ui.InterruptedEndCompleteScreen
import com.mju.onda.driver.feature.settings.ui.SafeStopConfirmScreen
import com.mju.onda.driver.feature.settings.ui.SafeStopHistoryScreen
import com.mju.onda.driver.feature.settings.ui.StopApprovedScreen
import com.mju.onda.driver.feature.settings.ui.StopReasonSelectScreen
import com.mju.onda.driver.feature.settings.ui.StopRequestConfirmScreen
import com.mju.onda.driver.feature.settings.ui.StopRequestDetailScreen
import com.mju.onda.driver.feature.settings.ui.StopRequestReceivedScreen
import com.mju.onda.driver.feature.settings.data.MockInterruptedEndComplete
import com.mju.onda.driver.feature.settings.ui.AccountInfoScreen
import com.mju.onda.driver.feature.settings.ui.DriverSettingsScreen
import com.mju.onda.driver.feature.settings.ui.LogoutConfirmScreen
import com.mju.onda.driver.feature.settings.ui.LogoutRestrictedScreen
import com.mju.onda.driver.feature.splash.ui.SplashScreen
import com.mju.onda.driver.feature.startcomplete.ui.StartCompleteScreen
import com.mju.onda.driver.feature.startconfirm.ui.StartConfirmScreen
import com.mju.onda.driver.feature.startprocessing.ui.StartProcessingScreen
import com.mju.onda.driver.feature.vehicle.ui.VehicleChangeScreen

/** 메인은 항상 오늘의 운행 홈 (운행 복구 화면은 연결 해제) */
private fun NavHostController.navigateToDriverHome(clearLoginStack: Boolean = true) {
    navigate(Routes.TODAY_OPERATION) {
        if (clearLoginStack) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToSettings() {
    navigate(Routes.DRIVER_SETTINGS) {
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToLoginAfterLogout() {
    navigate(Routes.LOGIN) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun OndaNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    when {
                        !SessionStateHolder.isLoggedIn -> {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                        SessionStateHolder.onboardingDone -> {
                            navController.navigate(Routes.TODAY_OPERATION) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                        else -> {
                            navController.navigate(Routes.LOCATION_CONSENT) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // 초기화 후 재로그인: 온보딩부터 / 이미 온보딩 완료면 홈
                    if (SessionStateHolder.onboardingDone) {
                        navController.navigateToDriverHome()
                    } else {
                        navController.navigate(Routes.LOCATION_CONSENT) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Routes.LOCATION_CONSENT) {
            LocationConsentScreen(
                onAgree = {
                    navController.navigate(Routes.PERMISSION_GUIDE)
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOCATION_CONSENT) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.PERMISSION_GUIDE) {
            PermissionGuideScreen(
                onContinue = {
                    navController.navigate(Routes.PERMISSION_COMPLETE)
                },
                onSkipToHome = {
                    navController.navigateToDriverHome()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PERMISSION_COMPLETE) {
            PermissionCompleteScreen(
                onGoToOperation = {
                    navController.navigateToDriverHome()
                },
            )
        }

        composable(Routes.TODAY_OPERATION) {
            TodayOperationHomeScreen(
                onOpenAlarms = {
                    navController.navigate(Routes.OPERATION_ALARMS)
                },
                onOpenOperationDetail = { operationId ->
                    navController.navigate(Routes.operationDetail(operationId))
                },
                onOpenInOperation = { _ ->
                    // 운행 중 배차 → 운행 복구 (DRI-01-03F)
                    navController.navigate(Routes.OPERATION_RECOVERY)
                },
                onOpenHistory = {
                    navController.navigate(Routes.OPERATION_HISTORY) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = { navController.navigateToSettings() },
                onResetToLogin = {
                    navController.navigateToLoginAfterLogout()
                },
            )
        }

        composable(Routes.OPERATION_HISTORY) {
            OperationHistoryScreen(
                onGoToToday = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenDetail = { recordId ->
                    navController.navigate(Routes.operationHistoryDetail(recordId))
                },
                onOpenSettings = { navController.navigateToSettings() },
            )
        }

        composable(Routes.DRIVER_SETTINGS) {
            DriverSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenLogoutConfirm = {
                    navController.navigate(Routes.LOGOUT_CONFIRM)
                },
                onOpenLogoutRestricted = {
                    navController.navigate(Routes.LOGOUT_RESTRICTED)
                },
                onOpenAccountInfo = {
                    navController.navigate(Routes.ACCOUNT_INFO)
                },
                onOpenDevicePermission = {
                    navController.navigate(Routes.DEVICE_PERMISSION)
                },
                onOpenAlarmSettings = {
                    navController.navigate(Routes.ALARM_SETTINGS)
                },
                onOpenLocationConsentManage = {
                    navController.navigate(Routes.LOCATION_CONSENT_MANAGE)
                },
                onOpenContactAdmin = {
                    navController.navigate(Routes.CONTACT_ADMIN)
                },
                onOpenSafeStopHistory = {
                    navController.navigate(Routes.SAFE_STOP_HISTORY)
                },
            )
        }

        composable(Routes.LOGOUT_CONFIRM) {
            LogoutConfirmScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = { navController.navigateToLoginAfterLogout() },
            )
        }

        composable(Routes.LOGOUT_RESTRICTED) {
            LogoutRestrictedScreen(
                onBack = { navController.popBackStack() },
                onGoToOperation = { operationId ->
                    navController.navigate(Routes.inOperationMinimal(operationId)) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onContactAdmin = {
                    navController.navigate(Routes.CONTACT_ADMIN)
                },
            )
        }

        composable(Routes.SAFE_STOP_HISTORY) {
            SafeStopHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenNewRequest = {
                    navController.navigate(Routes.SAFE_STOP_CONFIRM)
                },
                onOpenReceived = {
                    navController.navigate(Routes.STOP_REQUEST_RECEIVED)
                },
                onOpenApproved = {
                    navController.navigate(Routes.STOP_APPROVED)
                },
                onOpenContinue = {
                    navController.navigate(Routes.CONTINUE_OPERATION)
                },
            )
        }

        composable(Routes.ACCOUNT_INFO) {
            AccountInfoScreen(
                onBack = { navController.popBackStack() },
                onGoToSettings = {
                    if (!navController.popBackStack(Routes.DRIVER_SETTINGS, inclusive = false)) {
                        navController.navigate(Routes.DRIVER_SETTINGS) {
                            launchSingleTop = true
                        }
                    }
                },
                onOpenEdit = {
                    navController.navigate(Routes.ACCOUNT_EDIT)
                },
            )
        }

        composable(Routes.ACCOUNT_EDIT) {
            AccountEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(Routes.DEVICE_PERMISSION) {
            DevicePermissionScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ALARM_SETTINGS) {
            AlarmSettingsScreen(
                onBack = { navController.popBackStack() },
                onGoToSettings = {
                    if (!navController.popBackStack(Routes.DRIVER_SETTINGS, inclusive = false)) {
                        navController.navigate(Routes.DRIVER_SETTINGS) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Routes.LOCATION_CONSENT_MANAGE) {
            LocationConsentManageScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.CONTACT_ADMIN) {
            ContactAdminScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SAFE_STOP_CONFIRM) {
            SafeStopConfirmScreen(
                onBack = { navController.popBackStack() },
                onProceedStopOperation = { _ ->
                    navController.navigate(Routes.STOP_REASON_SELECT)
                },
                onReturnToOperation = { operationId ->
                    if (operationId != null) {
                        navController.navigate(Routes.inOperationMinimal(operationId)) {
                            popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Routes.TODAY_OPERATION) {
                            popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Routes.STOP_REASON_SELECT) {
            StopReasonSelectScreen(
                onBack = { navController.popBackStack() },
                onNext = { reason ->
                    navController.navigate(Routes.stopRequestDetail(reason))
                },
            )
        }

        composable(
            route = Routes.STOP_REQUEST_DETAIL,
            arguments = listOf(
                navArgument("reason") { type = NavType.StringType },
            ),
        ) { entry ->
            val reason = entry.arguments?.getString("reason").orEmpty()
            StopRequestDetailScreen(
                selectedReason = reason,
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.navigate(Routes.STOP_REQUEST_CONFIRM)
                },
            )
        }

        composable(Routes.STOP_REQUEST_CONFIRM) {
            StopRequestConfirmScreen(
                onBack = {
                    // 취소/뒤로가기 → 중단 요청 상세로 복귀
                    if (!navController.popBackStack(Routes.STOP_REQUEST_DETAIL, inclusive = false)) {
                        navController.popBackStack()
                    }
                },
                onSent = {
                    navController.navigate(Routes.STOP_REQUEST_RECEIVED) {
                        popUpTo(Routes.STOP_REQUEST_CONFIRM) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.STOP_REQUEST_RECEIVED) {
            StopRequestReceivedScreen(
                onBack = {
                    if (!navController.popBackStack(Routes.SAFE_STOP_HISTORY, inclusive = false)) {
                        navController.navigate(Routes.SAFE_STOP_HISTORY) {
                            launchSingleTop = true
                        }
                    }
                },
                onGoToList = {
                    if (!navController.popBackStack(Routes.SAFE_STOP_HISTORY, inclusive = false)) {
                        navController.navigate(Routes.SAFE_STOP_HISTORY) {
                            launchSingleTop = true
                        }
                    }
                },
                onContactAdmin = {
                    navController.navigate(Routes.CONTACT_ADMIN)
                },
            )
        }

        composable(Routes.STOP_APPROVED) {
            StopApprovedScreen(
                onBack = { navController.popBackStack() },
                onEndOperation = {
                    navController.navigate(Routes.INTERRUPTED_END_PROCESSING)
                },
                onContactAdmin = {
                    navController.navigate(Routes.CONTACT_ADMIN)
                },
            )
        }

        composable(Routes.INTERRUPTED_END_PROCESSING) {
            EndProcessingScreen(
                screenTitle = MockInterruptedEndComplete.PROCESSING_TITLE,
                headline = MockInterruptedEndComplete.PROCESSING_HEADLINE,
                onFinished = {
                    navController.navigate(Routes.INTERRUPTED_END_COMPLETE) {
                        popUpTo(Routes.INTERRUPTED_END_PROCESSING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.INTERRUPTED_END_COMPLETE) {
            InterruptedEndCompleteScreen(
                onBack = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onGoToToday = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenHistory = {
                    navController.navigate(Routes.OPERATION_HISTORY) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.CONTINUE_OPERATION) {
            ContinueOperationScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    val operationId = OperationRuntimeStateHolder.activeOperationId()
                    if (operationId != null) {
                        navController.navigate(Routes.inOperationMinimal(operationId)) {
                            popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Routes.TODAY_OPERATION) {
                            popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onContactAdmin = {
                    navController.navigate(Routes.CONTACT_ADMIN)
                },
            )
        }

        composable(
            route = Routes.OPERATION_HISTORY_DETAIL,
            arguments = listOf(
                navArgument("recordId") { type = NavType.StringType },
            ),
        ) {
            HistoryDetailScreen(
                onBack = { navController.popBackStack() },
                onGoToTodayHome = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.OPERATION_DETAIL,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            OperationDetailScreen(
                operationId = operationId,
                onBack = { navController.popBackStack() },
                onGoHome = {
                    navController.popBackStack(Routes.TODAY_OPERATION, inclusive = false)
                },
                onOpenPreCheck = { id ->
                    OperationRuntimeStateHolder.setPendingStart(id)
                    navController.navigate(Routes.PRE_OPERATION_CHECK)
                },
                onOpenHistory = {
                    navController.navigate(Routes.OPERATION_HISTORY) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = { navController.navigateToSettings() },
            )
        }

        composable(Routes.PRE_OPERATION_CHECK) {
            PreOperationCheckScreen(
                onBack = { navController.popBackStack() },
                onOpenComplete = {
                    navController.navigate(Routes.PRE_CHECK_COMPLETE) {
                        popUpTo(Routes.PRE_OPERATION_CHECK) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.PRE_CHECK_COMPLETE) {
            PreCheckCompleteScreen(
                onBack = { navController.popBackStack() },
                onStartOperation = {
                    navController.navigate(Routes.START_CONFIRM)
                },
            )
        }

        composable(Routes.START_CONFIRM) {
            StartConfirmScreen(
                onBack = { navController.popBackStack() },
                onConfirmStart = {
                    navController.navigate(Routes.START_PROCESSING)
                },
            )
        }

        composable(Routes.START_PROCESSING) {
            StartProcessingScreen(
                onFinished = {
                    // 위치 수집·운행 중 전환은 StartProcessingViewModel에서 이미 수행
                    OperationRuntimeStateHolder.takePendingStartId()
                    navController.navigate(Routes.START_COMPLETE) {
                        popUpTo(Routes.START_PROCESSING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.START_COMPLETE) {
            StartCompleteScreen(
                onGoToOperation = {
                    val operationId = OperationRuntimeStateHolder.activeOperationId()
                        ?: OperationRuntimeStateHolder.resolveFocusedOperationId()
                    navController.navigate(Routes.inOperationMinimal(operationId)) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = Routes.IN_OPERATION_MINIMAL,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            InOperationMinimalScreen(
                operationId = operationId,
                onBack = { navController.popBackStack() },
                onHome = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenDetailStatus = {
                    navController.navigate(Routes.inOperationDetailStatus(operationId))
                },
                onEndOperation = {
                    navController.navigate(Routes.endOperationConfirm(operationId))
                },
            )
        }

        composable(
            route = Routes.IN_OPERATION_DETAIL_STATUS,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            InOperationDetailStatusScreen(
                operationId = operationId,
                onBack = { navController.popBackStack() },
                onHome = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onEndOperation = {
                    navController.navigate(Routes.endOperationConfirm(operationId))
                },
                onSuspendRequest = {
                    navController.navigate(Routes.SAFE_STOP_CONFIRM)
                },
            )
        }

        composable(
            route = Routes.END_OPERATION_CONFIRM,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            EndOperationConfirmScreen(
                operationId = operationId,
                onBack = { navController.popBackStack() },
                onGoToProcessing = {
                    navController.navigate(Routes.endOperationProcessing(operationId)) {
                        popUpTo(Routes.END_OPERATION_CONFIRM) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.END_TIME_ELAPSED,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            EndTimeElapsedScreen(
                operationId = operationId,
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.inOperationMinimal(operationId)) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onEndOperation = {
                    navController.navigate(Routes.endOperationProcessing(operationId)) {
                        popUpTo(Routes.END_OPERATION_CONFIRM) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.ADMIN_FORCE_END,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            AdminForceEndScreen(
                operationId = operationId,
                onBack = { navController.popBackStack() },
                onGoToToday = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.END_OPERATION_PROCESSING,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            EndProcessingScreen(
                operationId = operationId,
                onFinished = {
                    navController.navigate(Routes.endOperationComplete(operationId)) {
                        popUpTo(Routes.END_OPERATION_PROCESSING) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.END_OPERATION_COMPLETE,
            arguments = listOf(
                navArgument("operationId") { type = NavType.StringType },
            ),
        ) { entry ->
            val operationId = entry.arguments?.getString("operationId").orEmpty()
            EndCompleteScreen(
                operationId = operationId,
                onGoToToday = {
                    navController.navigate(Routes.TODAY_OPERATION) {
                        popUpTo(Routes.TODAY_OPERATION) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenHistory = {
                    navController.navigate(Routes.OPERATION_HISTORY) {
                        launchSingleTop = true
                    }
                },
            )
        }

        // DRI-01-03F: 홈에서 운행 중 배차 탭 시 진입
        composable(Routes.OPERATION_RECOVERY) {
            OperationRecoveryScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.TODAY_OPERATION) {
                            popUpTo(Routes.OPERATION_RECOVERY) { inclusive = true }
                        }
                    }
                },
                onGoToOperation = { operationId ->
                    // 운행 화면으로 이동 → DRI-01-03D 운행 중 최소
                    navController.navigate(Routes.inOperationMinimal(operationId))
                },
                onGoToToday = {
                    if (!navController.popBackStack(Routes.TODAY_OPERATION, inclusive = false)) {
                        navController.navigate(Routes.TODAY_OPERATION) {
                            popUpTo(Routes.OPERATION_RECOVERY) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onOpenBackgroundGuide = {
                    navController.navigate(Routes.BACKGROUND_GUIDE)
                },
                onOpenBatteryWarning = {
                    navController.navigate(Routes.BATTERY_WARNING)
                },
            )
        }

        composable(Routes.BACKGROUND_GUIDE) {
            BackgroundGuideScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.BATTERY_WARNING) {
            BatteryWarningScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.OPERATION_ALARMS) {
            OperationAlarmListScreen(
                onBack = { navController.popBackStack() },
                onOpenAssignmentChange = {
                    navController.navigate(Routes.ASSIGNMENT_CHANGE)
                },
                onOpenDepartureTimeChange = {
                    navController.navigate(Routes.DEPARTURE_TIME_CHANGE)
                },
                onOpenOperationCancel = {
                    navController.navigate(Routes.OPERATION_CANCEL)
                },
                onOpenSettings = { navController.navigateToSettings() },
            )
        }

        composable(Routes.ASSIGNMENT_CHANGE) {
            AssignmentChangeScreen(
                onConfirm = {
                    navController.navigate(Routes.VEHICLE_CHANGE)
                },
                onGoHome = {
                    navController.popBackStack(Routes.TODAY_OPERATION, inclusive = false)
                },
                onBack = {
                    navController.popBackStack(Routes.OPERATION_ALARMS, inclusive = false)
                },
                onOpenAlarms = {
                    navController.popBackStack(Routes.OPERATION_ALARMS, inclusive = false)
                },
            )
        }

        composable(Routes.VEHICLE_CHANGE) {
            VehicleChangeScreen(
                onConfirm = {
                    navController.popBackStack(Routes.TODAY_OPERATION, inclusive = false)
                },
                onBack = {
                    navController.popBackStack()
                },
                onOpenAlarms = {
                    // 종 버튼 → 운행 알림 목록
                    if (!navController.popBackStack(Routes.OPERATION_ALARMS, inclusive = false)) {
                        navController.navigate(Routes.OPERATION_ALARMS) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Routes.DEPARTURE_TIME_CHANGE) {
            DepartureTimeChangeScreen(
                onConfirm = {
                    navController.popBackStack(Routes.TODAY_OPERATION, inclusive = false)
                },
                onBack = {
                    navController.popBackStack(Routes.OPERATION_ALARMS, inclusive = false)
                },
                onOpenAlarms = {
                    if (!navController.popBackStack(Routes.OPERATION_ALARMS, inclusive = false)) {
                        navController.navigate(Routes.OPERATION_ALARMS) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Routes.OPERATION_CANCEL) {
            OperationCancelScreen(
                onConfirm = {
                    navController.popBackStack(Routes.TODAY_OPERATION, inclusive = false)
                },
                onBack = {
                    navController.popBackStack(Routes.OPERATION_ALARMS, inclusive = false)
                },
                onOpenAlarms = {
                    if (!navController.popBackStack(Routes.OPERATION_ALARMS, inclusive = false)) {
                        navController.navigate(Routes.OPERATION_ALARMS) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}
