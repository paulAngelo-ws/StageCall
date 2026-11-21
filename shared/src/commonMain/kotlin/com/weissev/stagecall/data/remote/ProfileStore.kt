package com.weissev.stagecall.data.remote

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

internal const val DEFAULT_YEAR_LEVEL = "1st Year"
internal val yearLevels = listOf(DEFAULT_YEAR_LEVEL, "2nd Year", "3rd Year", "4th Year")

internal fun defaultStudentNumber(email: String) = email.substringBefore('@').take(STUDENT_ID_LIMIT)

@Serializable
private data class ProfileRow(
    @SerialName("user_id") val userId: String,
    val bio: String,
    @SerialName("dark_theme") val darkTheme: Boolean = false,
    val bookmarks: List<String> = emptyList(),
    @SerialName("student_number") val studentNumber: String = "",
    @SerialName("year_level") val yearLevel: String? = null,
)

@Serializable
private data class ProfileUpdate(
    @SerialName("p_user_id") val userId: String,
    @SerialName("p_bio") val bio: String? = null,
    @SerialName("p_dark_theme") val darkTheme: Boolean? = null,
    @SerialName("p_bookmarks") val bookmarks: List<String>? = null,
    @SerialName("p_student_number") val studentNumber: String? = null,
    @SerialName("p_year_level") val yearLevel: String? = null,
)

internal class ProfileStore(
    private val client: SupabaseClient,
    private val userId: String,
    private val cache: CollectionCache,
) {
    // App.kt can open with the profile and preferences saved by CollectionCache.kt.
    private val cached = cache.cached<ProfileRow>()
    private val initialProfile = cached?.firstOrNull()
    private val lock = Mutex()
    var bio by mutableStateOf(initialProfile?.bio.orEmpty()) // Start with the saved bio so ProfileScreen.kt can show it immediately.
        private set
    var darkTheme by mutableStateOf(initialProfile?.darkTheme ?: false) // Use the saved preference or light mode if there is none.
        private set
    var bookmarks by mutableStateOf(initialProfile?.bookmarks?.distinct().orEmpty()) // Drop duplicate saved IDs before App.kt updates the directory.
        private set
    var studentNumber by mutableStateOf(initialProfile?.studentNumber.orEmpty())
        private set
    var yearLevel by mutableStateOf(initialProfile?.yearLevel ?: DEFAULT_YEAR_LEVEL)
        private set
    var loaded by mutableStateOf(cached != null) // A saved empty profile still counts as a successful load.
        private set
    var loading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun load() = lock.withLock {
        loading = true
        error = null
        try {
            // Check for updated profile fields through CollectionCache.kt. ProfileScreen.kt reads the bio and App.kt uses the theme and bookmarks.
            val row = cache.refresh {
                client.from("user_profiles").select {
                    filter { eq("user_id", userId) }
                }.decodeList<ProfileRow>()
            }.firstOrNull()
            bio = row?.bio.orEmpty()
            darkTheme = row?.darkTheme ?: false
            bookmarks = row?.bookmarks?.distinct().orEmpty()
            studentNumber = row?.studentNumber.orEmpty()
            yearLevel = row?.yearLevel ?: DEFAULT_YEAR_LEVEL
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not load your profile and preferences. Check your connection and try again."
        } finally { loading = false }
    }

    // Save acknowledged changes through CollectionCache.kt. The profile update function checks the caller before saving changes.
    private suspend fun saveCachedProfile() {
        val row = ProfileRow(userId, bio, darkTheme, bookmarks, studentNumber, yearLevel)
        cache.saveLocal(userId, cacheJson.encodeToJsonElement(row).jsonObject)
    }

    suspend fun save(bio: String, studentNumber: String, yearLevel: String): Boolean = lock.withLock {
        saving = true
        try {
            val trimmedBio = bio.trim()
            val trimmedStudentNumber = studentNumber.trim()
            val update = ProfileUpdate(userId, bio = trimmedBio, studentNumber = trimmedStudentNumber, yearLevel = yearLevel)
            client.postgrest.rpc("update_own_profile", Json.encodeToJsonElement(update).jsonObject)
            this.bio = trimmedBio
            this.studentNumber = trimmedStudentNumber
            this.yearLevel = yearLevel
            saveCachedProfile()
            error = null
            true
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not save your profile. Check your connection and try again."
            false
        } finally { saving = false }
    }

    suspend fun saveTheme(value: Boolean): Boolean = lock.withLock {
        try {
            client.postgrest.rpc("update_own_profile", Json.encodeToJsonElement(ProfileUpdate(userId, darkTheme = value)).jsonObject)
            darkTheme = value
            saveCachedProfile()
            true
        } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
    }

    suspend fun saveBookmarks(value: List<String>): Boolean = lock.withLock {
        try {
            client.postgrest.rpc("update_own_profile", Json.encodeToJsonElement(ProfileUpdate(userId, bookmarks = value)).jsonObject)
            bookmarks = value
            saveCachedProfile()
            true
        } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
    }
}
