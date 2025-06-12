package net.phbwt.paperwork.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorScheme.success: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF00E676) else Color(0xFF4CAF50)

val ColorScheme.caution: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFFFEA00) else Color(0xFFFF9100)

