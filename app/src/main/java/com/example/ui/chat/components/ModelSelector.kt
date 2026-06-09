package com.example.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiModel
import com.example.domain.model.Provider
import com.example.ui.components.ProviderBadge
import com.example.ui.theme.*

@Composable
fun ModelSelector(
    providers: List<Provider>,
    models: List<AiModel>,
    activeModelId: String?,
    onSelectModel: (String) -> Unit,
    onAddProvider: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Surface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        val groupedModels = models.groupBy { it.providerId }
        
        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
            contentPadding = PaddingValues(horizontal = LocalAppSpacing.current.large)
        ) {
            providers.forEach { provider ->
                val providerModels = groupedModels[provider.id] ?: emptyList()
                if (providerModels.isNotEmpty()) {
                    item {
                        Text(
                            text = provider.name,
                            style = AppTypography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = LocalAppSpacing.current.large, bottom = LocalAppSpacing.current.small)
                        )
                    }
                    items(providerModels) { model ->
                        val isSelected = model.id == activeModelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(LocalAppShapes.current.medium)
                                .then(if (isSelected) Modifier.background(SurfaceElevated) else Modifier)
                                .clickable { onSelectModel(model.id) }
                                .padding(LocalAppSpacing.current.large),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = model.displayName, style = AppTypography.bodyLarge, color = TextPrimary)
                                Spacer(Modifier.height(4.dp))
                                ProviderBadge(providerName = provider.name, providerType = provider.type)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Vybráno", tint = Success)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(LocalAppSpacing.current.large))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddProvider() }
                        .padding(LocalAppSpacing.current.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary)
                    Spacer(Modifier.width(LocalAppSpacing.current.medium))
                    Text("Přidat providera / API klíč", style = AppTypography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}
