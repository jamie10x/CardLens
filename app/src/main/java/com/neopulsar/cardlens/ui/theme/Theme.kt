package com.neopulsar.cardlens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.neopulsar.cardlens.core.domain.ContactStatus

private val LightColors = lightColorScheme(
    primary = Teal700,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    inversePrimary = Teal400,
    secondary = Indigo600,
    onSecondary = Color.White,
    secondaryContainer = Indigo50,
    onSecondaryContainer = Indigo600,
    tertiary = InfoCyan,
    onTertiary = Color.White,
    tertiaryContainer = InfoCyanSoft,
    onTertiaryContainer = InfoCyan,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceTint = Teal700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedSoft,
    onErrorContainer = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = Teal400,
    onPrimary = Teal900,
    primaryContainer = Teal800,
    onPrimaryContainer = Teal100,
    inversePrimary = Teal700,
    secondary = Indigo200,
    onSecondary = Indigo600,
    secondaryContainer = Indigo600,
    onSecondaryContainer = Indigo50,
    tertiary = InfoCyan,
    onTertiary = Color.White,
    tertiaryContainer = InfoCyan,
    onTertiaryContainer = Teal900,
    background = Ink950,
    onBackground = Slate200,
    surface = Ink900,
    onSurface = Slate200,
    surfaceVariant = Ink800,
    onSurfaceVariant = Slate400,
    surfaceTint = Teal400,
    outline = Ink700,
    outlineVariant = Ink700,
    error = Color(0xFFFCA5A5),
    onError = Slate900,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
)

@Composable
fun CardLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

fun ColorScheme.statusColor(status: ContactStatus): Color {
    return when (status) {
        ContactStatus.New -> secondary
        ContactStatus.FollowUpNeeded -> WarningAmber
        ContactStatus.Contacted -> InfoCyan
        ContactStatus.MeetingScheduled -> PurpleViolet
        ContactStatus.Converted -> SuccessGreen
        ContactStatus.Archived -> onSurfaceVariant
    }
}

fun ColorScheme.statusSoftColor(status: ContactStatus): Color {
    return when (status) {
        ContactStatus.New -> Indigo50
        ContactStatus.FollowUpNeeded -> WarningAmberSoft
        ContactStatus.Contacted -> InfoCyanSoft
        ContactStatus.MeetingScheduled -> PurpleVioletSoft
        ContactStatus.Converted -> SuccessGreenSoft
        ContactStatus.Archived -> surfaceVariant
    }
}
