package com.weissev.stagecall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
actual fun rememberThemeSettings(): ThemeSettings = remember {
    val preferences = NSUserDefaults.standardUserDefaults
    ThemeSettings(preferences.boolForKey("stagecall_dark_theme")) { enabled ->
        preferences.setBool(enabled, forKey = "stagecall_dark_theme")
    }
}
