package com.example.streetfoodai.ui.vendor

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streetfoodai.data.model.*
import com.example.streetfoodai.data.repository.VendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VendorUiState {
    object Idle : VendorUiState()
    object Loading : VendorUiState()
    data class Success(val message: String) : VendorUiState()
    data class Error(val message: String) : VendorUiState()
}

@HiltViewModel
class VendorViewModel @Inject constructor(
    private val repository: VendorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VendorUiState>(VendorUiState.Idle)
    val uiState: StateFlow<VendorUiState> = _uiState.asStateFlow()

    private val _menu = MutableStateFlow<List<ProductDto>>(emptyList())
    val menu: StateFlow<List<ProductDto>> = _menu.asStateFlow()

    private val _analytics = MutableStateFlow<AnalyticsResponse?>(null)
    val analytics: StateFlow<AnalyticsResponse?> = _analytics.asStateFlow()

    private val _recommendations = MutableStateFlow<RecommendationResponse?>(null)
    val recommendations: StateFlow<RecommendationResponse?> = _recommendations.asStateFlow()

    private val _vendorStatus = MutableStateFlow("open")
    val vendorStatus: StateFlow<String> = _vendorStatus.asStateFlow()

    private val _profile = MutableStateFlow<VendorProfileDto?>(null)
    val profile: StateFlow<VendorProfileDto?> = _profile.asStateFlow()

    private val _reviews = MutableStateFlow<List<ReviewDto>>(emptyList())
    val reviews: StateFlow<List<ReviewDto>> = _reviews.asStateFlow()

    private val _isOnboardingRequired = MutableStateFlow(false)
    val isOnboardingRequired: StateFlow<Boolean> = _isOnboardingRequired.asStateFlow()

    // POS State
    val cart = mutableStateListOf<Pair<ProductDto, Int>>()
    val cartTotal: Double
        get() = cart.sumOf { it.first.price * it.second }

    init {
        getMenu()
        getAnalytics()
        getRecommendations()
        getProfile()
        getReviews()
    }

    fun getProfile() {
        viewModelScope.launch {
            try {
                val response = repository.getProfile()
                if (response.isSuccessful) {
                    val p = response.body()
                    _profile.value = p
                    _isOnboardingRequired.value = p?.businessName.isNullOrBlank()
                }
            } catch (e: Exception) {}
        }
    }

    fun updateProfile(profile: VendorProfileDto) {
        viewModelScope.launch {
            _uiState.value = VendorUiState.Loading
            try {
                val response = repository.updateProfile(profile)
                if (response.isSuccessful) {
                    _profile.value = response.body()
                    _isOnboardingRequired.value = false
                    _uiState.value = VendorUiState.Success("Profile updated successfully")
                } else {
                    _uiState.value = VendorUiState.Error("Failed to update profile")
                }
            } catch (e: Exception) {
                _uiState.value = VendorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getReviews() {
        viewModelScope.launch {
            try {
                val response = repository.getReviews()
                if (response.isSuccessful) {
                    _reviews.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        }
    }

    fun getAnalytics(range: String = "weekly") {
        viewModelScope.launch {
            try {
                val response = repository.getAnalytics(range)
                if (response.isSuccessful) {
                    _analytics.value = response.body()
                }
            } catch (e: Exception) {}
        }
    }

    fun getRecommendations() {
        viewModelScope.launch {
            try {
                val response = repository.getRecommendations()
                if (response.isSuccessful) {
                    _recommendations.value = response.body()
                }
            } catch (e: Exception) {}
        }
    }

    fun getMenu() {
        viewModelScope.launch {
            _uiState.value = VendorUiState.Loading
            try {
                val response = repository.getMenu()
                if (response.isSuccessful) {
                    _menu.value = response.body() ?: emptyList()
                    _uiState.value = VendorUiState.Idle
                } else {
                    _uiState.value = VendorUiState.Error("Failed to fetch menu")
                }
            } catch (e: Exception) {
                _uiState.value = VendorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addProduct(name: String, price: Double, category: String, description: String?) {
        viewModelScope.launch {
            _uiState.value = VendorUiState.Loading
            try {
                val product = ProductDto(name = name, price = price, category = category, description = description)
                val response = repository.addMenuItem(product)
                if (response.isSuccessful) {
                    getMenu() // Refresh menu
                    _uiState.value = VendorUiState.Success("Product added successfully")
                } else {
                    _uiState.value = VendorUiState.Error("Failed to add product")
                }
            } catch (e: Exception) {
                _uiState.value = VendorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // POS Actions
    fun addToCart(product: ProductDto) {
        val index = cart.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val currentQty = cart[index].second
            cart[index] = product to (currentQty + 1)
        } else {
            cart.add(product to 1)
        }
    }

    fun removeFromCart(product: ProductDto) {
        val index = cart.indexOfFirst { it.first.id == product.id }
        if (index != -1) {
            val currentQty = cart[index].second
            if (currentQty > 1) {
                cart[index] = product to (currentQty - 1)
            } else {
                cart.removeAt(index)
            }
        }
    }

    fun checkout(paymentMethod: String) {
        viewModelScope.launch {
            _uiState.value = VendorUiState.Loading
            try {
                val items = cart.map { TransactionItemRequest(it.first.id!!, it.second) }
                val request = TransactionRequest(paymentMethod, items)
                val response = repository.recordTransaction(request)
                if (response.isSuccessful) {
                    cart.clear()
                    _uiState.value = VendorUiState.Success("Sale recorded: ₹${response.body()?.total_amount}")
                } else {
                    _uiState.value = VendorUiState.Error("Checkout failed")
                }
            } catch (e: Exception) {
                _uiState.value = VendorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logExpense(amount: Double, description: String, category: String) {
        viewModelScope.launch {
            _uiState.value = VendorUiState.Loading
            try {
                val request = ExpenseRequest(amount, description, category)
                val response = repository.logExpense(request)
                if (response.isSuccessful) {
                    _uiState.value = VendorUiState.Success("Expense logged")
                } else {
                    _uiState.value = VendorUiState.Error("Failed to log expense")
                }
            } catch (e: Exception) {
                _uiState.value = VendorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateLocation(lat: Double, lng: Double, status: String = "open") {
        viewModelScope.launch {
            _uiState.value = VendorUiState.Loading
            try {
                val response = repository.updateLocation(lat, lng, status)
                if (response.isSuccessful) {
                    _vendorStatus.value = status
                    _uiState.value = VendorUiState.Success("Location & Status updated successfully")
                } else {
                    _uiState.value = VendorUiState.Error("Failed to update location")
                }
            } catch (e: Exception) {
                _uiState.value = VendorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = VendorUiState.Idle
    }
}
