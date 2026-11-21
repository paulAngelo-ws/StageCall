package com.weissev.stagecall.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.MembershipApplication
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.Student
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

internal const val STUDENT_ID_LIMIT = 8
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
    // Convert the database row to the membership application model in Models.kt.
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

internal class ApplicationStore(
    private val client: SupabaseClient,
    private val repository: StageCallRepository,
    private val cache: CollectionCache,
) {
    // Read the cached rows through CollectionCache.kt and pass them to StageCallRepository.kt for ApplicationsScreen.kt.
    private val cached = cache.cached<ApplicationRow>()
    init { cached?.let(::applyRows) } // Show saved applications before the first server check.

    private fun applyRows(rows: List<ApplicationRow>) {
        repository.setApplications(rows.map { it.toApplication() }.sortedByDescending { it.submittedAt }) // Give StageCallRepository.kt the newest requests first.
    }

    private val lock = Mutex()
    var loaded by mutableStateOf(cached != null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var submitting by mutableStateOf(false)
        private set

    suspend fun refresh() = lock.withLock {
        try {
            applyRows(cache.refresh { client.from("membership_applications").select().decodeList<ApplicationRow>() }) // CollectionCache.kt checks for changes or uses the full read on an older database.
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
            val profile = repository.profile // Take the applicant’s name and email from StageCallRepository.kt.
            // ApplyScreen.kt sends the form here. The database validates the application fields and checks recruitment access.
            val row = client.from("membership_applications").insert(ApplicationInsert(
                organizationId, profile.name, profile.email, studentNumber.trim(), year, reason.trim(),
            )) { select() }.decodeSingle<ApplicationRow>() // Ask Supabase to return the inserted row with its assigned ID.
            val application = row.toApplication() // Convert the returned row into the model in Models.kt.
            repository.setApplications(listOf(application) + repository.applications.filter { it.id != application.id })
            // Keep the acknowledged row in CollectionCache.kt and return its ID to ApplyScreen.kt.
            cache.saveLocal(row.id.toString(), cacheJson.encodeToJsonElement(row).jsonObject)
            error = null
            application.id // Return this ID to ApplyScreen.kt after the save.
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not submit your application. It may already exist, recruitment may be closed, or your connection may be unavailable."
            null
        } finally { submitting = false }
    }
}
