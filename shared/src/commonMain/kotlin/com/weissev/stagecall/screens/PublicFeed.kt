package com.weissev.stagecall.screens

import androidx.compose.foundation.background
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
            val organization = repository.organization(post.organizationId)
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "No image available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(post.title, style = MaterialTheme.typography.titleLarge)
                    Text(post.message)
                    if (post.schedule.isNotEmpty()) Text(post.schedule)
                    Row(
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
