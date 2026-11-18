package com.weissev.stagecall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
// FeedScreen.kt and ProfileScreen.kt pass an already loaded bitmap here for zooming and panning.
internal fun ImageLightbox(image: ImageBitmap, onClose: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    // Limit zoom and panning so the image stays within reach.
    fun updateTransform(nextScale: Float, nextOffset: Offset) {
        scale = nextScale.coerceIn(1f, 5f)
        val maxX = viewport.width * (scale - 1f) / 2f // Limit horizontal panning to the extra width created by zooming.
        val maxY = viewport.height * (scale - 1f) / 2f // Limit vertical panning to the extra height created by zooming.
        offset = Offset(nextOffset.x.coerceIn(-maxX, maxX), nextOffset.y.coerceIn(-maxY, maxY))
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))) {
            Box(
                Modifier.fillMaxSize().padding(top = 64.dp, bottom = 72.dp).clip(MaterialTheme.shapes.extraSmall)
                    .onSizeChanged { viewport = it }
                    .pointerInput(viewport) {
                        detectTransformGestures { _, pan, zoom, _ -> updateTransform(scale * zoom, offset + pan) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(image, contentDescription = "Expanded post image", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    })
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close image", tint = Color.White)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { updateTransform(scale / 1.5f, offset) }, enabled = scale > 1f) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom out", tint = if (scale > 1f) Color.White else Color.Gray)
                }
                Text("${(scale * 100).toInt()}%", color = Color.White)
                IconButton(onClick = { updateTransform(scale * 1.5f, offset) }, enabled = scale < 5f) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom in", tint = if (scale < 5f) Color.White else Color.Gray)
                }
            }
        }
    }
}
