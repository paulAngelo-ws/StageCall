package com.weissev.stagecall.data.remote

import com.weissev.stagecall.data.CampusPost
import com.weissev.stagecall.data.Organization
import com.weissev.stagecall.data.RemoteStateStore
import com.weissev.stagecall.data.RepositorySnapshot
import com.weissev.stagecall.data.publicCatalogSnapshot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Public catalog data stays separate from account-specific content. */
internal class SupabaseStateStore(
    private val client: SupabaseClient,
    private val organizationCache: CollectionCache,
    private val postCache: CollectionCache,
) : RemoteStateStore {
    // Read the organization and public post collections through CollectionCache.kt. RepositoryPersistence.kt receives the resulting snapshot.
    override suspend fun load(): RepositorySnapshot = coroutineScope {
        val organizations = async { // Start loading organizations while the post request starts below.
            organizationCache.refresh { client.from("organizations").select().decodeList<OrganizationRow>() }
                .map { it.toOrganization() }.sortedBy { it.id }
        }
        val posts = async { // Read posts separately so each collection can check its own saved versions.
            postCache.refresh {
                client.from("campus_posts").select { filter { eq("members_only", false) } }.decodeList<PostRow>()
            }
                .map { it.toPost() }.filter { !it.membersOnly }.sortedByDescending { it.id }
        }
        publicCatalogSnapshot(organizations.await(), posts.await()) // Wait for both lists before returning the catalog to RepositoryPersistence.kt.
    }

    override suspend fun close() = Unit
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
    @SerialName("next_activity_title") val nextActivityTitle: String,
    @SerialName("next_activity_schedule") val nextActivitySchedule: String,
    @SerialName("banner_color") val bannerColor: String = "blue",
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("banner_path") val bannerPath: String? = null,
) {
    // Map database column names to the organization model in Models.kt.
    fun toOrganization() = Organization(
        id, name, category, description, shortTagline, aboutHeadline, fullDescription,
        memberCount, isRecruiting, requirements, nextActivityTitle, nextActivitySchedule, bannerColor, photoPath, bannerPath,
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
    @SerialName("image_path") val imagePath: String? = null,
) {
    fun toPost() = CampusPost(id, organizationId, title, message, category, membersOnly, schedule, createdAt, imagePath)
}
