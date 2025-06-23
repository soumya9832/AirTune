package com.soumyadip.airtune.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel:ViewModel() {
    private val _showSplashScreen = MutableStateFlow(true)
    val showSplashScreen: StateFlow<Boolean> = _showSplashScreen

    init {
        // This init block runs ONCE per ViewModel instance.
        // It does NOT run again on Activity recreation.
        viewModelScope.launch {
            // Simulate any necessary loading or data fetching here
            // e.g., loading user preferences, initializing services
            delay(2500) // Simulate a 2.5-second splash screen duration

            _showSplashScreen.value = false // Once loading is complete, hide splash
        }
    }


}