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

@Serializable
private data class ProfileRow(@SerialName("user_id") val userId: String, val bio: String,
    @SerialName("dark_theme") val darkTheme: Boolean = false, val bookmarks: List<String> = emptyList())

@Serializable
private data class ProfileUpdate(
    @SerialName("p_user_id") val userId: String,
    @SerialName("p_bio") val bio: String? = null,
    @SerialName("p_dark_theme") val darkTheme: Boolean? = null,
    @SerialName("p_bookmarks") val bookmarks: List<String>? = null,
)

internal class ProfileStore(private val client: SupabaseClient, private val userId: String) {
    private val lock = Mutex()
    var bio by mutableStateOf("")
        private set
    var darkTheme by mutableStateOf(false)
        private set
    var bookmarks by mutableStateOf<List<String>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
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
            val row = client.from("user_profiles").select {
                filter { eq("user_id", userId) }
            }.decodeList<ProfileRow>().firstOrNull()
            bio = row?.bio.orEmpty()
            darkTheme = row?.darkTheme ?: false
            bookmarks = row?.bookmarks?.distinct().orEmpty()
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not load your profile and preferences. Check your connection and try again."
        } finally { loading = false }
    }

    suspend fun save(value: String): Boolean = lock.withLock {
        saving = true
        try {
            val trimmed = value.trim()
            client.postgrest.rpc("update_own_profile", Json.encodeToJsonElement(ProfileUpdate(userId, bio = trimmed)).jsonObject)
            bio = trimmed
            error = null
            true
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not save your bio. Check your connection and try again."
            false
        } finally { saving = false }
    }

    suspend fun saveTheme(value: Boolean): Boolean = lock.withLock {
        try {
            client.postgrest.rpc("update_own_profile", Json.encodeToJsonElement(ProfileUpdate(userId, darkTheme = value)).jsonObject)
            darkTheme = value
            true
        } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
    }

    suspend fun saveBookmarks(value: List<String>): Boolean = lock.withLock {
        try {
            client.postgrest.rpc("update_own_profile", Json.encodeToJsonElement(ProfileUpdate(userId, bookmarks = value)).jsonObject)
            bookmarks = value
            true
        } catch (e: CancellationException) { throw e } catch (_: Exception) { false }
    }
}
