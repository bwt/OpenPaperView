package net.phbwt.paperwork.ui.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val lightScheme = lightColorScheme(
    primary = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryLight,
    onPrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryLight,
    primaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryContainerLight,
    onPrimaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryContainerLight,
    secondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryLight,
    onSecondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryLight,
    secondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryContainerLight,
    onSecondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryContainerLight,
    tertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryLight,
    onTertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryLight,
    tertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryContainerLight,
    onTertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryContainerLight,
    error = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorLight,
    onError = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorLight,
    errorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorContainerLight,
    onErrorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorContainerLight,
    background = _root_ide_package_.net.phbwt.paperwork.ui.theme.backgroundLight,
    onBackground = _root_ide_package_.net.phbwt.paperwork.ui.theme.onBackgroundLight,
    surface = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceLight,
    onSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceLight,
    surfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceVariantLight,
    onSurfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceVariantLight,
    outline = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineLight,
    outlineVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineVariantLight,
    scrim = _root_ide_package_.net.phbwt.paperwork.ui.theme.scrimLight,
    inverseSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseSurfaceLight,
    inverseOnSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseOnSurfaceLight,
    inversePrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.inversePrimaryLight,
    surfaceDim = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDimLight,
    surfaceBright = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceBrightLight,
    surfaceContainerLowest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowestLight,
    surfaceContainerLow = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowLight,
    surfaceContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLight,
    surfaceContainerHigh = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighLight,
    surfaceContainerHighest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighestLight,
)

val darkScheme = darkColorScheme(
    primary = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryDark,
    onPrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryDark,
    primaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryContainerDark,
    onPrimaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryContainerDark,
    secondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryDark,
    onSecondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryDark,
    secondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryContainerDark,
    onSecondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryContainerDark,
    tertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryDark,
    onTertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryDark,
    tertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryContainerDark,
    onTertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryContainerDark,
    error = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorDark,
    onError = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorDark,
    errorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorContainerDark,
    onErrorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorContainerDark,
    background = _root_ide_package_.net.phbwt.paperwork.ui.theme.backgroundDark,
    onBackground = _root_ide_package_.net.phbwt.paperwork.ui.theme.onBackgroundDark,
    surface = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDark,
    onSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceDark,
    surfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceVariantDark,
    onSurfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceVariantDark,
    outline = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineDark,
    outlineVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineVariantDark,
    scrim = _root_ide_package_.net.phbwt.paperwork.ui.theme.scrimDark,
    inverseSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseSurfaceDark,
    inverseOnSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseOnSurfaceDark,
    inversePrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.inversePrimaryDark,
    surfaceDim = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDimDark,
    surfaceBright = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceBrightDark,
    surfaceContainerLowest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowestDark,
    surfaceContainerLow = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowDark,
    surfaceContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerDark,
    surfaceContainerHigh = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighDark,
    surfaceContainerHighest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighestDark,
)

