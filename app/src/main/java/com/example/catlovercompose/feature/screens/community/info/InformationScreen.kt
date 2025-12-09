package com.example.catlovercompose.feature.screens.community.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.catlovercompose.core.model.CatBreedDetail
import com.example.catlovercompose.core.model.CatImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationScreen(
    viewModel: InformationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cat Information") },
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrentTab() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Random Cat Fact Banner
            uiState.currentFact?.let { fact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Did You Know?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fact,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = { viewModel.loadRandomFact() }) {
                            Icon(Icons.Default.Refresh, "New fact")
                        }
                    }
                }
            }

            // Tabs
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == InfoTab.RANDOM_CATS,
                    onClick = { viewModel.selectTab(InfoTab.RANDOM_CATS) },
                    text = { Text("Random Cats") },
                    icon = { Icon(Icons.Default.PhotoCamera, null) }
                )
                Tab(
                    selected = uiState.selectedTab == InfoTab.BREEDS,
                    onClick = { viewModel.selectTab(InfoTab.BREEDS) },
                    text = { Text("Breeds") },
                    icon = { Icon(Icons.Default.Pets, null) }
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshCurrentTab() }) {
                                Text("Retry")
                            }
                        }
                    }
                    else -> {
                        when (uiState.selectedTab) {
                            InfoTab.RANDOM_CATS -> RandomCatsGrid(
                                images = uiState.randomImages,
                                onLoadMore = { viewModel.loadRandomImages() }
                            )
                            InfoTab.BREEDS -> BreedsListView(
                                breeds = uiState.breeds,
                                onBreedClick = { viewModel.selectBreed(it) }
                            )
                            InfoTab.FACTS -> {} // Facts shown in banner
                        }
                    }
                }
            }
        }

        // Breed Detail Bottom Sheet
        if (uiState.selectedBreed != null) {
            BreedDetailBottomSheet(
                breed = uiState.selectedBreed!!,
                onDismiss = { viewModel.clearSelectedBreed() }
            )
        }
    }
}

@Composable
private fun RandomCatsGrid(
    images: List<CatImage>,
    onLoadMore: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(images) { image ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = image.url,
                    contentDescription = "Random cat",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Load More Button
        item {
            OutlinedButton(
                onClick = onLoadMore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Load More")
            }
        }
    }
}

@Composable
private fun BreedsListView(
    breeds: List<CatBreedDetail>,
    onBreedClick: (CatBreedDetail) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(breeds) { breed ->
            BreedCard(
                breed = breed,
                onClick = { onBreedClick(breed) }
            )
        }
    }
}

@Composable
private fun BreedCard(
    breed: CatBreedDetail,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = breed.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "View details"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Origin and Lifespan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabelValue(
                    label = "Origin",
                    value = breed.origin,
                    icon = "🌍"
                )
                LabelValue(
                    label = "Lifespan",
                    value = "${breed.lifeSpan} years",
                    icon = "⏱️"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = breed.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Temperament chips
            Text(
                text = breed.temperament,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LabelValue(
    label: String,
    value: String,
    icon: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreedDetailBottomSheet(
    breed: CatBreedDetail,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = breed.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { },
                        label = { Text(breed.origin) }
                    )
                }
            }

            item {
                // Description
                Text(
                    text = breed.description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                // Stats
                Text(
                    text = "Characteristics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow("Temperament", breed.temperament)
                    DetailRow("Lifespan", "${breed.lifeSpan} years")
                    DetailRow("Weight", "${breed.weight.metric} kg")

                    breed.intelligence?.let {
                        StatBar("Intelligence", it)
                    }
                    breed.affectionLevel?.let {
                        StatBar("Affection Level", it)
                    }
                    breed.childFriendly?.let {
                        StatBar("Child Friendly", it)
                    }
                    breed.energyLevel?.let {
                        StatBar("Energy Level", it)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$value/5",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value / 5f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}