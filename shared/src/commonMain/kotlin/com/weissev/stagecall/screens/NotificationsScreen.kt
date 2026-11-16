package com.weissev.stagecall.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.OrganizationThumbnail
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.data.StageCallNotification
import com.weissev.stagecall.data.StageCallRepository
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
fun NotificationsPage(
    repository: StageCallRepository,
    onOpenNotification: (StageCallNotification) -> Unit,
    onOpenDirectory: () -> Unit,
) {
    var unreadOnly by remember { mutableStateOf(false) }
    val now by produceState(Clock.System.now()) {
        while (true) {
            delay(1_000L)
            value = Clock.System.now()
        }
    }
    val notifications = repository.notifications
    val visibleNotifications = if (unreadOnly) notifications.filter { !it.isRead } else notifications
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(selected = !unreadOnly, onClick = { unreadOnly = false }, label = { Text("All") })
                FilterChip(
                    selected = unreadOnly,
                    onClick = { unreadOnly = true },
                    label = { Text("Unread (${repository.unreadNotificationCount})") },
                )
            }
        }
        if (repository.unreadNotificationCount > 0) {
            item {
                TextButton(onClick = repository::markAllNotificationsRead) { Text("Mark all as read") }
            }
        }
        if (visibleNotifications.isEmpty()) {
            item {
                StageCallCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if (unreadOnly) "You're all caught up" else "No notifications yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                unreadOnly -> "You have no unread updates."
                                !repository.notificationsEnabled -> "Notifications are turned off in your profile."
                                else -> "Posts, activities, and changes from organizations you've joined will appear here."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!unreadOnly) TextButton(onClick = onOpenDirectory) { Text("Explore organizations") }
                    }
                }
            }
        }
        items(visibleNotifications, key = { it.id }) { notification ->
            val organization = repository.organization(notification.organizationId)
            StageCallCard(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button, onClickLabel = "Open ${notification.title}") {
                        repository.markNotificationRead(notification.id)
                        onOpenNotification(notification)
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    OrganizationThumbnail()
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(organization.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            notification.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        )
                        Text(
                            notification.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            relativePostTime(notification.createdAt, now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!notification.isRead) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
