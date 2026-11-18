package com.weissev.stagecall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeSettingsTest {
    @Test
    fun changesAppearanceAndRestoresSavedPreference() {
        var storedDarkTheme = false
        val settings = ThemeSettings(storedDarkTheme) { storedDarkTheme = it }
        settings.updateDarkTheme(true)
        assertTrue(settings.isDarkTheme)
        assertTrue(ThemeSettings(storedDarkTheme) {}.isDarkTheme)
        settings.updateDarkTheme(false)
        assertFalse(settings.isDarkTheme)
        assertFalse(ThemeSettings(storedDarkTheme) {}.isDarkTheme)
    }

    @Test
    fun unchangedAppearanceDoesNotWritePreference() {
        val writes = mutableListOf<Boolean>()
        val settings = ThemeSettings(true) { writes.add(it) }
        settings.updateDarkTheme(true)
        settings.updateDarkTheme(false)
        settings.updateDarkTheme(false)
        assertEquals(listOf(false), writes)
    }
}
