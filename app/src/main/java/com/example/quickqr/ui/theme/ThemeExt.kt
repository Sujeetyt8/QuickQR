package com.example.quickqr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppTheme {
    val colors: AppColors
        @Composable
        get() = MaterialTheme.colorScheme.run {
            AppColors(
                primary = primary,
                primaryVariant = primaryContainer,
                secondary = secondary,
                secondaryVariant = secondaryContainer,
                background = background,
                surface = surface,
                error = error,
                onPrimary = onPrimary,
                onSecondary = onSecondary,
                onBackground = onBackground,
                onSurface = onSurface,
                onError = onError,
                isLight = !isSystemInDarkTheme()
            )
        }

    val typography: AppTypography
        @Composable
        get() = AppTypography(
            h1 = MaterialTheme.typography.displayLarge,
            h2 = MaterialTheme.typography.displayMedium,
            h3 = MaterialTheme.typography.displaySmall,
            h4 = MaterialTheme.typography.headlineLarge,
            h5 = MaterialTheme.typography.headlineMedium,
            h6 = MaterialTheme.typography.headlineSmall,
            subtitle1 = MaterialTheme.typography.titleLarge,
            subtitle2 = MaterialTheme.typography.titleMedium,
            body1 = MaterialTheme.typography.bodyLarge,
            body2 = MaterialTheme.typography.bodyMedium,
            button = MaterialTheme.typography.labelLarge,
            caption = MaterialTheme.typography.bodySmall,
            overline = MaterialTheme.typography.labelSmall
        )
}

data class AppColors(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onError: Color,
    val isLight: Boolean
)

data class AppTypography(
    val h1: androidx.compose.ui.text.TextStyle,
    val h2: androidx.compose.ui.text.TextStyle,
    val h3: androidx.compose.ui.text.TextStyle,
    val h4: androidx.compose.ui.text.TextStyle,
    val h5: androidx.compose.ui.text.TextStyle,
    val h6: androidx.compose.ui.text.TextStyle,
    val subtitle1: androidx.compose.ui.text.TextStyle,
    val subtitle2: androidx.compose.ui.text.TextStyle,
    val body1: androidx.compose.ui.text.TextStyle,
    val body2: androidx.compose.ui.text.TextStyle,
    val button: androidx.compose.ui.text.TextStyle,
    val caption: androidx.compose.ui.text.TextStyle,
    val overline: androidx.compose.ui.text.TextStyle
)
