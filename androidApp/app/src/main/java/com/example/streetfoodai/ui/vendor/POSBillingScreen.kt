package com.example.streetfoodai.ui.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.streetfoodai.data.model.ProductDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSBillingScreen(
    viewModel: VendorViewModel,
    onBack: () -> Unit
) {
    val menu by viewModel.menu.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val cart = viewModel.cart

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Bill") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Product Selection (Left)
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text("Menu", style = MaterialTheme.typography.titleLarge)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(menu) { product ->
                        Card(
                            onClick = { viewModel.addToCart(product) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(product.name)
                                Text("₹${product.price}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Cart (Right)
            Column(modifier = Modifier.weight(1f).padding(8.dp).fillMaxHeight()) {
                Text("Cart", style = MaterialTheme.typography.titleLarge)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart) { (product, qty) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${product.name} x$qty", modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.removeFromCart(product) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total: ₹${viewModel.cartTotal}", style = MaterialTheme.typography.headlineMedium)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.checkout("cash") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cart.isNotEmpty()
                ) {
                    Text("Cash Payment")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.checkout("upi") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cart.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("UPI Payment")
                }
                
                if (uiState is VendorUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }

    // Success/Error handling
    if (uiState is VendorUiState.Success) {
        AlertDialog(
            onDismissRequest = { viewModel.resetUiState() },
            confirmButton = { Button(onClick = { viewModel.resetUiState() }) { Text("OK") } },
            text = { Text((uiState as VendorUiState.Success).message) }
        )
    }
}
