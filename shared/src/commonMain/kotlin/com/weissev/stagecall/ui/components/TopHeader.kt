package com.weissev.stagecall.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
// App.kt supplies the unread count and handles the search and notification taps.
fun TopHeader(
    unreadCount: Int = 0,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), // Left and right inset plus space above and below the header.
        horizontalArrangement = Arrangement.SpaceBetween, // Push the app title and action buttons to opposite sides.
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App icon and title on the left.
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp), // Gap between the logo and app name.
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp), // 36.dp sizes the logo container and the icon.
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.TheaterComedy,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp), // 36.dp sizes the logo container and the icon.
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "StageCall",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Search and notification buttons on the right.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search organizations")
            }
            IconButton(onClick = onNotificationClick) {
                // Show the badge only when there are unread notifications.
                BadgedBox(badge = {
                    if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) } // Hide the badge at zero and show 99+ above 99.
                }) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = if (unreadCount > 0) "Notifications, $unreadCount unread" else "Open notifications",
                    )
                }
            }
        }
    }
}
