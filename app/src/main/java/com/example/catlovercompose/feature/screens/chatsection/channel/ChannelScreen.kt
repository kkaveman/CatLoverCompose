package com.example.catlovercompose.feature.screens.chatsection.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.catlovercompose.core.model.getOtherParticipant
import com.example.catlovercompose.core.util.AuthState
import com.example.catlovercompose.navigation.NavDestinations
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    navController: NavController,
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val currentUserId = AuthState.getCurrentUser()?.uid ?: ""

    // Add Channel Dialog
    if (uiState.showAddChannelDialog) {
        AddChannelDialog(
            onDismiss = { viewModel.toggleAddChannelDialog(false) },
            onSearchEmail = { email -> viewModel.searchUserByEmail(email) },
            searchedUser = uiState.searchedUser,
            isSearching = uiState.isSearchingUser,
            isCreating = uiState.isCreatingChannel,
            error = uiState.userSearchError,
            onCreateChannel = { userId ->
                viewModel.createChannel(userId) { channelId ->
                    navController.navigate("${NavDestinations.Chat.route}/$channelId")
                }
            },
            onClearUser = { viewModel.clearSearchedUser() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAddChannelDialog(true) }) {
                        Icon(Icons.Default.Add, contentDescription = "New Message")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.filteredChannels.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start a conversation by tapping the + button",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Channel list
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredChannels) { channel ->
                        val otherUserId = channel.participantIds.firstOrNull { it != currentUserId }

                        // ✅ Use fresh user profile data instead of cached participantData
                        val otherUser = otherUserId?.let { uiState.userProfiles[it] }
                        val fallbackData = channel.getOtherParticipant(currentUserId)

                        ChannelItem(
                            username = otherUser?.username ?: fallbackData?.username ?: "Unknown",
                            email = otherUser?.email ?: fallbackData?.email ?: "",
                            profileImageUrl = otherUser?.profileImageUrl
                                ?: fallbackData?.profileImageUrl, // ✅ Fresh data
                            lastMessage = channel.lastMessage,
                            lastMessageTime = channel.lastMessageTime,
                            onClick = {
                                navController.navigate("${NavDestinations.Chat.route}/${channel.id}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    username: String,
    email: String,
    profileImageUrl: String?,
    lastMessage: String,
    lastMessageTime: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture
        if (profileImageUrl != null) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User info and message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (lastMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Time
        if (lastMessageTime > 0) {
            Text(
                text = formatTime(lastMessageTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChannelDialog(
    onDismiss: () -> Unit,
    onSearchEmail: (String) -> Unit,
    searchedUser: com.example.catlovercompose.core.model.UserProfile?,
    isSearching: Boolean,
    isCreating: Boolean,
    error: String?,
    onCreateChannel: (String) -> Unit,
    onClearUser: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Message") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search for a user by email address")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email") },
                    placeholder = { Text("user@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search button
                Button(
                    onClick = { onSearchEmail(emailInput) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = emailInput.isNotBlank() && !isSearching
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Search")
                    }
                }

                // Error message
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Search result
                if (searchedUser != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (searchedUser.profileImageUrl != null) {
                            AsyncImage(
                                model = searchedUser.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = searchedUser.username.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = searchedUser.username,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = searchedUser.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onCreateChannel(searchedUser.uid) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Start Chat")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}