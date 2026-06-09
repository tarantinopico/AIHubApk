package com.example.ui.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.AiModel
import com.example.domain.model.ApiKey
import com.example.domain.model.Provider
import com.example.ui.components.AppSwitch
import com.example.ui.components.AppTextField
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddProvider: () -> Unit,
    viewModel: ProvidersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provideři & Klíče", style = AppTypography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddProvider) {
                        Icon(Icons.Default.Add, contentDescription = "Přidat providera", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { innerPadding ->
        if (state.isLoading && state.providers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GradientMiddle)
            }
        } else if (state.providers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp).padding(bottom = 16.dp))
                    Text("Zatím žádní provideři", style = AppTypography.titleMedium, color = TextPrimary)
                    Text("Přidejte vlastního providera klávesou + nahoře", style = AppTypography.bodyMedium, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = LocalAppSpacing.current.large),
                verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.medium)
            ) {
                items(state.providers, key = { it.provider.id }) { providerDetails ->
                    ProviderCard(
                        details = providerDetails,
                        isExpanded = state.expandedProviderId == providerDetails.provider.id,
                        onExpandClick = { viewModel.toggleProviderExpansion(providerDetails.provider.id) },
                        onEnableToggle = { enabled -> viewModel.toggleProviderEnabled(providerDetails.provider, enabled) },
                        onRotationToggle = { enabled -> viewModel.toggleKeyRotation(providerDetails.provider, enabled) },
                        onAddKeyClick = { viewModel.showAddKeyDialog(providerDetails.provider.id) },
                        onDeleteKey = { key -> viewModel.deleteApiKey(key) },
                        onSetPrimaryKey = { keyId -> viewModel.setPrimaryApiKey(providerDetails.provider.id, keyId) },
                        onTestKey = { key -> viewModel.testApiKey(providerDetails.provider, key) },
                        onLoadModels = { viewModel.loadModelsForProvider(providerDetails.provider) },
                        onAddManualModel = { viewModel.showAddModelDialog(providerDetails.provider.id) },
                        onDeleteModel = { model -> viewModel.deleteModel(model) },
                        onToggleFavoriteModel = { model -> viewModel.toggleModelFavorite(model) }
                    )
                }
            }
        }

        // Add Key Dialog
        if (state.showAddKeyDialogForProviderId != null) {
            var label by remember { mutableStateOf("") }
            var keyValue by remember { mutableStateOf("") }
            var isPrimary by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { viewModel.hideAddKeyDialog() },
                title = { Text("Přidat API klíč", style = AppTypography.titleLarge, color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(
                            value = label,
                            onValueChange = { label = it },
                            placeholder = "Název klíče (např. Hlavní, Projekt X)"
                        )
                        AppTextField(
                            value = keyValue,
                            onValueChange = { keyValue = it },
                            placeholder = "API klíč (skryté po uložení)"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppSwitch(checked = isPrimary, onCheckedChange = { isPrimary = it })
                            Spacer(Modifier.width(8.dp))
                            Text("Nastavit jako hlavní", color = TextSecondary)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.addApiKey(state.showAddKeyDialogForProviderId!!, label, keyValue, isPrimary) }) {
                        Text("Uložit", color = GradientMiddle)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideAddKeyDialog() }) {
                        Text("Zrušit", color = TextSecondary)
                    }
                },
                containerColor = SurfaceElevated
            )
        }

        // Add Model Dialog
        if (state.showAddModelDialogForProviderId != null) {
            var idName by remember { mutableStateOf("") }
            var displayName by remember { mutableStateOf("") }
            var contextLen by remember { mutableStateOf("8192") }
            var supportsImages by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { viewModel.hideAddModelDialog() },
                title = { Text("Přidat Model", style = AppTypography.titleLarge, color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppTextField(value = idName, onValueChange = { idName = it }, placeholder = "ID Modelu (např. gpt-4)")
                        AppTextField(value = displayName, onValueChange = { displayName = it }, placeholder = "Zobrazovaný Název")
                        AppTextField(value = contextLen, onValueChange = { contextLen = it }, placeholder = "Kontext (tokeny)")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppSwitch(checked = supportsImages, onCheckedChange = { supportsImages = it })
                            Spacer(Modifier.width(8.dp))
                            Text("Podporuje obrázky", color = TextSecondary)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.addManualModel(
                            state.showAddModelDialogForProviderId!!, 
                            idName, 
                            displayName.ifBlank { idName }, 
                            contextLen.toIntOrNull() ?: 8192, 
                            supportsImages
                        ) 
                    }) {
                        Text("Uložit", color = GradientMiddle)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideAddModelDialog() }) {
                        Text("Zrušit", color = TextSecondary)
                    }
                },
                containerColor = SurfaceElevated
            )
        }

        // Testing result dialog
        if (state.testingKeyResult != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearTestResult() },
                title = { Text("Výsledek testu", style = AppTypography.titleLarge, color = TextPrimary) },
                text = { Text(state.testingKeyResult!!, color = TextPrimary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearTestResult() }) {
                        Text("OK", color = GradientMiddle)
                    }
                },
                containerColor = SurfaceElevated
            )
        }
    }
}

