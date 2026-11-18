package com.weissev.stagecall.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Instant

internal interface RemoteStateStore {
    suspend fun load(): RepositorySnapshot
    suspend fun close()
}

/** Public catalog reads use the publishable key; no Auth session is created. */
internal class SupabaseStateStore : RemoteStateStore {
    private val client = createSupabaseClient(SupabaseConfig.url, SupabaseConfig.publishableKey) {
        install(Postgrest)
    }

    override suspend fun load(): RepositorySnapshot = coroutineScope {
        val organizations = async {
            client.from("organizations").select().decodeList<OrganizationRow>()
                .map { it.toOrganization() }.sortedBy { it.id }
        }
        val posts = async {
            client.from("campus_posts").select().decodeList<PostRow>()
                .map { it.toPost() }.filter { !it.membersOnly }.sortedByDescending { it.id }
        }
        publicCatalogSnapshot(organizations.await(), posts.await())
    }

    override suspend fun close() { client.close() }
}

@Serializable
internal data class OrganizationRow(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    @SerialName("short_tagline") val shortTagline: String,
    @SerialName("about_headline") val aboutHeadline: String,
    @SerialName("full_description") val fullDescription: String,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("is_recruiting") val isRecruiting: Boolean,
    val requirements: List<String>,
    val deadline: String,
    @SerialName("next_activity_title") val nextActivityTitle: String,
    @SerialName("next_activity_schedule") val nextActivitySchedule: String,
) {
    fun toOrganization() = Organization(
        id, name, category, description, shortTagline, aboutHeadline, fullDescription,
        memberCount, isRecruiting, requirements, deadline, nextActivityTitle, nextActivitySchedule,
    )
}

@Serializable
internal data class PostRow(
    val id: Int,
    @SerialName("organization_id") val organizationId: String,
    val title: String,
    val message: String,
    val category: String,
    @SerialName("members_only") val membersOnly: Boolean,
    val schedule: String,
    @SerialName("created_at") val createdAt: Instant,
) {
    fun toPost() = CampusPost(id, organizationId, title, message, category, membersOnly, schedule, createdAt)
}
