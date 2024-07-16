package net.phbwt.paperwork.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import net.phbwt.paperwork.ui.main.MainScreen
import net.phbwt.paperwork.ui.theme.AppTheme
import net.phbwt.paperwork.ui.theme.darkScheme
import net.phbwt.paperwork.ui.theme.lightScheme


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val isDark = isSystemInDarkTheme()
            ChangeSystemBarsTheme(isDark)
            AppTheme(isDark, false) {
                MainScreen()
            }
        }
    }

    @Composable
    private fun ChangeSystemBarsTheme(isDark: Boolean) {

        LaunchedEffect(isDark) {
            val statusbarDark = darkScheme.primaryContainer.toArgb()
            val statusbarLight = lightScheme.primaryContainer.toArgb()
            // ignored on 29+
            val navbarLight = lightScheme.primaryContainer.copy(alpha = .6f).toArgb()
            val navbarDark = darkScheme.primaryContainer.copy(alpha = .6f).toArgb()
            if (isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(statusbarDark),
                    navigationBarStyle = SystemBarStyle.auto(navbarLight, navbarDark),
                )
            } else {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(statusbarLight, statusbarDark),
                    navigationBarStyle = SystemBarStyle.auto(navbarLight, navbarDark),
                )
            }
        }
    }
}


