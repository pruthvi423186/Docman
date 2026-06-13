package com.example.docmanager.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.docmanager.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val themePreference: StateFlow<String> = preferencesManager.themePreferenceFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "system"
        )

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setThemePreference(theme)
        }
    }
}
