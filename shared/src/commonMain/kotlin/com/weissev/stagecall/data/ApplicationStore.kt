package com.weissev.stagecall.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

internal const val STUDENT_ID_LIMIT = 50
internal const val APPLICATION_REASON_LIMIT = 1000

@Serializable
internal data class ApplicationRow(
    val id: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("applicant_name") val name: String,
    @SerialName("applicant_email") val email: String,
    @SerialName("student_number") val studentNumber: String,
    @SerialName("year_level") val year: String,
    val reason: String,
    val status: ApplicationStatus,
    val decision: String,
    @SerialName("submitted_at") val submittedAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant? = null,
) {
    fun toApplication() = MembershipApplication(id, organizationId, Student(name, email, userId), year,
        reason, status, decision, submittedAt, updatedAt, studentNumber)
}

@Serializable
private data class ApplicationInsert(
    @SerialName("organization_id") val organizationId: String,
    @SerialName("applicant_name") val name: String,
    @SerialName("applicant_email") val email: String,
    @SerialName("student_number") val studentNumber: String,
    @SerialName("year_level") val year: String,
    val reason: String,
)

internal class ApplicationStore(private val client: SupabaseClient, private val repository: StageCallRepository) {
    private val lock = Mutex()
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var submitting by mutableStateOf(false)
        private set

    suspend fun refresh() = lock.withLock {
        try {
            val rows = client.from("membership_applications").select().decodeList<ApplicationRow>()
            repository.setApplications(rows.map { it.toApplication() }.sortedByDescending { it.submittedAt })
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not load applications. Check your connection and try again."
        }
    }

    suspend fun submit(organizationId: String, studentNumber: String, year: String, reason: String): Int? = lock.withLock {
        if (studentNumber.isBlank() || studentNumber.length > STUDENT_ID_LIMIT ||
            reason.isBlank() || reason.length > APPLICATION_REASON_LIMIT) {
            error = "Enter a student ID (up to $STUDENT_ID_LIMIT characters) and a reason (up to $APPLICATION_REASON_LIMIT characters)."
            return@withLock null
        }
        submitting = true
        try {
            val profile = repository.profile
            val row = client.from("membership_applications").insert(ApplicationInsert(
                organizationId, profile.name, profile.email, studentNumber.trim(), year, reason.trim(),
            )) { select() }.decodeSingle<ApplicationRow>()
            val application = row.toApplication()
            repository.setApplications(listOf(application) + repository.applications.filter { it.id != application.id })
            error = null
            application.id
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not submit your application. It may already exist, recruitment may be closed, or your connection may be unavailable."
            null
        } finally { submitting = false }
    }
}
