package com.example.catlovercompose.feature.screens.chatsection.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.catlovercompose.navigation.NavDestinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun ChannelScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channel") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Channel")
                    //onclick, this would show a pop up consisting :
                        //an input field as searching feature for email adding a message channel
                        //searches the email in the user firestore (global) for adding a channel in the firestore -> ChannelViewModel
                        //if found, proceed to add a channel with the card corresponding to the account,
                        //show a message of not found
                    }
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            //the channel is started with empty screen, with a
            //should display cards of message channels with
            //the content :username (bigger text), email (smaller text)
            //should have a search channel feature by username
            //every card being tapped should navigate the channel to the chat screen corresponding to the data of the card
            Text(
                text = "Channel Screen",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChannelScreenPreview() {
    val fakeNavController = rememberNavController()
    ChannelScreen(navController = fakeNavController)
}