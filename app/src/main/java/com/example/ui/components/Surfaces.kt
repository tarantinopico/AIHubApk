package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppShapes

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = com.example.ui.theme.Surface,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = LocalAppShapes.current.large
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor.copy(alpha = 0.8f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), shape),
        content = content
    )
}
