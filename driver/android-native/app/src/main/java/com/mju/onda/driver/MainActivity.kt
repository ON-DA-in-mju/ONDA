package com.mju.onda.driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mju.onda.driver.core.navigation.OndaNavHost
import com.mju.onda.driver.core.theme.OndaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // 시스템 스플래시(원형 아이콘)를 바로 종료 → Compose DRI-00-00만 표시
        splashScreen.setKeepOnScreenCondition { false }
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OndaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OndaNavHost()
                }
            }
        }
    }
}
