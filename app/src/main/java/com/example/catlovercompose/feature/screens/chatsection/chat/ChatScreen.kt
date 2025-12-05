package com.example.catlovercompose.feature.screens.chatsection.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.catlovercompose.core.model.Message
import com.example.catlovercompose.core.util.AuthState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    channelId: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val currentUserId = AuthState.getCurrentUser()?.uid ?: ""
    val listState = rememberLazyListState()

    // Initialize chat
    LaunchedEffect(channelId) {
        viewModel.initializeChat(channelId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.otherUserProfileUrl != null) {
                            AsyncImage(
                                model = uiState.otherUserProfileUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.otherUserName.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(uiState.otherUserName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Message input
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = uiState.messageInput,
                        onValueChange = { viewModel.updateMessageInput(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    IconButton(
                        onClick = {
                            viewModel.sendMessage(uiState.messageInput)
                        },
                        enabled = uiState.messageInput.isNotBlank() && !uiState.isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (uiState.messageInput.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (uiState.messageInput.isNotBlank())
                                Color.White
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
            } else if (uiState.messages.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a message to start the conversation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Message list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        MessageBubble(
                            message = message,
                            isFromCurrentUser = message.senderId == currentUserId
                        )
                    }
                }
            }

            // Error snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error!!)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                ),
                color = if (isFromCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = if (isFromCurrentUser)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

fun formatMessageTime(timestamp: Long): String {
    val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()

    return when {
        messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                messageDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        messageDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MMM dd yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}