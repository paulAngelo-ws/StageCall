package com.weissev.stagecall.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileRow(@SerialName("user_id") val userId: String, val bio: String)

internal class ProfileStore(private val client: SupabaseClient, private val userId: String) {
    var bio by mutableStateOf("")
        private set
    var loaded by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun load() {
        try {
            bio = client.from("user_profiles").select {
                filter { eq("user_id", userId) }
            }.decodeList<ProfileRow>().firstOrNull()?.bio.orEmpty()
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not load your bio. Check your connection and try again."
        }
    }

    suspend fun save(value: String): Boolean {
        saving = true
        return try {
            val trimmed = value.trim()
            client.from("user_profiles").upsert(ProfileRow(userId, trimmed))
            bio = trimmed
            error = null
            true
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            error = "Could not save your bio. Check your connection and try again."
            false
        } finally { saving = false }
    }
}
