package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable data object Chat : Destination
    @Serializable data object Settings : Destination
    @Serializable data object Providers : Destination
    @Serializable data object AddProvider : Destination
}
