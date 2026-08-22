package com.example.streetfoodai.ui.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseLoggingScreen(
    viewModel: VendorViewModel,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("raw_materials") }
    
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₹)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What did you buy?") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", modifier = Modifier.align(Alignment.Start))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val categories = listOf("raw_materials", "gas", "rent", "other")
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.replace("_", " ").capitalize()) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is VendorUiState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { 
                        viewModel.logExpense(amount.toDoubleOrNull() ?: 0.0, description, category)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Expense")
                }
            }
        }
    }

    if (uiState is VendorUiState.Success) {
        AlertDialog(
            onDismissRequest = { viewModel.resetUiState() },
            confirmButton = { Button(onClick = { viewModel.resetUiState(); onBack() }) { Text("Done") } },
            text = { Text((uiState as VendorUiState.Success).message) }
        )
    }
}
