package com.example.streetfoodai.ui.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    viewModel: VendorViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pickedLocation by remember { mutableStateOf(GeoPoint(12.9348, 77.6240)) }
    val vendorStatus by viewModel.vendorStatus.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()

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
                title = { Text("Set My Location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.updateLocation(pickedLocation.latitude, pickedLocation.longitude, vendorStatus)
                    onBack()
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirm Location")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(cartoDbTileSource)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        controller.setCenter(pickedLocation)

                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let { pickedLocation = it }
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint?): Boolean { return false }
                        }
                        overlays.add(MapEventsOverlay(eventsReceiver))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    mapView.overlays.removeAll { it is Marker || it is org.osmdroid.views.overlay.Polygon }
                    
                    // 1. Show Demand Hotspots (as circles/special markers)
                    recommendations?.demand_hotspots?.forEach { demand ->
                        val demandMarker = Marker(mapView)
                        demandMarker.position = GeoPoint(demand.latitude, demand.longitude)
                        demandMarker.title = "DEMAND: ${demand.item_name}"
                        demandMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        // Use a distinct icon or color for demand
                        demandMarker.icon = context.getDrawable(android.R.drawable.presence_online) 
                        mapView.overlays.add(demandMarker)
                    }

                    // 2. Show the shop picker marker
                    val marker = Marker(mapView)
                    marker.position = pickedLocation
                    marker.title = "Move My Shop Here"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(marker)
                    
                    mapView.invalidate()
                }
            )
            
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Tap on map to pin your shop location",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
