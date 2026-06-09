package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import com.example.ui.theme.GlobalGradient

fun Modifier.gradientBackground(shape: Shape): Modifier = composed {
    this.background(brush = GlobalGradient, shape = shape)
}
