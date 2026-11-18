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

    suspend fun loadApplications(organizationId: String) {
        applications = emptyList()
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
            true
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            reviewError = "Could not save the decision. The application may already be reviewed, your access may have changed, or your connection may be unavailable."
            false
        } finally { reviewing = false }
    }
}
