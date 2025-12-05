package com.example.catlovercompose.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsState()

    // Local edit state
    var isEditing by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf("") }
    var editedBio by remember { mutableStateOf("") }
    var editedAge by remember { mutableStateOf("") }
    var editedGender by remember { mutableStateOf(2) }

    // Update local state when profile loads
    LaunchedEffect(uiState.username, uiState.bio, uiState.age, uiState.gender) {
        if (!isEditing) {
            editedUsername = uiState.username
            editedBio = uiState.bio?.toString() ?: ""
            editedAge = uiState.age?.toString() ?: ""
            editedGender = uiState.gender
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        // Cancel button
                        TextButton(onClick = {
                            isEditing = false
                            editedUsername = uiState.username
                            editedAge = uiState.age?.toString() ?: ""
                            editedGender = uiState.gender
                        }) {
                            Text("Cancel")
                        }
                    } else {
                        // Edit button
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Picture Section
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (uiState.profileImageUrl != null) {
                        AsyncImage(
                            model = uiState.profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(enabled = isEditing) {
                                    if (isEditing) imagePickerLauncher.launch("image/*")
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(enabled = isEditing) {
                                    if (isEditing) imagePickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = editedUsername.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Edit Icon (only show in edit mode)
                    if (isEditing) {
                        FloatingActionButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.size(40.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change Profile Picture",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Loading indicator for image upload
                if (uiState.isUploadingImage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Uploading image...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Profile Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Username Field
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedUsername,
                                onValueChange = { editedUsername = it },
                                label = { Text("Username") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            ListItem(
                                headlineContent = { Text(uiState.username) },
                                overlineContent = { Text("Username") },
                                leadingContent = { Icon(Icons.Default.Person, null) }
                            )
                        }

                        //bio

                        if (isEditing) {
                            OutlinedTextField(
                                value = editedBio,
                                onValueChange = { editedBio = it },
                                label = { Text("Bio (optional)") },
                                leadingIcon = { Icon(Icons.Default.Face, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                placeholder = { Text("Write an interesting fact about you") }
                            )
                        } else {
                            ListItem(
                                headlineContent = {
                                    Text(uiState.bio ?: "Not set")
                                },
                                overlineContent = { Text("Bio") },
                                leadingContent = { Icon(Icons.Default.Face, null) }
                            )
                        }


                        Spacer(modifier = Modifier.height(8.dp))

                        // Email (read-only)
                        ListItem(
                            headlineContent = { Text(uiState.email) },
                            overlineContent = { Text("Email") },
                            leadingContent = { Icon(Icons.Default.Email, null) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Age Field
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedAge,
                                onValueChange = {
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        editedAge = it
                                    }
                                },
                                label = { Text("Age (optional)") },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("Enter your age") }
                            )
                        } else {
                            ListItem(
                                headlineContent = {
                                    Text(uiState.age?.toString() ?: "Not set")
                                },
                                overlineContent = { Text("Age") },
                                leadingContent = { Icon(Icons.Default.DateRange, null) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Gender Field
                        if (isEditing) {
                            Text(
                                text = "Gender",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = editedGender == 0,
                                    onClick = { editedGender = 0 },
                                    label = { Text("Male") },
                                    leadingIcon = if (editedGender == 0) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = editedGender == 1,
                                    onClick = { editedGender = 1 },
                                    label = { Text("Female") },
                                    leadingIcon = if (editedGender == 1) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = editedGender == 2,
                                    onClick = { editedGender = 2 },
                                    label = { Text("Other") },
                                    leadingIcon = if (editedGender == 2) {
                                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        when (uiState.gender) {
                                            0 -> "Male"
                                            1 -> "Female"
                                            else -> "Other"
                                        }
                                    )
                                },
                                overlineContent = { Text("Gender") },
                                leadingContent = { Icon(Icons.Default.Face, null) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error message
                if (uiState.error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons
                if (isEditing) {
                    // Save Button
                    Button(
                        onClick = {
                            val age = editedAge.toIntOrNull()
                            viewModel.updateProfile(
                                username = editedUsername,
                                bio = editedBio,
                                age = age,
                                gender = editedGender
                            )
                            isEditing = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editedUsername.isNotBlank() && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Save Changes")
                    }
                } else {
                    // Sign Out Button
                    OutlinedButton(
                        onClick = { viewModel.signOut(navController) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Full screen loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading profile...")
                        }
                    }
                }
            }
        }
    }
}