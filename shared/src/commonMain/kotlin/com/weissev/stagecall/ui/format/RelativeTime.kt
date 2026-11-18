package com.weissev.stagecall.ui.format

import kotlin.time.Instant

// FeedScreen.kt and NotificationsScreen.kt pass a timestamp here and receive a short age label.
internal fun relativeTime(createdAt: Instant, now: Instant): String {
    val seconds = (now - createdAt).inWholeSeconds.coerceAtLeast(0) // Measure elapsed time using the timestamp and the current time supplied by the screen.
    return when {
        seconds < 60 -> "${seconds}s ago"
        seconds < 3_600 -> "${seconds / 60}min ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        seconds < 2_592_000 -> "${seconds / 86_400}d ago"
        // Relative months use 30-day intervals.
        else -> "${seconds / 2_592_000}M ago"
    }
}
