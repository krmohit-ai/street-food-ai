package com.example.streetfoodai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.streetfoodai.ui.auth.AuthViewModel
import com.example.streetfoodai.ui.auth.LoginScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            
            LaunchedEffect(Unit) {
                splashViewModel.events.collect { event ->
                    when (event) {
                        is SplashEvent.NavigateToHome -> {
                            val destination = if (event.role == "vendor") Screen.VendorHome.route else Screen.CustomerHome.route
                            navController.navigate(destination) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                        is SplashEvent.NavigateToLogin -> {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                }
            }

            // Splash UI
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("StreetFood AI", style = MaterialTheme.typography.headlineLarge)
            }
        }
        
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { role ->
                    val destination = if (role == "vendor") Screen.VendorHome.route else Screen.CustomerHome.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // navController.navigate(Screen.Register.route)
                }
            )
        }
        
        // Register screen removed as we use Google Auth only
        
        composable(Screen.VendorHome.route) {
            val vendorViewModel: com.example.streetfoodai.ui.vendor.VendorViewModel = hiltViewModel()
            com.example.streetfoodai.ui.vendor.VendorDashboardScreen(
                viewModel = vendorViewModel,
                onNavigateToMenu = { navController.navigate(Screen.VendorMenu.route) },
                onNavigateToBilling = { navController.navigate(Screen.VendorBilling.route) },
                onNavigateToExpense = { navController.navigate(Screen.VendorExpense.route) },
                onNavigateToRecommendations = { navController.navigate(Screen.VendorRecommendations.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToLocationPicker = { navController.navigate(Screen.LocationPicker.route) }
            )
        }

        composable(Screen.VendorRecommendations.route) {
            val vendorViewModel: com.example.streetfoodai.ui.vendor.VendorViewModel = hiltViewModel()
            com.example.streetfoodai.ui.vendor.RecommendationDetailScreen(
                viewModel = vendorViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VendorMenu.route) {
            val vendorViewModel: com.example.streetfoodai.ui.vendor.VendorViewModel = hiltViewModel()
            com.example.streetfoodai.ui.vendor.MenuManagementScreen(
                viewModel = vendorViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VendorBilling.route) {
            val vendorViewModel: com.example.streetfoodai.ui.vendor.VendorViewModel = hiltViewModel()
            com.example.streetfoodai.ui.vendor.POSBillingScreen(
                viewModel = vendorViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VendorExpense.route) {
            val vendorViewModel: com.example.streetfoodai.ui.vendor.VendorViewModel = hiltViewModel()
            com.example.streetfoodai.ui.vendor.ExpenseLoggingScreen(
                viewModel = vendorViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LocationPicker.route) {
            val vendorViewModel: com.example.streetfoodai.ui.vendor.VendorViewModel = hiltViewModel()
            com.example.streetfoodai.ui.vendor.LocationPickerScreen(
                viewModel = vendorViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.CustomerHome.route) {
            val customerViewModel: com.example.streetfoodai.ui.customer.CustomerViewModel = hiltViewModel()
            com.example.streetfoodai.ui.customer.CustomerHomeScreen(
                viewModel = customerViewModel,
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.Profile.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            com.example.streetfoodai.ui.components.ProfileScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
