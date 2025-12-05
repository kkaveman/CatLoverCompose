package com.example.catlovercompose.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.catlovercompose.core.components.AppShell
import com.example.catlovercompose.core.util.AuthState
import com.example.catlovercompose.feature.screens.chatsection.channel.ChannelScreen
import com.example.catlovercompose.feature.auth.signin.SignInScreen
import com.example.catlovercompose.feature.auth.signup.SignUpScreen
import com.example.catlovercompose.feature.profile.ProfileScreen
import com.example.catlovercompose.feature.screens.admin.AdminScreen
import com.example.catlovercompose.feature.screens.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Determine start destination based on auth state
    val startDestination = NavDestinations.AppShell.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth screens
        composable(NavDestinations.SignIn.route) {
            SignInScreen(navController)
        }

        composable(NavDestinations.SignUp.route) {
            SignUpScreen(navController)
        }

        // App Shell (contains bottom nav with Home, Events, Community)
        composable(NavDestinations.AppShell.route) {
            AppShell(mainNavController = navController)
        }

        // Profile screen (separate from bottom nav)
        composable(NavDestinations.Profile.route) {
            ProfileScreen(navController)
        }

        composable(NavDestinations.Admin.route) {
            AdminScreen(navController)
        }

        composable(NavDestinations.Settings.route) {
            SettingsScreen(navController)
        }

        composable(NavDestinations.Channel.route){
            ChannelScreen(navController)
        }

    }
}