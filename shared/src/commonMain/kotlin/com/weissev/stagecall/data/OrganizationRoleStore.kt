package com.weissev.stagecall.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
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

internal class OrganizationRoleStore(private val client: SupabaseClient, private val repository: StageCallRepository) {
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

    suspend fun saveSettings(org: Organization, headline: String, description: String, recruiting: Boolean, color: String): Boolean {
        savingSettings = true
        settingsError = null
        return try {
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
            val rows = client.from("organization_memberships").select().decodeList<MembershipRow>()
            roles = rows.associate { it.organizationId to it.role }
            repository.setMemberships(rows.map { Member(it.organizationId, repository.profile, it.role) })
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            roles = emptyMap()
            repository.setMemberships(emptyList())
            loaded = false
            error = "Could not load organization roles. Check your connection and retry."
        }
    }

    private var reviewOrganizationId: String? = null

    suspend fun loadApplications(organizationId: String) {
        if (reviewOrganizationId != organizationId) applications = emptyList()
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
