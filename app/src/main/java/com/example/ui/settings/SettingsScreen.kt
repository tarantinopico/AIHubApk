package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.example.ui.theme.TextMuted
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ui.components.AppSwitch
import com.example.ui.components.gradientBackground
import com.example.ui.theme.AppTypography
import com.example.ui.theme.Background
import com.example.ui.theme.LocalAppShapes
import com.example.ui.theme.LocalAppSpacing
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showExportDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showImportDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var password by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var exportDataStr by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    if (state.backupMessage != null) {
        androidx.compose.runtime.LaunchedEffect(state.backupMessage) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
        AlertDialog(
            onDismissRequest = { viewModel.clearBackupMessage() },
            title = { Text("Výsledek", color = TextPrimary) },
            text = { Text(state.backupMessage ?: "", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearBackupMessage() }) {
                    Text("OK", color = com.example.ui.theme.GradientMiddle)
                }
            },
            containerColor = SurfaceElevated
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false; password = ""; exportDataStr = null },
            title = { Text("Exportovat zálohu", color = TextPrimary) },
            text = {
                Column {
                    Text("Zadejte heslo pro zašifrování dat (Klíče, nastavení).", color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    com.example.ui.components.AppTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Heslo",
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    if (exportDataStr != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Data exportována. Zkopírujte si je:", color = TextPrimary)
                        Text(exportDataStr!!.take(20) + "...", style = AppTypography.labelSmall, color = TextMuted)
                        Button(onClick = { 
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exportDataStr!!)) 
                        }) {
                            Text("Kopírovat do schránky")
                        }
                    }
                }
            },
            confirmButton = {
                if (exportDataStr == null) {
                    TextButton(onClick = { 
                        if (password.isNotEmpty()) {
                            exportDataStr = viewModel.exportData(password)
                        }
                    }) {
                        Text("Šifrovat", color = com.example.ui.theme.GradientMiddle)
                    }
                } else {
                    TextButton(onClick = { showExportDialog = false; password = ""; exportDataStr = null }) {
                        Text("Zavřít", color = com.example.ui.theme.GradientMiddle)
                    }
                }
            },
            dismissButton = {
                if (exportDataStr == null) {
                    TextButton(onClick = { showExportDialog = false; password = "" }) {
                        Text("Zrušit", color = TextMuted)
                    }
                }
            },
            containerColor = SurfaceElevated
        )
    }

    if (showImportDialog) {
        var importDataStr by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false; password = ""; importDataStr = "" },
            title = { Text("Obnovit ze zálohy", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vložte zašifrovaná data a heslo.", color = TextSecondary)
                    com.example.ui.components.AppTextField(
                        value = importDataStr,
                        onValueChange = { importDataStr = it },
                        placeholder = "Zašifrovaná data"
                    )
                    com.example.ui.components.AppTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Heslo",
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (password.isNotEmpty() && importDataStr.isNotEmpty()) {
                        viewModel.importData(importDataStr, password)
                        showImportDialog = false
                        password = ""
                        importDataStr = ""
                    }
                }) {
                    Text("Obnovit", color = com.example.ui.theme.GradientMiddle)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; password = ""; importDataStr = "" }) {
                    Text("Zrušit", color = TextMuted)
                }
            },
            containerColor = SurfaceElevated
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nastavení vzhledu", style = AppTypography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(LocalAppSpacing.current.large),
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.large)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(LocalAppSpacing.current.large),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tmavý režim", style = AppTypography.titleMedium, color = TextPrimary)
                        Text("Použít tmavé barvy v celé aplikaci", style = AppTypography.bodySmall, color = TextSecondary)
                    }
                    AppSwitch(
                        checked = state.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }
            }

            Text("Záloha a obnova", style = AppTypography.labelLarge, color = TextPrimary)
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(LocalAppSpacing.current.large)) {
                    Text("Šifrovaný export API klíčů a nastavení, např. pro přenos do jiného zařízení.", style = AppTypography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showExportDialog = true }, modifier = Modifier.weight(1f)) {
                            Text("Exportovat")
                        }
                        Button(onClick = { showImportDialog = true }, modifier = Modifier.weight(1f)) {
                            Text("Importovat")
                        }
                    }
                }
            }

            Text("Náhled aktivního gradientu", style = AppTypography.labelLarge, color = TextPrimary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .gradientBackground(LocalAppShapes.current.large),
                contentAlignment = Alignment.Center
            ) {
                Text("AI Hub Gradient", style = AppTypography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}
