package com.onda.mju.student

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.onda.mju.student.data.auth.AutoLoginPreferences
import com.onda.mju.student.data.auth.SupabaseAuthRepository
import com.onda.mju.student.data.permission.PermissionGrantState
import com.onda.mju.student.data.permission.currentPermissionGrantState
import com.onda.mju.student.ui.screen.login.FindIdScreen
import com.onda.mju.student.ui.screen.login.FindPasswordScreen
import com.onda.mju.student.ui.screen.login.LoginScreen
import com.onda.mju.student.ui.screen.login.LoginStartScreen
import com.onda.mju.student.ui.screen.main.StudentMainShell
import com.onda.mju.student.ui.screen.permission.PermissionCompleteScreen
import com.onda.mju.student.ui.screen.permission.PermissionGuideScreen
import com.onda.mju.student.ui.theme.ONDAStudentTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppScreen {
    Splash,
    LoginStart,
    Login,
    FindId,
    FindPassword,
    PermissionGuide,
    PermissionComplete,
    Main,
}

/** 브랜드가 보이도록 기사 앱과 같은 스플래시 시간 */
private const val SplashMinMillis = 2800L

/** 자동 로그인 세션 복원 대기 상한 (스플래시와 병렬) */
private const val SessionWaitMillis = 450L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ONDAStudentTheme {
                OndaStudentApp()
            }
        }
    }
}

@Composable
private fun OndaStudentApp() {
    val context = LocalContext.current
    val authRepository = remember { SupabaseAuthRepository() }
    val autoLoginPrefs = remember { AutoLoginPreferences(context) }

    var screen by remember { mutableStateOf(AppScreen.Splash) }
    var permissionGrantState by remember {
        mutableStateOf(currentPermissionGrantState(context))
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun screenAfterLogin(): AppScreen {
        val grants = currentPermissionGrantState(context)
        permissionGrantState = grants
        return if (grants.isReadyForMain) AppScreen.Main else AppScreen.PermissionGuide
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { _ ->
        when (screen) {
            AppScreen.Splash -> {
                // 세션 복원과 최소 스플래시를 병렬로 돌리고, 둘 다 끝나면 즉시 전환
                LaunchedEffect(Unit) {
                    val startedAt = SystemClock.elapsedRealtime()
                    coroutineScope {
                        val sessionOk = async {
                            if (autoLoginPrefs.isEnabled) {
                                authRepository.awaitActiveSession(SessionWaitMillis)
                            } else {
                                false
                            }
                        }
                        val canAutoLogin = sessionOk.await() && authRepository.hasActiveSession()
                        val elapsed = SystemClock.elapsedRealtime() - startedAt
                        if (elapsed < SplashMinMillis) {
                            delay(SplashMinMillis - elapsed)
                        }
                        screen = if (canAutoLogin) AppScreen.Main else AppScreen.LoginStart
                    }
                }
                SplashScreen(modifier = Modifier.fillMaxSize())
            }

            AppScreen.LoginStart -> {
                LoginStartScreen(
                    modifier = Modifier.fillMaxSize(),
                    onLoginClick = { screen = AppScreen.Login },
                    onPrivacyPolicyClick = {
                        showMessage("개인정보 처리방침은 준비 중입니다.")
                    },
                    onTermsClick = {
                        showMessage("서비스 이용약관은 준비 중입니다.")
                    },
                )
            }

            AppScreen.Login -> {
                LoginScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = { screen = AppScreen.LoginStart },
                    onFindIdClick = { screen = AppScreen.FindId },
                    onFindPasswordClick = { screen = AppScreen.FindPassword },
                    onShowMessage = ::showMessage,
                    onLoginSuccess = {
                        showMessage("로그인되었습니다.")
                        // 권한 이미 있으면 권한 화면을 건너뛰고 바로 홈으로
                        screen = screenAfterLogin()
                    },
                )
            }

            AppScreen.FindId -> {
                FindIdScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = { screen = AppScreen.Login },
                    onShowMessage = ::showMessage,
                    onGoLoginClick = { screen = AppScreen.Login },
                )
            }

            AppScreen.FindPassword -> {
                FindPasswordScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = { screen = AppScreen.Login },
                    onShowMessage = ::showMessage,
                    onGoLoginClick = { screen = AppScreen.Login },
                )
            }

            AppScreen.PermissionGuide -> {
                PermissionGuideScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = { screen = AppScreen.Login },
                    onSkipClick = { screen = AppScreen.Main },
                    onPermissionsConfigured = { grantState ->
                        permissionGrantState = grantState
                        // 완료 화면 한 번 더 거치지 않고 바로 홈으로
                        screen = AppScreen.Main
                    },
                )
            }

            AppScreen.PermissionComplete -> {
                PermissionCompleteScreen(
                    grantState = permissionGrantState,
                    modifier = Modifier.fillMaxSize(),
                    onGoHomeClick = { screen = AppScreen.Main },
                )
            }

            AppScreen.Main -> {
                StudentMainShell(
                    modifier = Modifier.fillMaxSize(),
                    onLogout = {
                        scope.launch {
                            authRepository.signOut()
                            // 명시적 로그아웃 후에는 자동 로그인을 끄고, 학번만 남겨 둔다.
                            autoLoginPrefs.isEnabled = false
                            showMessage("로그아웃되었습니다.")
                            screen = AppScreen.LoginStart
                        }
                    },
                )
            }
        }
    }
}
