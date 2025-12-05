package com.example.catlovercompose.navigation

// Type-safe navigation routes
sealed class NavDestinations(val route: String) {
    // Auth routes
    object SignIn : NavDestinations("signin")
    object SignUp : NavDestinations("signup")

    // Main app routes (requires authentication)
    object Home : NavDestinations("home")
    object Event : NavDestinations("event")
    object Community : NavDestinations("community")

    object Screening : NavDestinations("screening")

    object Profile : NavDestinations("profile")
    object Settings : NavDestinations("settings")

    // App shell route (wrapper for bottom nav screens)
    object AppShell : NavDestinations("app_shell")

    object Admin : NavDestinations("admin")

    object Channel : NavDestinations("channel")
    object Chat : NavDestinations("chat") // chat/{channelId}
}