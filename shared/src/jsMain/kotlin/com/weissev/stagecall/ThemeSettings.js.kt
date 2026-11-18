package com.weissev.stagecall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private fun readDarkTheme(): Boolean =
    js("(function() { try { return localStorage.getItem('stagecall_dark_theme') === 'true'; } catch (e) { return false; } })()") as Boolean

private fun writeDarkTheme(enabled: Boolean) {
    js("try { localStorage.setItem('stagecall_dark_theme', String(enabled)); } catch (e) {}")
}

@Composable
actual fun rememberThemeSettings(): ThemeSettings = remember {
    ThemeSettings(readDarkTheme(), ::writeDarkTheme)
}
