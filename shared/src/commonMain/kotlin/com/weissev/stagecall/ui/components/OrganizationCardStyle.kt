package com.weissev.stagecall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Default thumbnail for cards that represent an organization.
@Composable
// FeedScreen.kt, ApplicationsScreen.kt, NotificationsScreen.kt, and ProfileScreen.kt use this shared thumbnail style.
internal fun OrganizationThumbnail() {
    Box(
        modifier = Modifier.size(44.dp) // Width and height of the thumbnail background.
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Image,
            contentDescription = null,
            modifier = Modifier.size(26.dp), // Size of the icon inside the thumbnail.
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Each pair has a lighter shade first and a darker accent second. Card bars use the accent.
internal val organizationCardColors = listOf(
    Color(0xFFDCEEF8) to Color(0xFF2585B5),
    Color(0xFFDFEFE1) to Color(0xFF38864A),
    Color(0xFFEDE1F5) to Color(0xFF8A4BB0),
    Color(0xFFF9E2D8) to Color(0xFFCC6940),
    Color(0xFFE0E8FA) to Color(0xFF4C6DC7),
    Color(0xFFF7EDCD) to Color(0xFFB08B20),
)
