package com.weissev.stagecall.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

@Serializable
private data class NotificationRow(
    val id: Int,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("post_id") val postId: Int? = null,
    val kind: String,
    val title: String,
    val message: String,
    @SerialName("created_at") val createdAt: Instant,
)

@Serializable
private data class NotificationReadRow(@SerialName("notification_id") val notificationId: Int)

@Serializable
private data class MarkReadParameter(@SerialName("p_notification_id") val notificationId: Int?)

internal class NotificationStore(private val client: SupabaseClient) {
    var notifications by mutableStateOf<List<StageCallNotification>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private val mutex = Mutex()
    val unreadCount get() = notifications.count { !it.isRead }

    suspend fun refresh() = mutex.withLock {
        try {
            val rows = client.from("organization_notifications").select().decodeList<NotificationRow>()
            val readIds = client.from("organization_notification_reads").select().decodeList<NotificationReadRow>()
                .map { it.notificationId }.toSet()
            notifications = rows.map { row ->
                StageCallNotification(row.id, row.organizationId,
                    if (row.kind == "post") NotificationType.Post else NotificationType.Organization,
                    row.title, row.message, postId = row.postId, createdAt = row.createdAt, isRead = row.id in readIds)
            }.sortedWith(compareByDescending<StageCallNotification> { it.createdAt }.thenByDescending { it.id })
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (_: Exception) {
            // Do not retain potentially inaccessible members-only notifications after a failed refresh.
            notifications = emptyList()
            error = "Could not load notifications. Check your connection and try again."
        }
    }

    suspend fun markRead(id: Int? = null) = mutex.withLock {
        try {
            client.postgrest.rpc("mark_organization_notifications_read", Json.encodeToJsonElement(MarkReadParameter(id)).jsonObject)
            notifications = notifications.map { if (id == null || it.id == id) it.copy(isRead = true) else it }
            error = null
        } catch (e: CancellationException) { throw e } catch (_: Exception) {
            error = "Could not mark notifications as read. Please try again."
        }
    }
}
