package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkTerracottaPrimary,
    onPrimary = DarkTerracottaOnPrimary,
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = DarkTerracottaOnContainer,
    secondary = DarkSageSecondary,
    onSecondary = DarkSageOnSecondary,
    secondaryContainer = DarkSageContainer,
    onSecondaryContainer = DarkSageOnContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkAmberOnTertiary,
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = DarkAmberOnContainer,
    background = ObsidianBackground,
    onBackground = CrispOnDarkBackground,
    surface = ElevatedDarkSurface,
    onSurface = CrispOnDarkBackground,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = MutedOnDarkVariant,
    surfaceContainerLow = ElevatedDarkSurface,
    surfaceContainer = CharcoalSurfaceVariant,
    surfaceContainerHigh = ColorPaletteTokens.DarkCardHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaOnContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = AlabasterBackground,
    onBackground = InkOnBackground,
    surface = PaperSurface,
    onSurface = InkOnBackground,
    surfaceVariant = StoneSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = StoneSurfaceVariant,
    surfaceContainerHigh = ColorPaletteTokens.LightCardHigh,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = ErrorLight
)

private object ColorPaletteTokens {
    val LightCardHigh = androidx.compose.ui.graphics.Color(0xFFEAE3D7)
    val DarkCardHigh = androidx.compose.ui.graphics.Color(0xFF2F333C)
}

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DailyPlannerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
