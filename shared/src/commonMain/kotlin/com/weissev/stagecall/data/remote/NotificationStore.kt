package com.weissev.stagecall.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.weissev.stagecall.data.NotificationType
import com.weissev.stagecall.data.StageCallNotification
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
    @SerialName("organization_id") val organizationId: String? = null,
    @SerialName("post_id") val postId: Int? = null,
    @SerialName("registration_id") val registrationId: Int? = null,
    val kind: String,
    val title: String,
    val message: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("is_read") val isRead: Boolean = false,
)

@Serializable
private data class NotificationReadRow(@SerialName("notification_id") val notificationId: Int)

@Serializable
private data class MarkReadParameter(@SerialName("p_notification_id") val notificationId: Int?)

internal class NotificationStore(private val client: SupabaseClient, private val cache: CollectionCache) {
    var notifications by mutableStateOf<List<StageCallNotification>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private val mutex = Mutex()
    val unreadCount get() = notifications.count { !it.isRead } // Count only notifications the current account has not marked read.

    suspend fun refresh() = mutex.withLock {
        try {
            // CollectionCache.kt checks which notifications this account can still see before NotificationsScreen.kt displays them.
            val rows = cache.refresh {
                val legacyRows = client.from("organization_notifications").select().decodeList<NotificationRow>()
                val readIds = client.from("organization_notification_reads").select().decodeList<NotificationReadRow>()
                    .map { it.notificationId }.toSet()
                legacyRows.map { it.copy(isRead = it.id in readIds) }
            }
            notifications = rows.map { row -> // Turn database rows into the display data used by NotificationsScreen.kt.
                StageCallNotification(row.id, row.organizationId.orEmpty(),
                    when (row.kind) {
                        "post" -> NotificationType.Post
                        "application_review" -> NotificationType.ApplicationReview
                        "registration_review" -> NotificationType.RegistrationReview
                        "registration_decision" -> NotificationType.RegistrationDecision
                        else -> NotificationType.Organization
                    },
                    row.title, row.message, postId = row.postId, createdAt = row.createdAt, isRead = row.isRead,
                    registrationId = row.registrationId)
            }.sortedWith(compareByDescending<StageCallNotification> { it.createdAt }.thenByDescending { it.id })
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (_: Exception) {
            // Do not retain potentially inaccessible members-only notifications after a failed refresh.
            notifications = emptyList() // Hide previously visible rows when access cannot be checked.
            error = "Could not load notifications. Check your connection and try again."
        }
    }

    suspend fun markRead(id: Int? = null) = mutex.withLock {
        try {
            // Send one notification ID or a bulk mark-read request to the mark-read function.
            client.postgrest.rpc("mark_organization_notifications_read", Json.encodeToJsonElement(MarkReadParameter(id)).jsonObject)
            notifications = notifications.map { if (id == null || it.id == id) it.copy(isRead = true) else it } // Update the read indicator after Supabase saves the receipt.
            error = null
        } catch (e: CancellationException) { throw e } catch (_: Exception) {
            error = "Could not mark notifications as read. Please try again."
        }
    }
}
