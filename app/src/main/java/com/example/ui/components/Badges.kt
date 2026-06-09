package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTypography
import com.example.ui.theme.ProviderGoogle
import com.example.ui.theme.ProviderGroq
import com.example.ui.theme.ProviderNvidia
import com.example.ui.theme.SurfaceElevated

@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.gradientBackground(CircleShape)
                else Modifier.background(SurfaceElevated)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = AppTypography.labelMedium,
            color = if (selected) Color.White else com.example.ui.theme.TextMuted
        )
    }
}

@Composable
fun ProviderBadge(providerName: String, providerType: com.example.domain.model.ProviderType, modifier: Modifier = Modifier) {
    val color = when(providerType) {
        com.example.domain.model.ProviderType.GOOGLE -> ProviderGoogle
        com.example.domain.model.ProviderType.GROQ -> ProviderGroq
        com.example.domain.model.ProviderType.NVIDIA -> ProviderNvidia
        else -> Color.Gray
    }
    
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(SurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = providerName, style = AppTypography.labelMedium, color = Color.White)
    }
}
