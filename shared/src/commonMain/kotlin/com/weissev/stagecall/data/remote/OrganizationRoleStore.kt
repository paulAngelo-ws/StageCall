package com.weissev.stagecall.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.Member
import com.weissev.stagecall.data.MembershipApplication
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
internal data class OrganizationMemberProfile(
    @SerialName("user_id") val userId: String,
    val name: String,
    val picture: String,
    val bio: String,
    val role: String,
    @SerialName("joined_at") val joinedAt: Instant,
)

@Serializable
internal data class OrganizationMemberDetails(
    val email: String?,
    @SerialName("shared_organizations") val sharedOrganizations: List<String>,
    @SerialName("year_level") val yearLevel: String? = null,
)

@Serializable
private data class OrganizationMemberParameter(
    @SerialName("p_organization_id") val organizationId: String,
    @SerialName("p_user_id") val userId: String,
)

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
    @SerialName("p_requirements") val requirements: List<String>,
    @SerialName("p_photo_path") val photoPath: String?,
    @SerialName("p_remove_photo") val removePhoto: Boolean,
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

    suspend fun savePost(
        organizationId: String,
        title: String,
        message: String,
        category: String,
        schedule: String,
        membersOnly: Boolean,
        image: ByteArray?,
        imageType: String?,
        postId: Int? = null,
        removeImage: Boolean = false,
    ): Int? {
        creatingPost = true
        postError = null
        return try {
            val imagePath = if (image != null && imageType != null) {
                uploadImage("post-images", organizationId, image, imageType)
            } else null
            val parameters = Json.encodeToJsonElement(
                CreatePostParameter(organizationId, title.trim(), message.trim(), category, schedule.trim(), membersOnly, imagePath)
            ).jsonObject
            val result = if (postId == null) {
                client.postgrest.rpc("create_organization_post", parameters)
            } else {
                client.postgrest.rpc("update_organization_post", JsonObject(parameters + mapOf(
                    "p_post_id" to JsonPrimitive(postId),
                    "p_remove_image" to JsonPrimitive(removeImage),
                )))
            }
            val post = result.decodeList<PostRow>().single().toPost()
            val index = repository.posts.indexOfFirst { it.id == post.id }
            if (index >= 0) repository.posts[index] = post else repository.posts.add(0, post)
            post.id
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            postError = if (postId == null) "Could not publish the post. Please try again."
                else "Could not save changes to this post. Please try again."
            null
        } finally {
            creatingPost = false
        }
    }

    private suspend fun uploadImage(bucket: String, organizationId: String, bytes: ByteArray, mimeType: String): String {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        val extension = if (mimeType == "image/png") "png" else "jpg"
        val path = "$organizationId/$userId/${kotlin.random.Random.nextLong().toULong()}.$extension"
        client.storage.from(bucket).upload(path, bytes) { contentType = ContentType.parse(mimeType) }
        return path
    }

    fun clearSettingsError() { settingsError = null }

    suspend fun saveSettings(
        organizationId: String,
        headline: String,
        description: String,
        recruiting: Boolean,
        color: String,
        requirements: List<String>,
        photoBytes: ByteArray?,
        photoType: String?,
        removePhoto: Boolean,
    ): Boolean {
        savingSettings = true
        settingsError = null
        return try {
            val photoPath = if (photoBytes != null && photoType != null) {
                uploadImage("organization-photos", organizationId, photoBytes, photoType)
            } else null
            client.postgrest.rpc("update_organization_settings", Json.encodeToJsonElement(
                SettingsParameter(organizationId, headline.trim(), description.trim(), recruiting, color, requirements, photoPath, removePhoto)
            ).jsonObject)
            val current = repository.organizations.firstOrNull { it.id == organizationId }
            if (current != null) repository.updateOrganization(current.copy(aboutHeadline = headline.trim(),
                fullDescription = description.trim(), isRecruiting = recruiting, bannerColor = color,
                requirements = requirements, photoPath = if (removePhoto) null else photoPath ?: current.photoPath))
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

    suspend fun loadMembers(organizationId: String): List<OrganizationMemberProfile> =
        client.postgrest.rpc("list_organization_members", Json.encodeToJsonElement(
            OrganizationParameter(organizationId),
        ).jsonObject).decodeList<OrganizationMemberProfile>()

    suspend fun loadMemberDetails(organizationId: String, userId: String): OrganizationMemberDetails? =
        client.postgrest.rpc("get_organization_member_details", Json.encodeToJsonElement(
            OrganizationMemberParameter(organizationId, userId),
        ).jsonObject).decodeList<OrganizationMemberDetails>().singleOrNull()

    suspend fun leaveOrganization(organizationId: String) {
        client.postgrest.rpc("leave_organization", Json.encodeToJsonElement(
            OrganizationParameter(organizationId),
        ).jsonObject)
        refreshRoles()
    }

    suspend fun removeMember(organizationId: String, userId: String) {
        client.postgrest.rpc("remove_organization_member", Json.encodeToJsonElement(
            OrganizationMemberParameter(organizationId, userId),
        ).jsonObject)
    }

    private var reviewOrganizationId: String? = null

    suspend fun loadApplications(organizationId: String) {
        // ReviewApplicationsScreen.kt sends the organization ID. The review function checks president access.
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
            // Send the reviewer’s decision from ReviewApplicationsScreen.kt to the application decision function.
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
