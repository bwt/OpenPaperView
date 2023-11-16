package net.phbwt.paperwork.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import net.phbwt.paperwork.ui.main.MainScreen
import net.phbwt.paperwork.ui.theme.AppTheme
import net.phbwt.paperwork.ui.theme.DarkColors
import net.phbwt.paperwork.ui.theme.LightColors


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val isDark = isSystemInDarkTheme()
            ChangeSystemBarsTheme(isDark)
            AppTheme(isDark) {
                MainScreen()
            }
        }
    }

    @Composable
    private fun ChangeSystemBarsTheme(isDark: Boolean) {
        LaunchedEffect(isDark) {
            if (isDark) {
                val statusbarColor = LightColors.onSecondaryContainer.toArgb()
                val navbarColor = LightColors.onSecondaryContainer.copy(alpha = .6f).toArgb()
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(statusbarColor),
                    navigationBarStyle = SystemBarStyle.dark(navbarColor),
                )
            } else {
                val statusbarColor = DarkColors.onSecondaryContainer.toArgb()
                val navbarColor = DarkColors.onSecondaryContainer.copy(alpha = .6f).toArgb()
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(statusbarColor, statusbarColor),
                    navigationBarStyle = SystemBarStyle.light(navbarColor, navbarColor),
                )
            }
        }
    }
}


