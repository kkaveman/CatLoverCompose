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

    object FindUser : NavDestinations("finduser")
    object OtherProfile : NavDestinations("otherprofile")


    object Profile : NavDestinations("profile")

    object EditProfile : NavDestinations("editprofile")

    object Settings : NavDestinations("settings")

    // App shell route (wrapper for bottom nav screens)
    object AppShell : NavDestinations("app_shell")

    object Admin : NavDestinations("admin")
    object EventCRUD : NavDestinations("eventcrud")

    object Channel : NavDestinations("channel")
    object Chat : NavDestinations("chat") // chat/{channelId}

    object AddPost : NavDestinations("addpost")


    object SingleEvent : NavDestinations("singleevent") // singleevent/{eventId}
}