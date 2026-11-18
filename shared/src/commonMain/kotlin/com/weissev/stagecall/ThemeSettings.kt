package com.weissev.stagecall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The selected appearance is stored on this device, independently of demo data. */
class ThemeSettings(initialDarkTheme: Boolean, private val save: (Boolean) -> Unit) {
    var isDarkTheme by mutableStateOf(initialDarkTheme)
        private set

    fun updateDarkTheme(enabled: Boolean) {
        if (enabled == isDarkTheme) return
        isDarkTheme = enabled
        save(enabled)
    }
}

@Composable
expect fun rememberThemeSettings(): ThemeSettings
