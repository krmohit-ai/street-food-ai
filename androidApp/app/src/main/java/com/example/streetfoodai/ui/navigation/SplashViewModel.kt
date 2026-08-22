package com.example.streetfoodai.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streetfoodai.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

sealed class SplashEvent {
    data class NavigateToHome(val role: String) : SplashEvent()
    object NavigateToLogin : SplashEvent()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _events = Channel<SplashEvent>()
    val events = _events.receiveAsFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            // Add a small delay so the splash logo is actually visible
            kotlinx.coroutines.delay(1000)
            val token = tokenManager.getToken().first()
            if (token != null) {
                val role = tokenManager.getRole().first() ?: "customer"
                _events.send(SplashEvent.NavigateToHome(role))
            } else {
                _events.send(SplashEvent.NavigateToLogin)
            }
        }
    }
}
