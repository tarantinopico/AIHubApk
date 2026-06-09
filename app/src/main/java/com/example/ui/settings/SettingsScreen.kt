package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
