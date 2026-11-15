package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = OrangeSoftContainer,
    onPrimaryContainer = OrangePrimaryDark,
    secondary = IncomeGreen,
    onSecondary = Color.White,
    secondaryContainer = IncomeGreenSoft,
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = TransportBlue,
    onTertiary = Color.White,
    tertiaryContainer = TransportBlueSoft,
    onTertiaryContainer = Color(0xFF1E3A8A),
    background = CreamBackground,
    onBackground = CharcoalText,
    surface = CardWhite,
    onSurface = CharcoalText,
    surfaceVariant = SoftSandSurface,
    onSurfaceVariant = MutedSlateText,
    outline = SubtleBorderLight,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedSoft,
    onErrorContainer = Color(0xFF991B1B)
)

private val DarkColors = darkColorScheme(
    primary = DarkOrangePrimary,
    onPrimary = Color(0xFF1F120A),
    primaryContainer = DarkOrangeContainer,
    onPrimaryContainer = Color(0xFFFFD8C4),
    secondary = IncomeGreen,
    onSecondary = Color(0xFF042F1E),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = TransportBlue,
    onTertiary = Color(0xFF0A1D3D),
    tertiaryContainer = Color(0xFF1E3A8A),
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF3F0B0B),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

private val HisabKoriShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HisabKoriTheme(
    themeMode: String = "light",
    textScale: String = "medium",
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val scaleMultiplier = when (textScale) {
        "small" -> 0.90f
        "large" -> 1.14f
        else -> 1.0f
    }

    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = buildHisabKoriTypography(scaleMultiplier),
        shapes = HisabKoriShapes,
        content = content
    )
}
