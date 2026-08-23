package com.example.streetfoodai.util

object Constants {
    const val BASE_URL = "https://street-food-ai-backend.onrender.com/api/"
    
    // Auth Endpoints
    const val LOGIN_URL = "auth/login"
    const val REGISTER_URL = "auth/register"
    
    // Vendor Endpoints
    const val VENDOR_MENU = "vendor/menu"
    const val VENDOR_TRANSACTIONS = "vendor/transactions"
    const val VENDOR_EXPENSES = "vendor/expenses"
    const val VENDOR_LOCATION = "vendor/location"
    const val VENDOR_RECOMMENDATIONS = "vendor/recommendations"
    const val VENDOR_ANALYTICS = "vendor/analytics"
    
    // Customer Endpoints
    const val CUSTOMER_VENDORS = "customer/vendors"
    const val CUSTOMER_SEARCH = "customer/search"
}
