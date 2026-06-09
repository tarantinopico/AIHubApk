package com.example.ui.providers

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.ApiFormat
import com.example.ui.components.GradientButton
import com.example.ui.components.AppTextField
import com.example.ui.theme.AppTypography
import com.example.ui.theme.Background
import com.example.ui.theme.Error
import com.example.ui.theme.LocalAppSpacing
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddProviderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Přidat Providera", style = AppTypography.titleLarge, color = TextPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(LocalAppSpacing.current.medium)
        ) {
            AppTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                placeholder = "Název providera (např. Together AI)"
            )

            Text("Formát API", style = AppTypography.labelLarge, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.apiFormat == ApiFormat.OPENAI_COMPATIBLE,
                    onClick = { viewModel.updateApiFormat(ApiFormat.OPENAI_COMPATIBLE) },
                    label = { Text("OpenAI Compatible") }
                )
                FilterChip(
                    selected = state.apiFormat == ApiFormat.GEMINI,
                    onClick = { viewModel.updateApiFormat(ApiFormat.GEMINI) },
                    label = { Text("Gemini") }
                )
            }

            if (state.apiFormat == ApiFormat.OPENAI_COMPATIBLE) {
                AppTextField(
                    value = state.baseUrl,
                    onValueChange = viewModel::updateBaseUrl,
                    placeholder = "Base URL (např. https://api.together.xyz/v1)"
                )
            }

            AppTextField(
                value = state.apiKey,
                onValueChange = viewModel::updateApiKey,
                placeholder = "API Klíč"
            )

            if (state.error != null) {
                Text(text = state.error!!, color = Error, style = AppTypography.bodyMedium)
            }

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                text = "Uložit Providera",
                onClick = viewModel::saveProvider,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
