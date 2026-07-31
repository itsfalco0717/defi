package com.tenisturnuva.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CourtGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CourtGreenContainerLight,
    onPrimaryContainer = OnCourtGreen,
    secondary = EnergyOrange,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = OrangeContainerLight,
    onSecondaryContainer = OnEnergyOrange,
    tertiary = TennisBallYellow,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White
)

private val DarkColors = darkColorScheme(
    primary = CourtGreen,
    onPrimary = OnCourtGreen,
    primaryContainer = CourtGreenContainerDark,
    onPrimaryContainer = CourtGreenContainerLight,
    secondary = EnergyOrangeLight,
    onSecondary = OnEnergyOrange,
    secondaryContainer = OrangeContainerDark,
    onSecondaryContainer = OrangeContainerLight,
    tertiary = TennisBallYellow,
    background = SurfaceDark,
    surface = SurfaceContainerDark
)

@Composable
fun TenisTurnuvaTheme(
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
