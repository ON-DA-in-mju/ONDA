package com.onda.mju.student

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.onda.mju.student.data.permission.PermissionGrantState
import com.onda.mju.student.ui.screen.login.FindIdScreen
import com.onda.mju.student.ui.screen.login.FindPasswordScreen
import com.onda.mju.student.ui.screen.login.LoginScreen
import com.onda.mju.student.ui.screen.login.LoginStartScreen
import com.onda.mju.student.ui.screen.main.StudentMainShell
import com.onda.mju.student.ui.screen.permission.PermissionCompleteScreen
import com.onda.mju.student.ui.screen.permission.PermissionGuideScreen
import com.onda.mju.student.ui.theme.ONDAStudentTheme
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
    var screen by remember { mutableStateOf(AppScreen.Splash) }
    var permissionGrantState by remember {
        mutableStateOf(
            PermissionGrantState(
                locationGranted = false,
                notificationGranted = false,
            ),
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { _ ->
        when (screen) {
            AppScreen.Splash -> {
                SplashScreen(
                    modifier = Modifier.fillMaxSize(),
                    onSplashFinished = { screen = AppScreen.LoginStart },
                )
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
                        scope.launch {
                            snackbarHostState.showSnackbar("로그인되었습니다.")
                            delay(400)
                            screen = AppScreen.PermissionGuide
                        }
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
                        screen = AppScreen.PermissionComplete
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
                        showMessage("로그아웃되었습니다.")
                        screen = AppScreen.LoginStart
                    },
                )
            }
        }
    }
}
