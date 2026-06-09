package com.example.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.staticCompositionLocalOf

data class AppAnimationConfig(
    val durationShort: Int = 120,
    val durationMedium: Int = 180,
    val easing: Easing = FastOutSlowInEasing
)

val LocalAppAnimation = staticCompositionLocalOf { AppAnimationConfig() }
