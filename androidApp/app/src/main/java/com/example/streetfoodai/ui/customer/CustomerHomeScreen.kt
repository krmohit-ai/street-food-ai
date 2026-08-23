package com.example.streetfoodai.ui.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    viewModel: CustomerViewModel,
    onNavigateToProfile: () -> Unit
) {
    val nearbyVendors by viewModel.nearbyVendors.collectAsState()
    val defaultLocation = remember { GeoPoint(12.9348, 77.6240) } // Koramangala
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val sheetState = rememberModalBottomSheetState()
    var selectedVendor by remember { mutableStateOf<com.example.streetfoodai.data.model.NearbyVendorDto?>(null) }
    var showVendorSheet by remember { mutableStateOf(false) }
    var showDemandDialog by remember { mutableStateOf(false) }

    val cartoDbTileSource = remember {
        XYTileSource(
            "CartoDB_Positron",
            1, 20, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/light_all/",
                "https://b.basemaps.cartocdn.com/light_all/",
                "https://c.basemaps.cartocdn.com/light_all/"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StreetFood AI") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    IconButton(onClick = { viewModel.fetchNearbyVendors(12.9348, 77.6240) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDemandDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Request Food") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(cartoDbTileSource)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        controller.setZoom(16.0)
                        controller.setCenter(defaultLocation)
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_RESUME -> onResume()
                                Lifecycle.Event.ON_PAUSE -> onPause()
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.clear()
                    nearbyVendors.forEach { vendor ->
                        val marker = Marker(mapView)
                        marker.position = GeoPoint(vendor.latitude, vendor.longitude)
                        marker.title = vendor.business_name
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.setOnMarkerClickListener { _, _ ->
                            selectedVendor = vendor
                            viewModel.fetchVendorMenu(vendor.vendor_id.toString())
                            viewModel.fetchVendorReviews(vendor.vendor_id.toString())
                            showVendorSheet = true
                            true
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )
            
            // Search Bar
            Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search for Momos, Chai...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.searchVendors(searchQuery, 12.9348, 77.6240)
                        keyboardController?.hide()
                    }),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    }

    if (showVendorSheet && selectedVendor != null) {
        val menu by viewModel.selectedVendorMenu.collectAsState()
        val reviews by viewModel.selectedVendorReviews.collectAsState()
        var showReviewDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showVendorSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(text = selectedVendor!!.business_name, style = MaterialTheme.typography.headlineMedium)
                Text(text = "Rating: ${selectedVendor!!.rating} ⭐", style = MaterialTheme.typography.bodyLarge)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tabs for Menu and Reviews
                var selectedTab by remember { mutableIntStateOf(0) }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Menu") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reviews") })
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab == 0) {
                    Column {
                        menu.forEach { item ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text(item.description ?: "") },
                                trailingContent = { Text("₹${item.price}") }
                            )
                        }
                        if (menu.isEmpty()) Text("No items listed.", modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Customer Feedback", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { showReviewDialog = true }) {
                                Text("Add Review")
                            }
                        }
                        reviews.forEach { review ->
                            com.example.streetfoodai.ui.vendor.ReviewItem(review = review)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (reviews.isEmpty()) Text("Be the first to review!", modifier = Modifier.padding(16.dp))
                    }
                }

                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, 
                            android.net.Uri.parse("geo:${selectedVendor!!.latitude},${selectedVendor!!.longitude}?q=${selectedVendor!!.business_name}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Get Directions")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showReviewDialog) {
            var rating by remember { mutableIntStateOf(5) }
            var comment by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                title = { Text("Add Review") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rate your experience:")
                        Row {
                            repeat(5) { index ->
                                IconButton(onClick = { rating = index + 1 }) {
                                    Icon(
                                        Icons.Default.Star, 
                                        contentDescription = null,
                                        tint = if (index < rating) Color(0xFFFFB300) else Color.Gray
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Comment (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.postReview(selectedVendor!!.vendor_id.toString(), rating, comment)
                        showReviewDialog = false
                    }) { Text("Post") }
                },
                dismissButton = {
                    TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

    if (showDemandDialog) {
        var demandItem by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDemandDialog = false },
            title = { Text("Request Food Item") },
            text = {
                Column {
                    Text("What are you looking for? We'll notify vendors in this area.")
                    OutlinedTextField(
                        value = demandItem,
                        onValueChange = { demandItem = it },
                        label = { Text("Food Item (e.g. Vada Pav)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createDemand(demandItem, 12.9348, 77.6240)
                    showDemandDialog = false
                    android.widget.Toast.makeText(context, "Demand sent to nearby vendors!", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Submit") }
            }
        )
    }
}
