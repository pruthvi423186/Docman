package com.example.docmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.docmanager.auth.AuthState
import com.example.docmanager.auth.AuthViewModel
import com.example.docmanager.ui.dashboard.DashboardScreen
import com.example.docmanager.ui.dashboard.DashboardViewModel
import com.example.docmanager.ui.login.LoginScreen
import com.example.docmanager.ui.theme.DocManagerTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.foundation.isSystemInDarkTheme
import com.example.docmanager.ui.theme.ThemeViewModel
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable maximum refresh rate (e.g. 120Hz)
        try {
            val modes = if (android.os.Build.VERSION.SDK_INT.compareTo(29) == 1) {
                display?.supportedModes
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay?.supportedModes
            }
            val highestMode = modes?.maxByOrNull { it.refreshRate }
            if (highestMode != null && highestMode.refreshRate.toInt() != 60 && highestMode.refreshRate.toInt() != 50 && highestMode.refreshRate.toInt() != 30) {
                window.attributes = window.attributes.apply {
                    if (android.os.Build.VERSION.SDK_INT.compareTo(29) == 1) {
                        preferredDisplayModeId = highestMode.modeId
                    }
                    preferredRefreshRate = highestMode.refreshRate
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setContent {
            val themePreference by themeViewModel.themePreference.collectAsState()
            val darkTheme = when (themePreference) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            DocManagerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by authViewModel.authState.collectAsState()

                    when (val state = authState) {
                        is AuthState.Idle, is AuthState.Loading, is AuthState.Error -> {
                            val errorMessage = (state as? AuthState.Error)?.message
                            val authIntent = (state as? AuthState.Error)?.intent
                            
                            val authLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) { result ->
                                if (result.resultCode == Activity.RESULT_OK) {
                                    authViewModel.signIn()
                                } else {
                                    authViewModel.signOut()
                                }
                            }
                            
                            LaunchedEffect(authIntent) {
                                authIntent?.let { intent ->
                                    authLauncher.launch(intent)
                                    authViewModel.clearAuthIntent()
                                }
                            }

                            LoginScreen(
                                onSignInClick = { authViewModel.signIn() },
                                isLoading = state is AuthState.Loading,
                                errorMessage = errorMessage
                            )
                        }
                        is AuthState.Success -> {
                            androidx.compose.runtime.LaunchedEffect(state.email) {
                                dashboardViewModel.initData(state.email)
                            }
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                themeViewModel = themeViewModel,
                                userDisplayName = state.displayName,
                                userEmail = state.email,
                                userPhotoUrl = state.photoUrl,
                                onLogout = {
                                    dashboardViewModel.clearAllData()
                                    authViewModel.signOut()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
