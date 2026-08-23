package com.example.streetfoodai.ui.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.streetfoodai.data.model.VendorProfileDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorOnboardingScreen(
    viewModel: VendorViewModel,
    onComplete: () -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var whatHeSells by remember { mutableStateOf("") }
    var regularity by remember { mutableStateOf("regular") }
    var isMovable by remember { mutableStateOf(true) }
    var approxSales by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }

    val profile by viewModel.profile.collectAsState()
    
    LaunchedEffect(profile) {
        profile?.let {
            businessName = it.businessName ?: ""
            whatHeSells = it.whatHeSells ?: ""
            regularity = it.regularity ?: "regular"
            isMovable = it.isMovable
            approxSales = it.approxSalesPerDay?.toString() ?: ""
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vendor Setup (Step $step/3)") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                1 -> {
                    Text("Basic Details", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = whatHeSells,
                        onValueChange = { whatHeSells = it },
                        label = { Text("What do you sell? (e.g. Pani Puri, Tea)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.align(Alignment.End),
                        enabled = businessName.isNotBlank() && whatHeSells.isNotBlank()
                    ) { Text("Next") }
                }
                2 -> {
                    Text("Operations", style = MaterialTheme.typography.titleLarge)
                    Text("How often do you sell?", style = MaterialTheme.typography.bodyMedium)
                    val options = listOf("regular", "occasional")
                    Column(Modifier.selectableGroup()) {
                        options.forEach { text ->
                            Row(
                                Modifier.fillMaxWidth().height(48.dp)
                                    .selectable(
                                        selected = (text == regularity),
                                        onClick = { regularity = text },
                                        role = Role.RadioButton
                                    ).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (text == regularity), onClick = null)
                                Text(text.replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isMovable, onCheckedChange = { isMovable = it })
                        Text("Do you have a movable cart?")
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { step = 1 }) { Text("Back") }
                        Button(onClick = { step = 3 }) { Text("Next") }
                    }
                }
                3 -> {
                    Text("Past Data (Optional)", style = MaterialTheme.typography.titleLarge)
                    Text("This helps our AI model give you better advice starting day 1.", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = approxSales,
                        onValueChange = { approxSales = it },
                        label = { Text("Approx sales per day (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                VendorProfileDto(
                                    businessName = businessName,
                                    whatHeSells = whatHeSells,
                                    regularity = regularity,
                                    isMovable = isMovable,
                                    approxSalesPerDay = approxSales.toDoubleOrNull()
                                )
                            )
                            onComplete()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Complete Setup") }
                    
                    TextButton(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                }
            }
        }
    }
}
