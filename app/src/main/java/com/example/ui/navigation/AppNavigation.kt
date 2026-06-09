package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.chat.ChatScreen
import com.example.ui.providers.AddProviderScreen
import com.example.ui.providers.ProvidersScreen
import com.example.ui.settings.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Destination.Chat,
        modifier = modifier
    ) {
        composable<Destination.Chat> {
            ChatScreen(
                onNavigateToSettings = { navController.navigate(Destination.Settings) },
                onNavigateToProviders = { navController.navigate(Destination.Providers) }
            )
        }
        composable<Destination.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Destination.Providers> {
            ProvidersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProvider = { navController.navigate(Destination.AddProvider) }
            )
        }
        composable<Destination.AddProvider> {
            AddProviderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
