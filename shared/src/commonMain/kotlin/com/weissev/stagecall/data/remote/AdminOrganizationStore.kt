package com.weissev.stagecall.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

@Serializable
internal data class AdminOrganizationDraft(
    val name: String = "",
    val category: String = "",
    val description: String = "",
    @SerialName("short_tagline") val tagline: String = "",
    @SerialName("about_headline") val headline: String = "About us",
    @SerialName("full_description") val fullDescription: String = "",
    @SerialName("banner_color") val bannerColor: String = "blue",
    @SerialName("is_recruiting") val recruiting: Boolean = false,
    val requirements: List<String> = emptyList(),
) {
    val valid get() = name.trim().length in 1..100 && category.trim().length in 1..60 &&
        description.trim().length in 1..2000 && tagline.trim().length in 1..200 &&
        headline.trim().length in 1..200 && fullDescription.trim().length in 1..2000 &&
        requirements.size <= 30 && requirements.all { it.trim().length in 1..200 }
}

@Serializable
internal data class AdminOrganizationRow(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    @SerialName("short_tagline") val tagline: String,
    @SerialName("about_headline") val headline: String,
    @SerialName("full_description") val fullDescription: String,
    @SerialName("banner_color") val bannerColor: String,
    @SerialName("is_recruiting") val recruiting: Boolean,
    val requirements: List<String>,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("banner_path") val bannerPath: String? = null,
) {
    fun draft() = AdminOrganizationDraft(name, category, description, tagline, headline,
        fullDescription, bannerColor, recruiting, requirements)
}

internal class AdminOrganizationStore(private val client: SupabaseClient) {
    private val json = Json { encodeDefaults = true }
    var organizations by mutableStateOf<List<AdminOrganizationRow>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var working by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() { error = null }

    suspend fun refresh() {
        if (loading || working) return
        loading = true
        try {
            organizations = client.from("organizations").select().decodeList<AdminOrganizationRow>().sortedBy { it.name.lowercase() }
            loaded = true
            error = null
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "Could not load organizations. Please retry." }
        finally { loading = false }
    }

    suspend fun save(id: String?, draft: AdminOrganizationDraft,
        photoBytes: ByteArray? = null, photoType: String? = null, removePhoto: Boolean = false,
        bannerBytes: ByteArray? = null, bannerType: String? = null, removeBanner: Boolean = false,
    ): Boolean {
        if (!draft.valid) return false
        return mutate("Could not save organization. Check your connection, admin access, and whether the name is already in use.") {
            val photoPath = if (photoBytes != null && photoType != null) uploadImage(id, photoBytes, photoType) else null
            val bannerPath = if (bannerBytes != null && bannerType != null) uploadImage(id, bannerBytes, bannerType) else null
            client.postgrest.rpc("stagecall_admin_save_organization_with_images", buildJsonObject {
                put("p_id", id)
                put("p_data", json.encodeToJsonElement(draft))
                put("p_photo_path", photoPath)
                put("p_banner_path", bannerPath)
                put("p_remove_photo", removePhoto)
                put("p_remove_banner", removeBanner)
            })
        }
    }

    private suspend fun uploadImage(id: String?, bytes: ByteArray, mimeType: String): String {
        require(bytes.size <= 5 * 1024 * 1024 && mimeType in listOf("image/png", "image/jpeg"))
        val userId = requireNotNull(client.auth.currentUserOrNull()).id
        val extension = if (mimeType == "image/png") "png" else "jpg"
        val path = "${id ?: "admin"}/$userId/${kotlin.random.Random.nextLong().toULong()}.$extension"
        client.storage.from("organization-photos").upload(path, bytes) { contentType = ContentType.parse(mimeType) }
        return path
    }

    suspend fun delete(id: String): Boolean = mutate("Could not delete organization. Check your connection and admin access, then retry.") {
        client.postgrest.rpc("stagecall_admin_delete_organization", buildJsonObject { put("p_id", id) })
    }

    private suspend fun mutate(message: String, request: suspend () -> Unit): Boolean {
        if (working || loading) return false
        working = true
        error = null
        try {
            request()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = message; return false }
        finally { working = false }
        refresh()
        return true
    }
}
