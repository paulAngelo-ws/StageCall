package com.weissev.stagecall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.remote.LocalMediaCache
import kotlinx.coroutines.CancellationException

/** The image fills the existing card bounds; the content determines its height. */
@Composable
internal fun OrganizationCardBackground(
    path: String?,
    fallbackColor: Color,
    content: @Composable () -> Unit,
) {
    val media = LocalMediaCache.current
    val image by produceState<ImageBitmap?>(null, media, path) {
        value = null
        if (!path.isNullOrBlank() && media != null) {
            try { value = media.organizationPhoto(path) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Preserve the original card when unavailable. */ }
        }
    }
    Box(Modifier.fillMaxWidth().background(fallbackColor)) {
        image?.let {
            Image(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.75f)))))
        }
        content()
    }
}

@Composable
internal fun OrganizationBanner(
    path: String?,
    modifier: Modifier = Modifier,
    preview: ImageBitmap? = null,
    canEnlarge: Boolean = false,
) {
    val media = LocalMediaCache.current
    val stored by produceState<ImageBitmap?>(null, media, path, preview) {
        value = null
        if (path != null && preview == null && media != null) {
            try { value = media.organizationPhoto(path) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Keep the fallback if unavailable. */ }
        }
    }
    val image = preview ?: stored
    var enlarged by remember(path, image) { mutableStateOf(false) }
    if (enlarged && image != null) ImageLightbox(image, onClose = { enlarged = false })
    Box(modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .then(if (canEnlarge && image != null) Modifier.clickable(role = Role.Button,
            onClickLabel = "View organization banner") { enlarged = true } else Modifier),
        contentAlignment = Alignment.Center) {
        if (image != null) Image(image, "Organization banner", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Outlined.Image, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
