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

internal const val REGISTRATION_NAME_LIMIT = 100
internal const val REGISTRATION_CATEGORY_LIMIT = 60
internal const val REGISTRATION_TAGLINE_LIMIT = 200
internal const val REGISTRATION_DESCRIPTION_LIMIT = 2000
internal const val REGISTRATION_DECISION_LIMIT = 1000

@Serializable
internal data class OrganizationRegistration(
    val id: Int,
    @SerialName("applicant_id") val applicantId: String,
    @SerialName("applicant_email") val applicantEmail: String,
    val name: String,
    val category: String,
    val tagline: String,
    val description: String,
    val status: ApplicationStatus,
    val decision: String,
    @SerialName("organization_id") val organizationId: String?,
    @SerialName("submitted_at") val submittedAt: Instant,
    @SerialName("reviewed_at") val reviewedAt: Instant? = null,
)

@Serializable
internal data class RegistrationDraft(
    val name: String = "",
    val category: String = "",
    val tagline: String = "",
    val description: String = "",
) {
    val complete get() = name.isNotBlank() && category.isNotBlank() &&
        tagline.isNotBlank() && description.isNotBlank()
}

@Serializable
private data class RegistrationDecision(
    @SerialName("p_id") val id: Int,
    @SerialName("p_approve") val approve: Boolean,
    @SerialName("p_decision") val decision: String,
)

internal class OrganizationRegistrationStore(private val client: SupabaseClient, private val userId: String) {
    private val lock = Mutex()

    var own by mutableStateOf<List<OrganizationRegistration>>(emptyList())
        private set
    var pending by mutableStateOf<List<OrganizationRegistration>>(emptyList())
        private set
    var isAdmin by mutableStateOf(false)
        private set
    var accessError by mutableStateOf<String?>(null)
        private set
    var ownLoaded by mutableStateOf(false)
        private set
    var ownError by mutableStateOf<String?>(null)
        private set
    var queueLoaded by mutableStateOf(false)
        private set
    var queueError by mutableStateOf<String?>(null)
        private set
    var working by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun refresh(reviewing: Boolean) = lock.withLock {
        accessError = read("Could not check registration admin access. StageCall will retry automatically.") {
            isAdmin = client.from("stagecall_admins").select {
                filter { eq("user_id", userId) }
            }.decodeList<AdminRole>().isNotEmpty()
        }
        if (accessError != null) isAdmin = false
        if (!isAdmin) {
            pending = emptyList()
            queueLoaded = false
            queueError = null
        }
        if (reviewing) {
            if (isAdmin) {
                queueError = read("Could not load the review queue. StageCall will retry automatically.") {
                    pending = client.from("organization_registrations").select {
                        filter { eq("status", "Pending") }
                    }.decodeList<OrganizationRegistration>().sortedBy { it.id }
                    queueLoaded = true
                }
            }
        } else {
            ownError = read("Could not load your registrations. StageCall will retry automatically.") {
                own = client.from("organization_registrations").select {
                    filter { eq("applicant_id", userId) }
                }.decodeList<OrganizationRegistration>().sortedByDescending { it.id }
                ownLoaded = true
            }
        }
    }

    private suspend fun read(message: String, request: suspend () -> Unit): String? =
        try {
            request()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            message
        }

    suspend fun submit(draft: RegistrationDraft): Boolean {
        if (working || !draft.complete) return false
        return runRequest("Could not submit your registration. Check your connection and whether you already have a pending application for this name.") {
            val row = client.from("organization_registrations").insert(draft.copy(
                name = draft.name.trim(),
                category = draft.category.trim(),
                tagline = draft.tagline.trim(),
                description = draft.description.trim(),
            )) { select() }.decodeSingle<OrganizationRegistration>()
            own = listOf(row) + own.filter { it.id != row.id }
        }
    }

    suspend fun decide(id: Int, approve: Boolean, note: String): Boolean {
        if (working || !isAdmin) return false
        return runRequest("Could not save the decision. The application may have already been reviewed, or an organization with this name may already exist.") {
            client.postgrest.rpc("decide_organization_registration",
                Json.encodeToJsonElement(RegistrationDecision(id, approve, note.trim())).jsonObject)
            pending = pending.filter { it.id != id }
        }
    }

    private suspend fun runRequest(message: String, request: suspend () -> Unit): Boolean {
        working = true
        try {
            error = lock.withLock { read(message, request) }
            return error == null
        } finally {
            working = false
        }
    }
}

@Serializable
private data class AdminRole(@SerialName("user_id") val userId: String)
