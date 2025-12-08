package com.example.catlovercompose.core.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.catlovercompose.core.util.AuthState

import com.example.catlovercompose.feature.screens.home.HomeScreen
import com.example.catlovercompose.feature.event.EventScreen
import com.example.catlovercompose.feature.community.CommunityScreen
import com.example.catlovercompose.feature.screens.screening.ScreeningScreen
import com.example.catlovercompose.feature.screens.finduser.FindUserScreen

import com.example.catlovercompose.feature.screens.admin.AdminScreen
import com.example.catlovercompose.feature.screens.community.postsection.AddPostScreen
import com.example.catlovercompose.feature.screens.event.SingleEventScreen
import com.example.catlovercompose.feature.screens.settings.SettingsScreen

import com.example.catlovercompose.feature.profile.ProfileScreen
import com.example.catlovercompose.feature.profile.EditProfileScreen
import com.example.catlovercompose.feature.profile.OtherProfileScreen
import com.example.catlovercompose.feature.screens.admin.usercrud.SingleUserCRUDScreen
import com.example.catlovercompose.feature.screens.chatsection.channel.ChannelScreen
import com.example.catlovercompose.feature.screens.chatsection.chat.ChatScreen

import com.example.catlovercompose.navigation.NavDestinations
import kotlinx.coroutines.launch

// Bottom Navigation Items
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem(NavDestinations.Home.route, Icons.Default.Home, "Home")
    object Event : BottomNavItem(NavDestinations.Event.route, Icons.Default.DateRange, "Events")
    object Community : BottomNavItem(NavDestinations.Community.route, Icons.Default.ThumbUp, "Community")
    object Screening : BottomNavItem(NavDestinations.Screening.route, Icons.Default.Api, "Screening")
    object FindUser : BottomNavItem(NavDestinations.FindUser.route, Icons.Default.Search, "Find User")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(mainNavController: NavController) {
    val shellNavController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by shellNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute = currentDestination?.route

    // ✅ UPDATED: Check for routes that should hide TopBar and BottomBar
    val shouldHideShellBars = currentRoute?.startsWith("${NavDestinations.SingleEvent.route}/") == true ||
            currentRoute == NavDestinations.Channel.route ||

            currentRoute?.startsWith("${NavDestinations.Chat.route}/") == true ||
            currentRoute == NavDestinations.Profile.route ||
            currentRoute == NavDestinations.EditProfile.route ||

            currentRoute?.startsWith("${NavDestinations.OtherProfile.route}/") == true ||
            currentRoute == NavDestinations.Settings.route ||
            currentRoute == NavDestinations.Admin.route ||

            currentRoute?.startsWith("${NavDestinations.SingleUserCRUD.route}/") == true






    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Event,
        BottomNavItem.Community,
        BottomNavItem.Screening,
        BottomNavItem.FindUser
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        shellNavController.navigate(NavDestinations.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProfile = {
                        scope.launch { drawerState.close() }
                        shellNavController.navigate(NavDestinations.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAdmin = {
                        scope.launch { drawerState.close() }
                        shellNavController.navigate(NavDestinations.Admin.route) {
                            launchSingleTop = true
                        }
                    },
                    onSignOut = {
                        scope.launch { drawerState.close() }
                        AuthState.signOut()
                        mainNavController.navigate(NavDestinations.SignIn.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        },
        // ✅ UPDATED: Disable drawer for all screens with custom TopBar
        gesturesEnabled = !shouldHideShellBars
    ) {
        Scaffold(
            topBar = {
                // ✅ UPDATED: Hide TopBar for screens with custom TopBar
                if (!shouldHideShellBars) {
                    TopAppBar(
                        title = { Text("Cat Lover") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            if (AuthState.isUserSignedIn()) {
                                IconButton(onClick = {
                                    shellNavController.navigate(NavDestinations.Channel.route)
                                }) {
                                    Icon(Icons.Default.Email, contentDescription = "Chat")
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                // ✅ UPDATED: Hide BottomBar for screens with custom TopBar
                if (!shouldHideShellBars) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                onClick = {
                                    shellNavController.navigate(item.route) {
                                        popUpTo(NavDestinations.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = shellNavController,
                startDestination = NavDestinations.Home.route,
                modifier = Modifier.padding(
                    // ✅ UPDATED: Don't apply padding for screens with custom TopBar
                    if (shouldHideShellBars) PaddingValues(0.dp) else paddingValues
                )
            ) {
                // Bottom Nav Screens
                composable(NavDestinations.Home.route) {
                    HomeScreen(mainNavController)
                }
                composable(NavDestinations.Event.route) {
                    EventScreen(shellNavController)
                }
                composable(NavDestinations.Community.route) {
                    CommunityScreen(shellNavController)
                }
                composable(NavDestinations.Screening.route) {
                    ScreeningScreen()
                }
                composable(NavDestinations.FindUser.route) {
                    FindUserScreen(shellNavController)
                }

                // Event Detail
                composable("${NavDestinations.SingleEvent.route}/{eventId}") { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                    SingleEventScreen(
                        navController = shellNavController,
                        eventId = eventId
                    )
                }

                // Post Creation
                composable(NavDestinations.AddPost.route) {
                    AddPostScreen(shellNavController)
                }

                // Profile Screens
                composable(NavDestinations.Profile.route) {
                    ProfileScreen(shellNavController)
                }
                composable(NavDestinations.EditProfile.route) {
                    EditProfileScreen(shellNavController)
                }
                composable("${NavDestinations.OtherProfile.route}/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    OtherProfileScreen(
                        navController = shellNavController,
                        userId = userId
                    )
                }
                composable("${NavDestinations.SingleUserCRUD.route}/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    SingleUserCRUDScreen(shellNavController, userId)
                }

                // Settings & Admin
                composable(NavDestinations.Settings.route) {
                    SettingsScreen(shellNavController)
                }
                composable(NavDestinations.Admin.route) {
                    AdminScreen(shellNavController)
                }

                // Chat Screens
                composable(NavDestinations.Channel.route) {
                    ChannelScreen(shellNavController)
                }
                composable("${NavDestinations.Chat.route}/{channelId}") { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    ChatScreen(
                        navController = shellNavController,
                        channelId = channelId
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    val user = AuthState.getCurrentUser()
    val isSignedIn = AuthState.isUserSignedIn()
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            isAdmin = AuthState.isAdmin()
        }
    }

    Column {
        // User Info Header
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = user?.displayName ?: user?.email?.substringBefore("@") ?: "Guest",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (isSignedIn) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("Profile") },
                selected = false,
                onClick = onNavigateToProfile
            )
        }

        Divider()

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = onNavigateToSettings
        )

        if (isSignedIn && isAdmin) {
            Divider()
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Lock, null) },
                label = { Text("Admin") },
                selected = false,
                onClick = onNavigateToAdmin
            )
        }

        Divider()

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
            label = { if (isSignedIn) Text("Sign Out") else Text("Sign In") },
            selected = false,
            onClick = onSignOut
        )
    }
}