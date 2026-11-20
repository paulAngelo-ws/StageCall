package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.CampusPost
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.organizationCardColors
import com.weissev.stagecall.ui.feed.PostCard
import kotlin.time.Clock

// repository contains the data shared by all screens.
@Composable
internal fun OrganizationPage(
    repository: StageCallRepository,
    onCreatePost: () -> Unit,
    onEditPost: (Int) -> Unit,
    onEdit: () -> Unit,
    applicationsReady: Boolean,
    rolesReady: Boolean,
    roleError: String?,
    isPresident: Boolean,
    onRetryRoles: () -> Unit,
    onReview: () -> Unit,
    onOpenMembers: () -> Unit,
    id: String,
    onApply: () -> Unit,
    initialPostId: Int? = null,
) {
    val org = repository.organizations.firstOrNull { it.id == id }
    if (org == null) {
        Text("This organization is no longer available.", Modifier.padding(24.dp))
        return
    }
    val editPost = onEditPost.takeIf { isPresident }
    val isMember = repository.isMember(id)
    val visiblePosts = repository.posts.filter { it.organizationId == id && (!it.membersOnly || isMember) }
    val orgColor = organizationCardColors[organizationBannerColors.indexOf(org.bannerColor).coerceAtLeast(0)].second
    // Open the matching category when a notification or feed item links to a post.
    var tab by remember(id, initialPostId) {
        mutableStateOf(repository.posts.firstOrNull { it.id == initialPostId && it.organizationId == id }?.category
            ?.takeIf { it in listOf("Recruitment", "Events", "Announcements") } ?: "About")
    }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp), // Space between the page content and the screen edges.
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            org.photoPath?.takeIf { it.isNotBlank() }?.let { photoPath ->
                StageCallCard {
                    OrganizationThumbnail(photoPath, size = 88.dp, canEnlarge = true)
                }
            }
            StageCallCard(
                modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button, onClickLabel = "View organization members", onClick = onOpenMembers),
                colors = CardDefaults.cardColors(containerColor = orgColor, contentColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = org.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View members · ${org.memberCount}", Modifier.weight(1f))
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
        if (roleError != null) {
            Text(roleError, color = MaterialTheme.colorScheme.error)
            androidx.compose.material3.OutlinedButton(onClick = onRetryRoles) { Text("Retry") }
        }
        if (isPresident) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReview, modifier = Modifier.weight(1f)) { Text("Review applications") }
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit organization") }
            }
            Button(onClick = onCreatePost, modifier = Modifier.fillMaxWidth()) { Text("Create post") }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (option in listOf("About", "Recruitment", "Events", "Announcements")) {
                Column(
                    modifier = Modifier.selectable(selected = tab == option, role = Role.Tab, onClick = { tab = option }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(option, modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        color = if (tab == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (tab == option) FontWeight.SemiBold else FontWeight.Normal)
                    // Draw a thin underline under the selected tab.
                    Box(Modifier.fillMaxWidth().height(2.dp).background( // 2.dp sets the tab underline thickness.
                        if (tab == option) MaterialTheme.colorScheme.primary else Color.Transparent // Hide the underline on unselected tabs.
                    ))
                }
            }
        }
        when (tab) {
            "About" -> {
                Text(org.aboutHeadline, style = MaterialTheme.typography.titleLarge)
                Text(org.fullDescription)
                if (org.requirements.isNotEmpty()) {
                    Text("Membership requirements", style = MaterialTheme.typography.titleMedium)
                    org.requirements.forEach { Text("• $it") }
                }
                val latestEvent = visiblePosts.asSequence()
                    .filter { it.category == "Events" }
                    .maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
                if (latestEvent != null) {
                    StageCallCard(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                            .clickable(role = Role.Button, onClickLabel = "View events") { tab = "Events" },
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Latest event", style = MaterialTheme.typography.labelLarge)
                            Text(latestEvent.title, style = MaterialTheme.typography.titleMedium)
                            if (latestEvent.schedule.isNotBlank()) Text(latestEvent.schedule)
                        }
                    }
                }
                if (!isMember) Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp), // Keeps the Apply button at least 50.dp tall.
                    enabled = applicationsReady && rolesReady && repository.canApply(id),
                ) {
                    Text(
                        text = when {
                            !rolesReady -> "Loading organization membership"
                            !applicationsReady -> "Applications unavailable — check Applications to retry"
                            !org.isRecruiting -> "Recruitment closed"
                            !repository.canApply(id) -> "Application already submitted"
                            else -> "Apply"
                        }
                    )
                }
                val recentUpdates = visiblePosts
                    .filter { it.category == "Announcements" || it.category == "Events" }
                    .sortedWith(compareByDescending<CampusPost> { it.createdAt }.thenByDescending { it.id })
                    .distinctBy { it.category }
                Text("Recent updates", style = MaterialTheme.typography.titleLarge)
                if (recentUpdates.isEmpty()) Text("No announcements or events yet.")
                for (post in recentUpdates) {
                    PostCard(post, org, Clock.System.now(), onEditPost = editPost)
                }
            }
            else -> {
                val posts = visiblePosts.filter { it.category == tab }.sortedByDescending { it.id == initialPostId }
                if (posts.isEmpty()) Text("No posts in this category yet.")
                posts.forEach {
                    // FeedScreen.kt provides this shared post card and handles any image attached to the post.
                    PostCard(it, org, Clock.System.now(), onEditPost = editPost) // Send the post and organization to FeedScreen.kt for display.
                }
            }
        }
    }
}
