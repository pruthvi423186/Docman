package com.example.docmanager.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.docmanager.util.PreferencesManager
import kotlinx.coroutines.flow.first

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            val email = preferencesManager.savedEmailFlow.first()
            val displayName = preferencesManager.savedDisplayNameFlow.first()
            val photoUri = preferencesManager.savedPhotoUriFlow.first()
            if (!email.isNullOrBlank() && !displayName.isNullOrBlank()) {
                _authState.value = AuthState.Success(email, displayName, photoUri)
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun signIn() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = authManager.signIn()) {
                is AuthResult.Success -> {
                    preferencesManager.saveAuthSession(result.email, result.displayName, result.photoUrl)
                    _authState.value = AuthState.Success(result.email, result.displayName, result.photoUrl)
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message, result.intent)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            preferencesManager.clearAuthSession()
            _authState.value = AuthState.Idle
        }
    }

    fun clearAuthIntent() {
        val currentState = _authState.value
        if (currentState is AuthState.Error && currentState.intent != null) {
            _authState.value = AuthState.Error(currentState.message, null)
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val email: String, val displayName: String, val photoUrl: String?) : AuthState()
    data class Error(val message: String, val intent: android.content.Intent? = null) : AuthState()
}
