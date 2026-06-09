package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0A0A0C)
val Surface = Color(0xFF131318)
val SurfaceElevated = Color(0xFF1C1C24)

val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFA0A0AB)
val TextMuted = Color(0xFF6B6B76)

val GradientStart = Color(0xFF7C5CFF)
val GradientMiddle = Color(0xFF4C7DFF)
val GradientEnd = Color(0xFF2FD9C5)

val Success = Color(0xFF2FD98A)
val Error = Color(0xFFFF5C7C)
val Warning = Color(0xFFFFB84C)

val ProviderGoogle = Color(0xFF4285F4)
val ProviderGroq = Color(0xFFF55036)
val ProviderNvidia = Color(0xFF76B900)

val GlobalGradient = Brush.linearGradient(
    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
)
