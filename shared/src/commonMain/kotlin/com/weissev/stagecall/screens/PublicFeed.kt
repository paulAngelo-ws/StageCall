package com.weissev.stagecall.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.OrganizationThumbnail
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.decodeToImageBitmap
import com.weissev.stagecall.AuthSession
import io.github.jan.supabase.storage.storage
import com.weissev.stagecall.data.CampusPost
import com.weissev.stagecall.data.Organization
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Instant

// repository contains the data shared by all screens.
@Composable
fun FeedPage(
    repository: StageCallRepository,
    onOpenOrganization: (String) -> Unit,
    onOpenPost: (String, Int) -> Unit,
) {
    var category by remember { mutableStateOf("All") }
    var showFilters by remember { mutableStateOf(false) }
    val now by produceState(Clock.System.now()) {
        while (true) {
            delay(1_000L)
            value = Clock.System.now()
        }
    }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Campus feed",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Box {
                IconButton(onClick = { showFilters = true }) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "Filter posts: $category",
                        tint = if (category == "All") MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                    )
                }
                DropdownMenu(
                    expanded = showFilters,
                    onDismissRequest = { showFilters = false },
                ) {
                    for (option in listOf("All", "Recruitment", "Events", "Announcements")) {
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                showFilters = false
                            },
                            trailingIcon = {
                                if (category == option) {
                                    Icon(Icons.Outlined.Check, contentDescription = "Selected")
                                }
                            },
                        )
                    }
                }
            }
        }
        // Keep posts that match the selected category and the student's access.
        val posts =
            repository.posts.filter { post ->
                val matchesCategory = category == "All" || post.category == category
                val canRead = !post.membersOnly || repository.isMember(post.organizationId)
                matchesCategory && canRead
            }
        if (posts.isEmpty()) Text("No posts in this category yet.")
        for (post in posts) {
            PostCard(post, repository.organization(post.organizationId), now, onOpenOrganization, onOpenPost)
        }
    }
}

@Composable
internal fun PostCard(
    post: CampusPost, organization: Organization, now: Instant,
    onOpenOrganization: ((String) -> Unit)? = null, onOpenPost: ((String, Int) -> Unit)? = null,
) {
            StageCallCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OrganizationThumbnail()
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                organization.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = relativePostTime(post.createdAt, now),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (post.imagePath != null) PostImage(post.imagePath)
                    Text(post.title, style = MaterialTheme.typography.titleLarge)
                    if (post.membersOnly) Text("Members only", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(post.message)
                    if (post.schedule.isNotEmpty()) Text(post.schedule)
                    if (onOpenPost != null && onOpenOrganization != null) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            onClick = { onOpenPost(post.organizationId, post.id) },
                        ) {
                            Text("View post", textAlign = TextAlign.Center)
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            onClick = { onOpenOrganization(post.organizationId) },
                        ) {
                            Text("View organization", textAlign = TextAlign.Center)
                        }
                    }
                }
            }
}

@Composable
private fun PostImage(path: String) {
    var viewing by remember(path) { mutableStateOf(false) }
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = try { AuthSession.client?.storage?.from("post-images")?.downloadAuthenticated(path)?.decodeToImageBitmap() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null }
    }
    bitmap?.let { image ->
        Image(image, contentDescription = "Open post image", modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small).clickable { viewing = true }, contentScale = ContentScale.Crop)
        if (viewing) ImageLightbox(image, onClose = { viewing = false })
    }
}

@Composable
internal fun ImageLightbox(image: ImageBitmap, onClose: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun updateTransform(nextScale: Float, nextOffset: Offset) {
        scale = nextScale.coerceIn(1f, 5f)
        val maxX = viewport.width * (scale - 1f) / 2f
        val maxY = viewport.height * (scale - 1f) / 2f
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

internal fun relativePostTime(createdAt: Instant, now: Instant): String {
    val seconds = (now - createdAt).inWholeSeconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> "${seconds}s ago"
        seconds < 3_600 -> "${seconds / 60}min ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        seconds < 2_592_000 -> "${seconds / 86_400}d ago"
        // Relative months use 30-day intervals.
        else -> "${seconds / 2_592_000}M ago"
    }
}
