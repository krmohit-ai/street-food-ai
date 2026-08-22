package com.example.streetfoodai.ui.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.streetfoodai.data.model.LocationRecommendation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationDetailScreen(
    viewModel: VendorViewModel,
    onBack: () -> Unit
) {
    val recommendations by viewModel.recommendations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Strategy Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Hotspot Recommendations", style = MaterialTheme.typography.headlineSmall)
                Text("Ranked by highest profit potential", style = MaterialTheme.typography.bodySmall)
            }

            recommendations?.let { recs ->
                items(recs.recommendations) { hotspot ->
                    HotspotCard(hotspot)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Demand Details", style = MaterialTheme.typography.headlineSmall)
                    recs.recommendations.firstOrNull()?.let { top ->
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Projected Demand: ₹${top.forecasted_demand.toInt()}", style = MaterialTheme.typography.titleMedium)
                                Text("Competition Level: ${if (top.active_competitors == 0) "Zero" else "Low"}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Advice (Gemini)", style = MaterialTheme.typography.headlineSmall)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = recs.ai_advice, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } ?: item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Data Insufficient", style = MaterialTheme.typography.titleLarge)
                    Text("The AI needs more transaction history to generate a moving strategy.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun HotspotCard(hotspot: LocationRecommendation) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Hotspot Zone (${hotspot.grid_x}, ${hotspot.grid_y})", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(text = "${hotspot.distance_km} km away", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Score: ${hotspot.score.toInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${hotspot.active_competitors} Competitors",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
