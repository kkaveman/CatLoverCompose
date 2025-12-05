package com.example.catlovercompose.feature.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.catlovercompose.core.util.AuthState
import com.example.catlovercompose.navigation.NavDestinations

@Composable
fun HomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp)
        ) {
            item {
                Column {
                    // Profile Navigation Button
                    TextButton(
                        onClick = {
                            // Check if user is signed in
                            if (AuthState.isUserSignedIn()) {
                                navController.navigate(NavDestinations.Profile.route)
                            } else {
                                navController.navigate(NavDestinations.SignIn.route)
                            }
                        }
                    ) {
                        Text(text = "Profile")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Placeholder content
                    Text(
                        text = "Welcome to Cat Lover!",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This is your home feed. Posts and content will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewHomeScreen() {
    HomeScreen(navController = rememberNavController())
}