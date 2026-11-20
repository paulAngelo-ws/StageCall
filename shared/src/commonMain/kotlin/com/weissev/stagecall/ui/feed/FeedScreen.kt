package com.weissev.stagecall.ui.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.CampusPost
import com.weissev.stagecall.data.Organization
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.remote.LocalMediaCache
import com.weissev.stagecall.ui.components.ImageLightbox
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.format.relativeTime
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
    // Update the clock so relative times stay current while the feed is open.
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
            // StageCallRepository.kt supplies the post and organization. A card tap sends their IDs to the route callbacks in App.kt.
            PostCard(post, repository.organization(post.organizationId), now, onOpenOrganization, onOpenPost) // Combine each post with the organization in StageCallRepository.kt.
        }
    }
}

@Composable
internal fun PostCard(
    post: CampusPost, organization: Organization, now: Instant,
    onOpenOrganization: ((String) -> Unit)? = null, onOpenPost: ((String, Int) -> Unit)? = null,
    onEditPost: ((Int) -> Unit)? = null,
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
                        OrganizationThumbnail(organization.photoPath)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(organization.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = relativeTime(post.createdAt, now),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (onEditPost != null) {
                            IconButton(onClick = { onEditPost(post.id) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit post")
                            }
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
internal fun PostImage(path: String) {
    var viewing by remember(path) { mutableStateOf(false) }
    // MediaCache.kt receives the storage path and returns an image from the cache or Supabase Storage.
    val media = LocalMediaCache.current
    val bitmap by produceState<ImageBitmap?>(null, media, path) {
        value = null
        value = try { media?.post(path) } // MediaCache.kt uses the image path to find or download the photo.
        catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null }
    }
    bitmap?.let { image ->
        Image(image, contentDescription = "Open post image", modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small).clickable { viewing = true }, contentScale = ContentScale.Crop)
        // Send the same bitmap to ImageLightbox.kt for the full-screen view.
        if (viewing) ImageLightbox(image, onClose = { viewing = false }) // Pass the loaded picture to ImageLightbox.kt for the larger view.
    }
}
