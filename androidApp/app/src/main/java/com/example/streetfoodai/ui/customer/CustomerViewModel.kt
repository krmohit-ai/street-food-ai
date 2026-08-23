package com.example.streetfoodai.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streetfoodai.data.api.StreetFoodApi
import com.example.streetfoodai.data.model.NearbyVendorDto
import com.example.streetfoodai.data.model.ProductDto
import com.example.streetfoodai.data.model.ReviewDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val api: StreetFoodApi
) : ViewModel() {

    private val _nearbyVendors = MutableStateFlow<List<NearbyVendorDto>>(emptyList())
    val nearbyVendors: StateFlow<List<NearbyVendorDto>> = _nearbyVendors.asStateFlow()

    private val _selectedVendorMenu = MutableStateFlow<List<ProductDto>>(emptyList())
    val selectedVendorMenu: StateFlow<List<ProductDto>> = _selectedVendorMenu.asStateFlow()

    private val _selectedVendorReviews = MutableStateFlow<List<ReviewDto>>(emptyList())
    val selectedVendorReviews: StateFlow<List<ReviewDto>> = _selectedVendorReviews.asStateFlow()

    fun fetchNearbyVendors(lat: Double, lng: Double) {
        android.util.Log.d("CustomerViewModel", "Fetching vendors for: $lat, $lng")
        viewModelScope.launch {
            try {
                val response = api.getNearbyVendors(lat, lng)
                if (response.isSuccessful) {
                    _nearbyVendors.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerViewModel", "Error fetching vendors", e)
            }
        }
    }

    fun searchVendors(query: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val response = api.searchVendors(query, lat, lng)
                if (response.isSuccessful) {
                    _nearbyVendors.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerViewModel", "Error searching vendors", e)
            }
        }
    }

    fun fetchVendorMenu(vendorId: String) {
        viewModelScope.launch {
            try {
                val response = api.getVendorMenu(vendorId)
                if (response.isSuccessful) {
                    _selectedVendorMenu.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    fun fetchVendorReviews(vendorId: String) {
        viewModelScope.launch {
            try {
                val response = api.getVendorReviewsCustomer(vendorId)
                if (response.isSuccessful) {
                    _selectedVendorReviews.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    fun postReview(vendorId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            try {
                val request = mapOf("rating" to rating, "comment" to comment)
                val response = api.postReview(vendorId, request)
                if (response.isSuccessful) {
                    fetchVendorReviews(vendorId) // Refresh
                }
            } catch (e: Exception) {}
        }
    }

    fun createDemand(itemName: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val request = mapOf("item_name" to itemName, "latitude" to lat, "longitude" to lng)
                api.createDemand(request)
            } catch (e: Exception) {}
        }
    }
}
