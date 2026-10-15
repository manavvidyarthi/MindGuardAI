package com.manav.mindguardai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val Background = Color(0xFF080D18)
    val BackgroundRaised = Color(0xFF0E1728)
    val Surface = Color(0xFF121E31)
    val SurfaceRaised = Color(0xFF192840)
    val SurfaceSoft = Color(0xFF202F49)
    val Mint = Color(0xFF63E6C2)
    val MintDeep = Color(0xFF31CFA7)
    val Lavender = Color(0xFFA7A2FF)
    val Blue = Color(0xFF8CB9FF)
    val Coral = Color(0xFFFF9C91)
    val Amber = Color(0xFFFFD18A)
    val Text = Color(0xFFF4F7FF)
    val Muted = Color(0xFF9AAAC1)
    val Border = Color(0x20FFFFFF)
    val Track = Color(0xFF263650)
}

private val DarkColors = darkColorScheme(
    primary = Palette.Mint,
    onPrimary = Palette.Background,
    secondary = Palette.Lavender,
    onSecondary = Palette.Background,
    background = Palette.Background,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceRaised,
    onSurfaceVariant = Palette.Muted,
    error = Palette.Coral,
    outline = Palette.Border
)

@Composable
fun MindGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
