package com.weissev.stagecall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

@Composable
actual fun rememberThemeSettings(): ThemeSettings = remember {
    val preferences = runCatching { Preferences.userRoot().node("com/weissev/stagecall") }.getOrNull()
    ThemeSettings(runCatching { preferences?.getBoolean("dark_theme", false) ?: false }.getOrDefault(false)) { enabled ->
        runCatching {
            preferences?.putBoolean("dark_theme", enabled)
            preferences?.flush()
        }
    }
}
