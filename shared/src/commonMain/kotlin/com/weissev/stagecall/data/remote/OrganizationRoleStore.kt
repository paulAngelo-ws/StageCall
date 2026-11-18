package com.weissev.stagecall.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.Member
import com.weissev.stagecall.data.MembershipApplication
import com.weissev.stagecall.data.Organization
import com.weissev.stagecall.data.StageCallRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

@Serializable
private data class MembershipRow(
    @SerialName("organization_id") val organizationId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("created_at") val createdAt: Instant,
)
@Serializable
private data class OrganizationParameter(@SerialName("p_organization_id") val organizationId: String)
@Serializable
private data class DecisionParameter(
    @SerialName("p_organization_id") val organizationId: String,
    @SerialName("p_application_id") val applicationId: Int,
    @SerialName("p_status") val status: String,
    @SerialName("p_decision") val decision: String,
)

@Serializable
private data class SettingsParameter(
    @SerialName("p_organization_id") val organizationId: String,
    @SerialName("p_about_headline") val headline: String,
    @SerialName("p_full_description") val description: String,
    @SerialName("p_is_recruiting") val recruiting: Boolean,
    @SerialName("p_banner_color") val color: String,
)

@Serializable
private data class CreatePostParameter(
    @SerialName("p_organization_id") val organizationId: String,
    @SerialName("p_title") val title: String,
    @SerialName("p_message") val message: String,
    @SerialName("p_category") val category: String,
    @SerialName("p_schedule") val schedule: String,
    @SerialName("p_members_only") val membersOnly: Boolean,
    @SerialName("p_image_path") val imagePath: String?,
)

