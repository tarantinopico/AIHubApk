package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTypography
import com.example.ui.theme.LocalAppShapes
import com.example.ui.theme.LocalAppSpacing

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = LocalAppShapes.current.extraLarge
    
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .then(
                if (enabled) Modifier.gradientBackground(shape)
                else Modifier.background(com.example.ui.theme.SurfaceElevated)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTypography.labelLarge,
            color = if (enabled) Color.White else com.example.ui.theme.TextMuted,
            modifier = Modifier.padding(horizontal = LocalAppSpacing.current.extraLarge)
        )
    }
}
