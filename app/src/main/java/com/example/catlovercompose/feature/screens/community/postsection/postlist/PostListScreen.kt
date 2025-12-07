package com.example.catlovercompose.feature.screens.community.postsection.postlist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.catlovercompose.core.util.AuthState
import com.example.catlovercompose.feature.screens.community.postsection.PostCard
@Composable

fun PostListScreen(
    navController: NavController,
    viewModel: PostListViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Show delete confirmation dialog
    if (uiState.showDeleteDialog) {
        DeletePostDialog(
            onConfirm = {
                uiState.postToDelete?.let { postId ->
                    viewModel.deletePost(postId)
                }
                viewModel.hideDeleteDialog()
            },
            onDismiss = { viewModel.hideDeleteDialog() }
        )
    }

    // Show error toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.posts.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No posts yet",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Be the first to share something!",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            // Post List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = uiState.posts,
                    key = { post -> post.id }
                ) { post ->
                    val isLiked = post.likedBy.contains(viewModel.getCurrentUserId())
                    PostCard(
                        post = post,
                        currentUserId = viewModel.getCurrentUserId(),
                        isLiked = isLiked,
                        onLikeClick = {
                            if (AuthState.isUserSignedIn()) {
                                viewModel.toggleLike(post.id, isLiked)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please sign in to like posts",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onCommentClick = {
                            // TODO: Navigate to comment screen
                            Toast.makeText(
                                context,
                                "Comments feature coming soon!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onDeleteClick = {
                            viewModel.showDeleteDialog(post.id)
                        }
                    )
                }
            }
        }

        // Floating Action Button to Add Post
        FloatingActionButton(
            onClick = {
                if (AuthState.isUserSignedIn()) {
                    navController.navigate("addpost")
                } else {
                    // Navigate to sign in screen
                    Toast.makeText(
                        context,
                        "Please sign in to create posts",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.navigate("signin")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Post",
                tint = Color.White
            )
        }
    }
}

@Composable
fun DeletePostDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Post?") },
        text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}