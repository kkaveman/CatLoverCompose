package com.example.catlovercompose.feature.screens.community.adoption

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.preference.PreferenceManager
import com.example.catlovercompose.core.model.PetShop
import com.example.catlovercompose.core.model.PetShopType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdoptionScreen(
    viewModel: AdoptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var showBottomSheet by remember { mutableStateOf(false) }
    var isListExpanded by remember { mutableStateOf(false) }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        initializeOSMConfiguration(context)
    }

    // Request location when permission is granted
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            getLastLocation(context) { location ->
                viewModel.updateUserLocation(location)
            }
        }
    }

    // ⭐ Simple Box - let parent (CommunityScreen) handle padding
    Box(modifier = Modifier.fillMaxSize()) {
        // Map fills everything
        OSMMapView(
            uiState = uiState,
            onMarkerClick = { petShop ->
                viewModel.selectPetShop(petShop)
                showBottomSheet = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // Filter chips at top
        FilterChips(
            selectedType = uiState.filterType,
            onFilterChange = { viewModel.setFilter(it) },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        // Loading
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Error
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = { viewModel.retry() }) {
                        Text("Retry")
                    }
                }
            ) {
                Text(error)
            }
        }

        // FAB
        if (locationPermissionState.status.isGranted) {
            FloatingActionButton(
                onClick = {
                    getLastLocation(context) { location ->
                        viewModel.updateUserLocation(location)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 180.dp, end = 16.dp)
            ) {
                Icon(Icons.Default.MyLocation, "My Location")
            }
        } else {
            ExtendedFloatingActionButton(
                onClick = { locationPermissionState.launchPermissionRequest() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 180.dp, end = 16.dp),
                icon = { Icon(Icons.Default.LocationOn, null) },
                text = { Text("Enable Location") }
            )
        }

        // List
        if (!uiState.isLoading && uiState.petShops.isNotEmpty()) {
            Box(modifier = Modifier.align(Alignment.BottomStart)) {
                CollapsiblePetShopList(
                    petShops = uiState.petShops,
                    isExpanded = isListExpanded,
                    onToggleExpand = { isListExpanded = !isListExpanded },
                    onPetShopClick = {
                        viewModel.selectPetShop(it)
                        showBottomSheet = true
                    }
                )
            }
        }

        // Bottom Sheet
        if (showBottomSheet && uiState.selectedPetShop != null) {
            PetShopBottomSheet(
                petShop = uiState.selectedPetShop!!,
                onDismiss = {
                    showBottomSheet = false
                    viewModel.selectPetShop(null)
                }
            )
        }
    }
}

private fun initializeOSMConfiguration(context: Context) {
    Configuration.getInstance().load(
        context,
        PreferenceManager.getDefaultSharedPreferences(context)
    )
    Configuration.getInstance().userAgentValue = context.packageName
}

@Composable
private fun OSMMapView(
    uiState: AdoptionUiState,
    onMarkerClick: (PetShop) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                minZoomLevel = 4.0
                maxZoomLevel = 20.0
                controller.setZoom(uiState.mapZoom)
                controller.setCenter(uiState.mapCenter)
                isHorizontalMapRepetitionEnabled = false

                if (uiState.userLocation != null) {
                    val overlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    overlay.enableMyLocation()
                    overlays.add(overlay)
                    locationOverlay = overlay
                }

                mapView = this
            }
        },
        update = { view ->
            view.overlays.removeAll { it is Marker }

            uiState.petShops.forEach { petShop ->
                val marker = Marker(view).apply {
                    position = petShop.location
                    title = petShop.name
                    snippet = "${petShop.type.displayName}\n${petShop.address}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(petShop)
                        true
                    }
                }
                view.overlays.add(marker)
            }

            uiState.selectedPetShop?.let { petShop ->
                view.controller.animateTo(petShop.location)
            }

            view.invalidate()
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            locationOverlay?.disableMyLocation()
            mapView?.onDetach()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChips(
    selectedType: PetShopType?,
    onFilterChange: (PetShopType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("All") },
                    leadingIcon = if (selectedType == null) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }

            items(PetShopType.entries.filter { it != PetShopType.UNKNOWN }) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onFilterChange(if (selectedType == type) null else type) },
                    label = { Text(type.displayName) },
                    leadingIcon = if (selectedType == type) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun CollapsiblePetShopList(
    petShops: List<PetShop>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPetShopClick: (PetShop) -> Unit
) {
    val maxHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isExpanded) 350.dp else 110.dp,
        label = "list_height"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nearby Locations (${petShops.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            if (isExpanded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(petShops) { petShop ->
                        PetShopListItem(
                            petShop = petShop,
                            onClick = { onPetShopClick(petShop) }
                        )
                        if (petShop != petShops.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(4.dp)) {
                    petShops.take(2).forEach { petShop ->
                        PetShopListItem(
                            petShop = petShop,
                            onClick = { onPetShopClick(petShop) },
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PetShopListItem(
    petShop: PetShop,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 6.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getPetShopIcon(petShop.type),
                contentDescription = null,
                modifier = Modifier
                    .size(if (compact) 32.dp else 40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(6.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = petShop.name,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = petShop.type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            petShop.distance?.let { distance ->
                Text(
                    text = String.format("%.1f km", distance),
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetShopBottomSheet(
    petShop: PetShop,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getPetShopIcon(petShop.type),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = petShop.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = petShop.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            DetailRow(
                icon = Icons.Default.LocationOn,
                title = "Address",
                content = petShop.address
            )

            petShop.distance?.let { distance ->
                DetailRow(
                    icon = Icons.Default.Directions,
                    title = "Distance",
                    content = String.format("%.2f km away", distance)
                )
            }

            petShop.phone?.let { phone ->
                DetailRow(
                    icon = Icons.Default.Phone,
                    title = "Phone",
                    content = phone
                )
            }

            petShop.website?.let { website ->
                DetailRow(
                    icon = Icons.Default.Language,
                    title = "Website",
                    content = website
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* TODO: Open in maps */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Directions, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Directions")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getPetShopIcon(type: PetShopType): ImageVector {
    return when (type) {
        PetShopType.PET_SHOP -> Icons.Default.Store
        PetShopType.VETERINARY -> Icons.Default.LocalHospital
        PetShopType.PET_GROOMING -> Icons.Default.ContentCut
        PetShopType.ANIMAL_SHELTER -> Icons.Default.Home
        PetShopType.UNKNOWN -> Icons.Default.Place
    }
}

@SuppressLint("MissingPermission")
private fun getLastLocation(
    context: Context,
    onLocationReceived: (android.location.Location) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        location?.let { onLocationReceived(it) }
    }
}