package com.example.streetfoodai.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object VendorHome : Screen("vendor_home")
    object CustomerHome : Screen("customer_home")
    object Splash : Screen("splash")
    object VendorMenu : Screen("vendor_menu")
    object VendorBilling : Screen("vendor_billing")
    object VendorExpense : Screen("vendor_expense")
    object VendorRecommendations : Screen("vendor_recommendations")
    object Profile : Screen("profile")
    object LocationPicker : Screen("location_picker")
    object VendorOnboarding : Screen("vendor_onboarding")
    object VendorReviews : Screen("vendor_reviews")
}