internal class OrganizationRoleStore(
    private val client: SupabaseClient,
    private val repository: StageCallRepository,
    private val membershipCache: CollectionCache,
    private val postCache: CollectionCache,
) {
    var roles by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var applications by mutableStateOf<List<MembershipApplication>>(emptyList())
        private set
    var reviewError by mutableStateOf<String?>(null)
        private set
    var reviewing by mutableStateOf(false)
        private set

    var settingsError by mutableStateOf<String?>(null)
        private set
    var savingSettings by mutableStateOf(false)
        private set

    var postError by mutableStateOf<String?>(null)
        private set
    var creatingPost by mutableStateOf(false)
        private set

    fun clearPostError() { postError = null }

    suspend fun createPost(organizationId: String, title: String, message: String, category: String, schedule: String, membersOnly: Boolean, image: ByteArray?, imageType: String?): Boolean {
        creatingPost = true
        postError = null
        return try {
            // OrganizationScreen.kt sends optional image bytes here. Upload them before giving the post function the storage path.
            val imagePath = if (image != null && imageType != null) { // Only upload an image if the editor supplied its bytes and type.
                val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
                val extension = if (imageType == "image/png") "png" else "jpg"
                val path = "$organizationId/$userId/${kotlin.random.Random.nextLong().toULong()}.$extension" // Give every upload its own path so MediaCache.kt can safely reuse the image later.
                client.storage.from("post-images").upload(path, image) { contentType = ContentType.parse(imageType) }
                path
            } else null
            // Migration 202611180013_post_images.sql checks the sender’s role and image path. Publish the returned row through StageCallRepository.kt.
            val post = client.postgrest.rpc("create_organization_post", Json.encodeToJsonElement(
                CreatePostParameter(organizationId, title.trim(), message.trim(), category, schedule.trim(), membersOnly, imagePath)
            ).jsonObject).decodeList<PostRow>().single().toPost()
            repository.posts.removeAll { it.id == post.id }
            repository.posts.add(0, post) // Make FeedScreen.kt show the returned post at the top.
            true
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            postError = "Could not create post. Check your connection and president access, then try again."
            false
        } finally { creatingPost = false }
    }

    suspend fun saveSettings(org: Organization, headline: String, description: String, recruiting: Boolean, color: String): Boolean {
        savingSettings = true
        settingsError = null
        return try {
            // Send the settings from OrganizationScreen.kt to 202611180007_organization_settings.sql.
            client.postgrest.rpc("update_organization_settings", Json.encodeToJsonElement(
                SettingsParameter(org.id, headline.trim(), description.trim(), recruiting, color)
            ).jsonObject)
            val current = repository.organizations.firstOrNull { it.id == org.id }
            if (current != null) repository.updateOrganization(current.copy(aboutHeadline = headline.trim(),
                fullDescription = description.trim(), isRecruiting = recruiting, bannerColor = color))
            true
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            settingsError = "Could not save settings. Check your connection and president access, then try again."
            false
        } finally { savingSettings = false }
    }

    suspend fun refreshRoles() {
        try {
            // Check membership with CollectionCache.kt first. StageCallRepository.kt gets the confirmed memberships for ProfileScreen.kt.
            val rows = membershipCache.refresh {
                client.from("organization_memberships").select().decodeList<MembershipRow>()
            }
            roles = rows.associate { it.organizationId to it.role } // Store one role per organization ID for App.kt to check.
            repository.setMemberships(rows.map { Member(it.organizationId, repository.profile, it.role) })
            repository.posts.removeAll { it.membersOnly && it.organizationId !in roles } // Hide old member posts from organizations the student no longer belongs to.
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            roles = emptyMap()
            repository.setMemberships(emptyList())
            repository.posts.removeAll { it.membersOnly }
            loaded = false
            error = "Could not load organization roles. Check your connection and retry."
        }
    }

    suspend fun refreshMemberPosts() {
        try {
            // Use the visible members-only rows from CollectionCache.kt and replace that portion of the feed in StageCallRepository.kt.
            val posts = postCache.refresh {
                client.from("campus_posts").select { filter { eq("members_only", true) } }.decodeList<PostRow>()
            }
                .map { it.toPost() }.filter { it.membersOnly && it.organizationId in roles }
            repository.posts.removeAll { it.membersOnly }
            repository.posts.addAll(posts)
        } catch (e: CancellationException) { throw e } catch (_: Exception) {
            repository.posts.removeAll { it.membersOnly }
        }
    }

    private var reviewOrganizationId: String? = null

    suspend fun loadApplications(organizationId: String) {
        // ReviewApplicationsScreen.kt sends the organization ID. The review function in 202611180005_organization_roles.sql checks president access.
        if (reviewOrganizationId != organizationId) applications = emptyList() // Clear the previous organization’s applications before loading another one.
        reviewOrganizationId = organizationId
        reviewing = true
        reviewError = null
        try {
            applications = client.postgrest.rpc("review_organization_applications", Json.encodeToJsonElement(OrganizationParameter(organizationId)).jsonObject)
                .decodeList<ApplicationRow>().map { it.toApplication() }
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            reviewError = "Could not load applications. Your president access may have changed, or your connection may be unavailable."
        } finally { reviewing = false }
    }

    suspend fun decide(organizationId: String, id: Int, accepted: Boolean, message: String): Boolean {
        reviewing = true
        reviewError = null
        return try {
            // Send the reviewer’s decision from ReviewApplicationsScreen.kt to 202611180005_organization_roles.sql.
            client.postgrest.rpc("decide_organization_application", Json.encodeToJsonElement(DecisionParameter(
                organizationId, id, if (accepted) "Accepted" else "Rejected", message.trim(),
            )).jsonObject)
            applications = applications.map { application ->
                if (application.id == id) application.copy(
                    status = if (accepted) ApplicationStatus.Accepted else ApplicationStatus.Rejected,
                    decision = message.trim(),
                ) else application
            }
            true
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            reviewError = "Could not save the decision. The application may already be reviewed, your access may have changed, or your connection may be unavailable."
            false
        } finally { reviewing = false }
    }
}
