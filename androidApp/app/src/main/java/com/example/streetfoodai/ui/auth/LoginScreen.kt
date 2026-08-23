package com.example.streetfoodai.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    
    var selectedRole by remember { mutableStateOf("vendor") }

    var customMockId by remember { mutableStateOf("") }
    var mockBusinessName by remember { mutableStateOf("") }
    var showMockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess((authState as AuthState.Success).role)
            viewModel.resetState()
        }
    }

    fun handleGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Set to false so it always shows the picker
            .setAutoSelectEnabled(false)
            .setServerClientId("514114549099-mqmr33666vf1g09eg52tbjbr9b5uu56i.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                val credential = result.credential
                Log.d("LoginScreen", "Received credential type: ${credential.type}")
                
                if (credential is GoogleIdTokenCredential) {
                    viewModel.loginWithGoogle(credential.idToken, selectedRole)
                } else if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    // Fallback for some library versions
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    viewModel.loginWithGoogle(googleIdTokenCredential.idToken, selectedRole)
                } else {
                    Log.e("LoginScreen", "Unexpected credential type: ${credential.type}")
                    android.widget.Toast.makeText(context, "Unexpected login type", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LoginScreen", "Google Sign-In Error", e)
                val msg = e.message ?: "Unknown Error"
                // This is the important part for troubleshooting
                viewModel.resetState()
                android.widget.Toast.makeText(context, "Google Error: $msg", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "StreetFood AI", style = MaterialTheme.typography.headlineLarge)
        Text(text = "AI Powered Street Food Intelligence", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Role Selector
        Text("I am a:", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selectedRole == "vendor", onClick = { selectedRole = "vendor" })
            Text("Vendor")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(selected = selectedRole == "customer", onClick = { selectedRole = "customer" })
            Text("Customer")
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (authState is AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { handleGoogleSignIn() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Sign in with Google")
            }
        }
        
        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Fix for the error: Use normal login for mocks
        Row {
            TextButton(onClick = { viewModel.loginWithGoogle("mock_momo", "vendor") }) {
                Text("Bypass: Momo", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { showMockDialog = true }) {
                Text("Bypass: Custom ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = { viewModel.loginWithGoogle("mock_customer", "customer") }) {
                Text("Bypass: Customer", style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    if (showMockDialog) {
        AlertDialog(
            onDismissRequest = { showMockDialog = false },
            title = { Text("Multi-Vendor Bypass") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a unique name to create a new session.")
                    OutlinedTextField(
                        value = customMockId,
                        onValueChange = { customMockId = it },
                        label = { Text("Custom Vendor ID") },
                        placeholder = { Text("e.g. prajwal") }
                    )
                    if (selectedRole == "vendor") {
                        OutlinedTextField(
                            value = mockBusinessName,
                            onValueChange = { mockBusinessName = it },
                            label = { Text("Business Name") },
                            placeholder = { Text("e.g. Prajwal's Chai") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    if (customMockId.isNotBlank()) {
                        // Ensure it starts with mock_
                        val token = if (customMockId.startsWith("mock_")) customMockId else "mock_$customMockId"
                        viewModel.loginWithGoogle(token, selectedRole, mockBusinessName.ifBlank { null })
                        showMockDialog = false 
                    }
                }) { Text("Login") }
            }
        )
    }
}
