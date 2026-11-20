package com.weissev.stagecall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import com.weissev.stagecall.data.remote.LocalMediaCache
import kotlinx.coroutines.CancellationException

// Default thumbnail for cards that represent an organization.
@Composable
// FeedScreen.kt, ApplicationsScreen.kt, NotificationsScreen.kt, and ProfileScreen.kt use this shared thumbnail style.
internal fun OrganizationThumbnail(
    photoPath: String? = null,
    size: Dp = 44.dp,
    preview: ImageBitmap? = null,
    canEnlarge: Boolean = false,
) {
    val media = LocalMediaCache.current
    val stored by key(media, photoPath, preview != null) {
        produceState<ImageBitmap?>(null) {
            if (media != null && photoPath != null && preview == null) {
                try {
                    value = media.organizationPhoto(photoPath)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Show the placeholder when the photo cannot be loaded.
                }
            }
        }
    }
    val photo = preview ?: stored
    var viewingPhoto by remember(photoPath, photo) { mutableStateOf(false) }
    if (viewingPhoto && photo != null) ImageLightbox(photo, onClose = { viewingPhoto = false })
    val photoAction = if (canEnlarge && photo != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = "View organization photo") { viewingPhoto = true }
    } else Modifier
    Box(
        modifier = Modifier.size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(photoAction),
        contentAlignment = Alignment.Center,
    ) {
        if (photo == null) {
            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(size * 0.6f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Image(photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
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
