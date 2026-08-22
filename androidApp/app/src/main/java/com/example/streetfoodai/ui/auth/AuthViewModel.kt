package com.example.streetfoodai.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.streetfoodai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun loginWithGoogle(idToken: String, role: String? = null, businessName: String? = null) {
        Log.d("AuthViewModel", "Attempting Google login with role: $role")
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = repository.googleLogin(idToken, role, businessName)
                Log.d("AuthViewModel", "Google login response code: ${response.code()}")
                if (response.isSuccessful) {
                    val actualRole = response.body()?.user?.role ?: "customer"
                    Log.d("AuthViewModel", "Login Success! Role: $actualRole")
                    _authState.value = AuthState.Success(actualRole)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown server error"
                    Log.e("AuthViewModel", "Google login failure: $errorBody")
                    _authState.value = AuthState.Error(errorBody)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google login exception", e)
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Idle
        }
    }
}
