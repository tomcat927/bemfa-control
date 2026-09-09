package com.tomcat927.bemfacontrol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Shapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF047E74),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F6F2),
    onPrimaryContainer = Color(0xFF004C46),
    inversePrimary = Color(0xFF66D3C6),

    secondary = Color(0xFF446A68),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9F2EF),
    onSecondaryContainer = Color(0xFF084C48),

    tertiary = Color(0xFF836A46),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E2C8),
    onTertiaryContainer = Color(0xFF3B2A11),

    error = Color(0xFFD92D20),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE4E2),
    onErrorContainer = Color(0xFF7A271A),

    background = Color(0xFFF7F8FB),
    onBackground = Color(0xFF171A1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171A1F),
    surfaceVariant = Color(0xFFF1F4F8),
    onSurfaceVariant = Color(0xFF667085),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF8FAFC),
    surfaceContainerHighest = Color(0xFFF1F4F8),
    surfaceTint = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFF2B3138),
    inverseOnSurface = Color(0xFFEFF1F4),
    outline = Color(0xFFDCE1E8),
    outlineVariant = Color(0xFFE8ECF2),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF68D4C6),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB1F1E8),
    inversePrimary = Color(0xFF047E74),

    secondary = Color(0xFFA5D0CB),
    onSecondary = Color(0xFF093733),
    secondaryContainer = Color(0xFF264E4A),
    onSecondaryContainer = Color(0xFFC0EBE6),

    tertiary = Color(0xFFE4C69F),
    onTertiary = Color(0xFF402D14),
    tertiaryContainer = Color(0xFF5C4328),
    onTertiaryContainer = Color(0xFFFFDFBA),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F150F),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD5),

    background = Color(0xFF0E1416),
    onBackground = Color(0xFFDEE4E6),
    surface = Color(0xFF151C1E),
    onSurface = Color(0xFFDEE4E6),
    surfaceVariant = Color(0xFF232C2E),
    onSurfaceVariant = Color(0xFFA9B3B5),
    surfaceContainerLowest = Color(0xFF101618),
    surfaceContainerLow = Color(0xFF151C1E),
    surfaceContainer = Color(0xFF182022),
    surfaceContainerHigh = Color(0xFF20282A),
    surfaceContainerHighest = Color(0xFF232C2E),
    surfaceTint = Color(0xFF151C1E),
    inverseSurface = Color(0xFFDEE4E6),
    inverseOnSurface = Color(0xFF151C1E),
    outline = Color(0xFF3A4446),
    outlineVariant = Color(0xFF2B3537),
    scrim = Color(0xFF000000),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)


@Composable
fun BemfaControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
