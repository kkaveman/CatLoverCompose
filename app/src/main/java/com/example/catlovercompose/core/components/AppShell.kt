package com.example.catlovercompose.core.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.catlovercompose.core.util.AuthState
import com.example.catlovercompose.feature.community.CommunityScreen
import com.example.catlovercompose.feature.event.EventScreen
import com.example.catlovercompose.feature.screens.admin.AdminScreen
import com.example.catlovercompose.feature.screens.home.HomeScreen
import com.example.catlovercompose.feature.screens.screening.ScreeningScreen
import com.example.catlovercompose.feature.screens.settings.SettingsScreen
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


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(mainNavController: NavController) {
    val shellNavController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by shellNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination


    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Event,
        BottomNavItem.Community,
        BottomNavItem.Screening
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    //add new navigations here if
                    // only want to put in drawer + topbar.. topbar should be
                    // in the screen.kt file
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(NavDestinations.Settings.route) {
                            launchSingleTop = true
                        }
                    },

                    onNavigateToProfile = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(NavDestinations.Profile.route)
                    },
                    onNavigateToAdmin = {
                        scope.launch { drawerState.close() }
                        mainNavController.navigate(NavDestinations.Admin.route)
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
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cat Lover") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { mainNavController.navigate(NavDestinations.Channel.route) }) {
                            Icon(Icons.Default.Email, contentDescription = "Chat")
                        }
                    }
                )
            },
            bottomBar = {
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
        ) { paddingValues ->
            NavHost(
                navController = shellNavController,
                startDestination = NavDestinations.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(NavDestinations.Home.route) {
                    HomeScreen(mainNavController)
                }
                composable(NavDestinations.Event.route) {
                    EventScreen()
                }
                composable(NavDestinations.Community.route) {
                    CommunityScreen()
                }
                composable(NavDestinations.Screening.route) {
                    ScreeningScreen()
                }
                composable(NavDestinations.Settings.route) {
                    SettingsScreen(mainNavController)
                }

                composable(NavDestinations.Admin.route) {
                    AdminScreen(mainNavController)
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
    var isSignedIn = AuthState.isUserSignedIn()
    var isAdmin by remember { mutableStateOf(false) }

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

        if(isSignedIn) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("Profile") },
                selected = false,
                onClick = {onNavigateToProfile()}
            )
        }

        Divider()

        // Menu Items


        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = onNavigateToSettings
        )


        LaunchedEffect(isSignedIn) {
            if (isSignedIn) {
                isAdmin = AuthState.isAdmin() // suspend OK here
            }
        }

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
            label = { if(isSignedIn) Text("Sign Out") else Text("Sign In")},
            selected = false,
            onClick = onSignOut
        )
    }
}