private val mediumContrastLightColorScheme = lightColorScheme(
    primary = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryLightMediumContrast,
    onPrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryLightMediumContrast,
    primaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryContainerLightMediumContrast,
    onPrimaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryContainerLightMediumContrast,
    secondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryLightMediumContrast,
    onSecondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryLightMediumContrast,
    secondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryContainerLightMediumContrast,
    onSecondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryContainerLightMediumContrast,
    tertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryLightMediumContrast,
    onTertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryLightMediumContrast,
    tertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryContainerLightMediumContrast,
    onTertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryContainerLightMediumContrast,
    error = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorLightMediumContrast,
    onError = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorLightMediumContrast,
    errorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorContainerLightMediumContrast,
    onErrorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorContainerLightMediumContrast,
    background = _root_ide_package_.net.phbwt.paperwork.ui.theme.backgroundLightMediumContrast,
    onBackground = _root_ide_package_.net.phbwt.paperwork.ui.theme.onBackgroundLightMediumContrast,
    surface = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceLightMediumContrast,
    onSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceLightMediumContrast,
    surfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceVariantLightMediumContrast,
    onSurfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceVariantLightMediumContrast,
    outline = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineLightMediumContrast,
    outlineVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineVariantLightMediumContrast,
    scrim = _root_ide_package_.net.phbwt.paperwork.ui.theme.scrimLightMediumContrast,
    inverseSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseSurfaceLightMediumContrast,
    inverseOnSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseOnSurfaceLightMediumContrast,
    inversePrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.inversePrimaryLightMediumContrast,
    surfaceDim = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDimLightMediumContrast,
    surfaceBright = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceBrightLightMediumContrast,
    surfaceContainerLowest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowestLightMediumContrast,
    surfaceContainerLow = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowLightMediumContrast,
    surfaceContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLightMediumContrast,
    surfaceContainerHigh = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighLightMediumContrast,
    surfaceContainerHighest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
    primary = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryLightHighContrast,
    onPrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryLightHighContrast,
    primaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryContainerLightHighContrast,
    onPrimaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryContainerLightHighContrast,
    secondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryLightHighContrast,
    onSecondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryLightHighContrast,
    secondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryContainerLightHighContrast,
    onSecondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryContainerLightHighContrast,
    tertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryLightHighContrast,
    onTertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryLightHighContrast,
    tertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryContainerLightHighContrast,
    onTertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryContainerLightHighContrast,
    error = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorLightHighContrast,
    onError = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorLightHighContrast,
    errorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorContainerLightHighContrast,
    onErrorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorContainerLightHighContrast,
    background = _root_ide_package_.net.phbwt.paperwork.ui.theme.backgroundLightHighContrast,
    onBackground = _root_ide_package_.net.phbwt.paperwork.ui.theme.onBackgroundLightHighContrast,
    surface = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceLightHighContrast,
    onSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceLightHighContrast,
    surfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceVariantLightHighContrast,
    onSurfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceVariantLightHighContrast,
    outline = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineLightHighContrast,
    outlineVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineVariantLightHighContrast,
    scrim = _root_ide_package_.net.phbwt.paperwork.ui.theme.scrimLightHighContrast,
    inverseSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseSurfaceLightHighContrast,
    inverseOnSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseOnSurfaceLightHighContrast,
    inversePrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.inversePrimaryLightHighContrast,
    surfaceDim = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDimLightHighContrast,
    surfaceBright = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceBrightLightHighContrast,
    surfaceContainerLowest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowestLightHighContrast,
    surfaceContainerLow = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowLightHighContrast,
    surfaceContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLightHighContrast,
    surfaceContainerHigh = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighLightHighContrast,
    surfaceContainerHighest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
    primary = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryDarkMediumContrast,
    onPrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryDarkMediumContrast,
    primaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryContainerDarkMediumContrast,
    onPrimaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryContainerDarkMediumContrast,
    secondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryDarkMediumContrast,
    onSecondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryDarkMediumContrast,
    secondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryContainerDarkMediumContrast,
    onSecondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryContainerDarkMediumContrast,
    tertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryDarkMediumContrast,
    onTertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryDarkMediumContrast,
    tertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryContainerDarkMediumContrast,
    onTertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryContainerDarkMediumContrast,
    error = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorDarkMediumContrast,
    onError = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorDarkMediumContrast,
    errorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorContainerDarkMediumContrast,
    onErrorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorContainerDarkMediumContrast,
    background = _root_ide_package_.net.phbwt.paperwork.ui.theme.backgroundDarkMediumContrast,
    onBackground = _root_ide_package_.net.phbwt.paperwork.ui.theme.onBackgroundDarkMediumContrast,
    surface = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDarkMediumContrast,
    onSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceDarkMediumContrast,
    surfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceVariantDarkMediumContrast,
    onSurfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceVariantDarkMediumContrast,
    outline = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineDarkMediumContrast,
    outlineVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineVariantDarkMediumContrast,
    scrim = _root_ide_package_.net.phbwt.paperwork.ui.theme.scrimDarkMediumContrast,
    inverseSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseSurfaceDarkMediumContrast,
    inverseOnSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseOnSurfaceDarkMediumContrast,
    inversePrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.inversePrimaryDarkMediumContrast,
    surfaceDim = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDimDarkMediumContrast,
    surfaceBright = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceBrightDarkMediumContrast,
    surfaceContainerLowest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowestDarkMediumContrast,
    surfaceContainerLow = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowDarkMediumContrast,
    surfaceContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerDarkMediumContrast,
    surfaceContainerHigh = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighDarkMediumContrast,
    surfaceContainerHighest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
    primary = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryDarkHighContrast,
    onPrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryDarkHighContrast,
    primaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.primaryContainerDarkHighContrast,
    onPrimaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onPrimaryContainerDarkHighContrast,
    secondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryDarkHighContrast,
    onSecondary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryDarkHighContrast,
    secondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.secondaryContainerDarkHighContrast,
    onSecondaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSecondaryContainerDarkHighContrast,
    tertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryDarkHighContrast,
    onTertiary = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryDarkHighContrast,
    tertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.tertiaryContainerDarkHighContrast,
    onTertiaryContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onTertiaryContainerDarkHighContrast,
    error = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorDarkHighContrast,
    onError = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorDarkHighContrast,
    errorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.errorContainerDarkHighContrast,
    onErrorContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.onErrorContainerDarkHighContrast,
    background = _root_ide_package_.net.phbwt.paperwork.ui.theme.backgroundDarkHighContrast,
    onBackground = _root_ide_package_.net.phbwt.paperwork.ui.theme.onBackgroundDarkHighContrast,
    surface = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDarkHighContrast,
    onSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceDarkHighContrast,
    surfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceVariantDarkHighContrast,
    onSurfaceVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.onSurfaceVariantDarkHighContrast,
    outline = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineDarkHighContrast,
    outlineVariant = _root_ide_package_.net.phbwt.paperwork.ui.theme.outlineVariantDarkHighContrast,
    scrim = _root_ide_package_.net.phbwt.paperwork.ui.theme.scrimDarkHighContrast,
    inverseSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseSurfaceDarkHighContrast,
    inverseOnSurface = _root_ide_package_.net.phbwt.paperwork.ui.theme.inverseOnSurfaceDarkHighContrast,
    inversePrimary = _root_ide_package_.net.phbwt.paperwork.ui.theme.inversePrimaryDarkHighContrast,
    surfaceDim = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceDimDarkHighContrast,
    surfaceBright = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceBrightDarkHighContrast,
    surfaceContainerLowest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowestDarkHighContrast,
    surfaceContainerLow = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerLowDarkHighContrast,
    surfaceContainer = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerDarkHighContrast,
    surfaceContainerHigh = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighDarkHighContrast,
    surfaceContainerHighest = _root_ide_package_.net.phbwt.paperwork.ui.theme.surfaceContainerHighestDarkHighContrast,
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

val unspecified_scheme = ColorFamily(
    Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
  val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      
      darkTheme -> darkScheme
      else -> lightScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    content = content
  )
}

