package com.example.streetfoodai.ui.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import com.example.streetfoodai.ui.components.WeeklyRevenueChart
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDashboardScreen(
    viewModel: VendorViewModel,
    onNavigateToMenu: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToRecommendations: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLocationPicker: () -> Unit,
    onNavigateToReviews: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val analytics by viewModel.analytics.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val vendorStatus by viewModel.vendorStatus.collectAsState()
    val isOnboardingRequired by viewModel.isOnboardingRequired.collectAsState()

    LaunchedEffect(isOnboardingRequired) {
        if (isOnboardingRequired) {
            onNavigateToOnboarding()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendor Dashboard") },
                actions = {
                    IconButton(onClick = { 
                        viewModel.getMenu()
                        viewModel.getAnalytics()
                        viewModel.getRecommendations()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (vendorStatus == "open") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Shop is ${vendorStatus.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (vendorStatus == "open") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text("Customers can see you on map", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = vendorStatus == "open",
                        onCheckedChange = { isOpen ->
                            viewModel.updateLocation(12.9348, 77.6240, if (isOpen) "open" else "closed")
                        }
                    )
                }
            }

            // Summary Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Business Performance", style = MaterialTheme.typography.titleMedium)
                        var expanded by remember { mutableStateOf(false) }
                        var selectedRange by remember { mutableStateOf("Weekly") }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(selectedRange)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { range ->
                                    DropdownMenuItem(
                                        text = { Text(range) },
                                        onClick = {
                                            selectedRange = range
                                            expanded = false
                                            viewModel.getAnalytics(range.lowercase())
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Revenue", style = MaterialTheme.typography.bodySmall)
                            Text("₹${analytics?.profit_margin?.total_revenue?.toInt() ?: 0}", style = MaterialTheme.typography.headlineSmall)
                        }
                        Column {
                            Text("Net Profit", style = MaterialTheme.typography.bodySmall)
                            Text("₹${analytics?.profit_margin?.net_profit?.toInt() ?: 0}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Weekly Chart
            analytics?.let { data ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        WeeklyRevenueChart(data = data.weekly_revenue)
                    }
                }
                
                // Best Sellers
                if (data.category_distribution.isNotEmpty()) {
                    Text("Top Categories", style = MaterialTheme.typography.titleLarge)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            for (cat in data.category_distribution) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                                    Text("₹${cat.sales.toInt()}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                if (uiState is VendorUiState.Loading) CircularProgressIndicator()
                else Text("No analytics data available", style = MaterialTheme.typography.bodySmall)
            }

            Text("Quick Actions", style = MaterialTheme.typography.titleLarge)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardActionCard(
                    title = "New Sale",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onNavigateToBilling,
                    modifier = Modifier.weight(1f)
                )
                DashboardActionCard(
                    title = "Log Expense",
                    icon = Icons.Default.Warning,
                    onClick = onNavigateToExpense,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardActionCard(
                    title = "Reviews",
                    icon = Icons.Default.AccountCircle,
                    onClick = onNavigateToReviews,
                    modifier = Modifier.weight(1f)
                )
                DashboardActionCard(
                    title = "Menu",
                    icon = Icons.Default.List,
                    onClick = onNavigateToMenu,
                    modifier = Modifier.weight(1f)
                )
            }

            DashboardActionCard(
                title = "Update Location",
                icon = Icons.Default.LocationOn,
                onClick = onNavigateToLocationPicker,
                modifier = Modifier.fillMaxWidth()
            )

            // AI Recommendation Card
            Text("AI Business Intelligence", style = MaterialTheme.typography.titleLarge)
            
            if (recommendations == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Data Insufficient. Record more sales to unlock AI insights.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                recommendations?.let { recs ->
                    // 1. Where to move
                    AiInsightSection(
                        title = "Optimal Location",
                        content = "Shift to Hotspot 1 (Score: ${recs.recommendations.firstOrNull()?.score?.toInt() ?: 0}/100) for maximum demand.",
                        icon = Icons.Default.LocationOn
                    )
                    
                    // 2. What to prepare
                    AiInsightSection(
                        title = "Inventory Strategy",
                        content = "High confidence demand spike expected. Check detailed prep quantities.",
                        icon = Icons.Default.ShoppingCart
                    )
                    
                    // 3. Live Demand Signals
                    if (recs.demand_hotspots.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "LIVE ALERT: ${recs.demand_hotspots.size} customers are currently searching for food nearby!",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    // 4. Gemini Advice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Gemini Business Consultant", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = recs.ai_advice, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onNavigateToRecommendations) {
                                Text("View Detailed Strategy")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Feedback Dialogs
    when (uiState) {
        is VendorUiState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUiState() },
                confirmButton = { Button(onClick = { viewModel.resetUiState() }) { Text("OK") } },
                title = { Text("Success") },
                text = { Text((uiState as VendorUiState.Success).message) }
            )
        }
        is VendorUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetUiState() },
                confirmButton = { Button(onClick = { viewModel.resetUiState() }) { Text("OK") } },
                title = { Text("Error") },
                text = { Text((uiState as VendorUiState.Error).message) }
            )
        }
        else -> {}
    }
}

@Composable
fun AiInsightSection(
    title: String,
    content: String,
    icon: ImageVector
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(content, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = title)
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }
    }
}