@Composable
fun ProviderCard(
    details: ProviderWithDetails,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onEnableToggle: (Boolean) -> Unit,
    onRotationToggle: (Boolean) -> Unit,
    onAddKeyClick: () -> Unit,
    onDeleteKey: (ApiKey) -> Unit,
    onSetPrimaryKey: (String) -> Unit,
    onTestKey: (ApiKey) -> Unit,
    onLoadModels: () -> Unit,
    onAddManualModel: () -> Unit,
    onDeleteModel: (AiModel) -> Unit,
    onToggleFavoriteModel: (AiModel) -> Unit
) {
    val provider = details.provider
    Card(
        modifier = Modifier.fillMaxWidth().clip(LocalAppShapes.current.medium),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(LocalAppSpacing.current.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = provider.name, style = AppTypography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(text = "${details.keys.size} klíčů • ${details.models.size} modelů", style = AppTypography.labelMedium, color = TextSecondary)
                }
                AppSwitch(
                    checked = provider.isEnabled,
                    onCheckedChange = { onEnableToggle(it) } // Allow toggling directly
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Rozbalit",
                    tint = TextMuted
                )
            }

            if (isExpanded) {
                HorizontalDivider(color = SurfaceElevated)
                Column(modifier = Modifier.padding(LocalAppSpacing.current.large)) {
                    // Keys Section
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("API Klíče", style = AppTypography.titleSmall, color = GradientMiddle)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rotace klíčů", style = AppTypography.labelMedium, color = TextSecondary)
                            Spacer(Modifier.width(4.dp))
                            AppSwitch(checked = provider.keyRotationEnabled, onCheckedChange = onRotationToggle)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    details.keys.forEach { key ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Background, RoundedCornerShape(8.dp)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(key.label, style = AppTypography.bodyMedium, color = TextPrimary)
                                    if (key.isPrimary) {
                                        Spacer(Modifier.width(4.dp))
                                        Badge(containerColor = GradientMiddle) { Text("Hlavní", color = Color.White) }
                                    }
                                }
                                Text("•••• •••• ••••", style = AppTypography.labelSmall, color = TextMuted)
                            }
                            IconButton(onClick = { onTestKey(key) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Build, contentDescription = "Otestovat", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onSetPrimaryKey(key.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Star, contentDescription = "Nastavit jako hlavní", tint = if (key.isPrimary) GradientMiddle else TextMuted, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteKey(key) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = Error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    TextButton(onClick = onAddKeyClick) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Přidat další klíč")
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    // Models section
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Modely", style = AppTypography.titleSmall, color = GradientMiddle)
                        Row {
                            TextButton(onClick = onLoadModels) {
                                Text("Načíst z API")
                            }
                            TextButton(onClick = onAddManualModel) {
                                Text("Přidat")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    details.models.forEach { model ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.displayName, style = AppTypography.bodyMedium, color = TextPrimary)
                                Text("${model.contextLength} tokens ${if (model.supportsImages) "• Obrázky" else ""}", style = AppTypography.labelSmall, color = TextMuted)
                            }
                            IconButton(onClick = { onToggleFavoriteModel(model) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    if (model.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                                    contentDescription = "Oblíbené", 
                                    tint = if (model.isFavorite) GradientMiddle else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteModel(model) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = Error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
