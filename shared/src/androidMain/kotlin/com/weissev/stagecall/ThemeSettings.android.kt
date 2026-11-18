package com.weissev.stagecall

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberThemeSettings(): ThemeSettings {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("stagecall_settings", Context.MODE_PRIVATE)
        ThemeSettings(preferences.getBoolean("dark_theme", false)) { enabled ->
            preferences.edit().putBoolean("dark_theme", enabled).apply()
        }
    }
}
