package com.weissev.stagecall.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Change these colors once to update every screen in the app.
// Color choices used when the account is set to light mode.
private val campusLightColors =
    lightColorScheme(
        primary = Color(0xFF165DCC),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEDF4FF),
        onPrimaryContainer = Color(0xFF124A9E),
        secondary = Color(0xFF165DCC),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEDF4FF),
        onSecondaryContainer = Color(0xFF124A9E),
        tertiary = Color(0xFF165DCC),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFEDF4FF),
        onTertiaryContainer = Color(0xFF124A9E),
        primaryFixed = Color(0xFFEDF4FF),
        primaryFixedDim = Color(0xFFB1CEFF),
        onPrimaryFixed = Color(0xFF124A9E),
        onPrimaryFixedVariant = Color(0xFF124A9E),
        secondaryFixed = Color(0xFFEDF4FF),
        secondaryFixedDim = Color(0xFFB1CEFF),
        onSecondaryFixed = Color(0xFF124A9E),
        onSecondaryFixedVariant = Color(0xFF124A9E),
        tertiaryFixed = Color(0xFFEDF4FF),
        tertiaryFixedDim = Color(0xFFB1CEFF),
        onTertiaryFixed = Color(0xFF124A9E),
        onTertiaryFixedVariant = Color(0xFF124A9E),
        background = Color.White,
        onBackground = Color(0xFF172B46),
        surface = Color.White,
        onSurface = Color(0xFF172B46),
        surfaceVariant = Color(0xFFF4F7FB),
        onSurfaceVariant = Color(0xFF53657B),
        surfaceTint = Color(0xFF165DCC),
        inverseSurface = Color(0xFF172B46),
        inverseOnSurface = Color.White,
        inversePrimary = Color(0xFFB1CEFF),
        surfaceDim = Color(0xFFE5EBF3),
        surfaceBright = Color.White,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF8FAFD),
        surfaceContainer = Color(0xFFF4F7FB),
        surfaceContainerHigh = Color(0xFFEDF2F8),
        surfaceContainerHighest = Color(0xFFE5EBF3),
        outline = Color(0xFF6E8097),
        outlineVariant = Color(0xFFDCE4EE),
    )

// Color choices used when the account is set to dark mode.
private val campusDarkColors = darkColorScheme(
    primary = Color(0xFF92BDFF),
    onPrimary = Color(0xFF002F6B),
    primaryContainer = Color(0xFF123F79),
    onPrimaryContainer = Color(0xFFD6E5FF),
    secondary = Color(0xFF92BDFF),
    onSecondary = Color(0xFF002F6B),
    secondaryContainer = Color(0xFF123F79),
    onSecondaryContainer = Color(0xFFD6E5FF),
    tertiary = Color(0xFF92BDFF),
    onTertiary = Color(0xFF002F6B),
    tertiaryContainer = Color(0xFF123F79),
    onTertiaryContainer = Color(0xFFD6E5FF),
    background = Color(0xFF101722),
    onBackground = Color(0xFFE0E8F4),
    surface = Color(0xFF151F2D),
    onSurface = Color(0xFFE0E8F4),
    surfaceVariant = Color(0xFF202D3F),
    onSurfaceVariant = Color(0xFFB4C3D8),
    surfaceTint = Color(0xFF92BDFF),
    inverseSurface = Color(0xFFE0E8F4),
    inverseOnSurface = Color(0xFF172B46),
    inversePrimary = Color(0xFF165DCC),
    surfaceDim = Color(0xFF101722),
    surfaceBright = Color(0xFF303C4D),
    surfaceContainerLowest = Color(0xFF0C121C),
    surfaceContainerLow = Color(0xFF151F2D),
    surfaceContainer = Color(0xFF1A2534),
    surfaceContainerHigh = Color(0xFF253142),
    surfaceContainerHighest = Color(0xFF303C4D),
    outline = Color(0xFF8394AB),
    outlineVariant = Color(0xFF36465C),
)

@Composable
// App.kt passes the account’s theme preference from ProfileStore.kt. Material components read this palette throughout the app.
fun StageCallTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) campusDarkColors else campusLightColors, // Choose the colors that the screens read through MaterialTheme.
        // Default text sizes and weights for titles, headings, and body text.
        typography = Typography(
            headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
            headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold), // Style used by the app title and other large titles.
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp), // Default size and line spacing for regular body text.
        ),
        // Corner sizes shared by cards and other Material components.
        shapes =
            Shapes(
                extraSmall = RoundedCornerShape(4.dp),
                small = RoundedCornerShape(6.dp),
                medium = RoundedCornerShape(8.dp), // Change 8.dp to adjust the corners of shared cards.
                large = RoundedCornerShape(12.dp),
                extraLarge = RoundedCornerShape(16.dp),
            ),
        content = content,
    )
}
