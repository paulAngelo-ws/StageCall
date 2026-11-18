package com.weissev.stagecall.data.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.weissev.stagecall.data.RepositoryCache
import com.weissev.stagecall.data.rememberRepositoryCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class CachedRow(val version: String, val data: JsonObject)

@Serializable
// Migration 202611180017_incremental_sync.sql returns rows to replace and IDs to remove. All other saved rows stay in the cache.
internal data class CollectionChanges(val changed: Map<String, CachedRow>, val removed: List<String>) {
    fun applyTo(rows: Map<String, CachedRow>) = (rows - removed.toSet()) + changed // Remove the listed IDs first, then add or replace the rows Supabase sent.
}

internal val cacheProjectKey get() = SupabaseConfig.url.encodeToByteArray().toHexString()

internal val cacheJson = Json { ignoreUnknownKeys = true }

@Composable
internal fun rememberCollectionCache(
    client: SupabaseClient,
    account: String,
    collection: String,
    shared: Boolean = false,
): CollectionCache {
    // Public records use a shared device entry. Profile, application, and other personal records use the signed-in account’s entry.
    val owner = if (shared) "public" else account
    val storage = rememberRepositoryCache("sync-$cacheProjectKey-$owner-$collection") // RepositoryCache.kt uses this key to separate projects, accounts, and collections.
    return remember(client, storage, account) { CollectionCache(client, account, collection, storage) }
}

internal class CollectionCache(
    private val client: SupabaseClient,
    private val account: String,
    private val collection: String,
    private val storage: RepositoryCache,
) {
    private val lock = Mutex() // Keep a refresh and a local write from replacing each other’s results.
    private var syncAvailable = true
    // The feature stores can read this saved snapshot before the first network request finishes.
    private var rows = runCatching {
        storage.read()?.let { cacheJson.decodeFromString<Map<String, CachedRow>>(it) } // Turn the saved text back into rows and versions.
    }.getOrNull() // Treat unreadable saved data as a cache miss.

    inline fun <reified T> cached(): List<T>? = runCatching {
        snapshot()?.map { cacheJson.decodeFromJsonElement<T>(it) }
    }.getOrNull()

    fun snapshot() = rows?.values?.map { it.data }

    suspend inline fun <reified T> refresh(crossinline legacy: suspend () -> List<T>): List<T> =
        refreshRows(
            decode = { rows -> rows.map { cacheJson.decodeFromJsonElement<T>(it) } },
            legacy = { legacy().map { cacheJson.encodeToJsonElement(it).jsonObject } },
        )

    suspend fun <T> refreshRows(
        decode: (List<JsonObject>) -> T,
        legacy: suspend () -> List<JsonObject>,
    ): T = lock.withLock {
        val updated = download(legacy) // Ask Supabase for the latest changes to this collection.
        val result = decode(updated.values.map { it.data }) // Give the calling store its typed rows after merging the changes.
        // Decode the merged rows before saving them, so a response the app cannot read does not overwrite the previous cache.
        if (updated != rows) save(updated) // Skip the disk write if the rows are the same.
        result
    }

    private suspend fun download(legacy: suspend () -> List<JsonObject>): Map<String, CachedRow> {
        if (!syncAvailable) return downloadLegacy(legacy)
        val current = rows.orEmpty()
        // Send the collection name, account ID, and saved versions to 202611180017_incremental_sync.sql.
        val changes = try {
            client.postgrest.rpc("stagecall_sync", JsonObject(mapOf(
                "p_account" to JsonPrimitive(account), // Tell the SQL function which signed-in account this request belongs to.
                "p_collection" to JsonPrimitive(collection), // Name the list to check, such as applications or public posts.
                "p_known" to JsonObject(current.mapValues { JsonPrimitive(it.value.version) }), // Send each saved ID and version without resending the row data.
            ))).decodeAs<CollectionChanges>() // Read the changed rows and removed IDs returned by migration 017.
        } catch (e: PostgrestRestException) {
            // Only use the older full read when the sync function is missing. Network and permission errors go back to the feature store.
            if (e.code != "PGRST202") throw e // Only fall back when Supabase reports the function is missing.
            syncAvailable = false
            return downloadLegacy(legacy)
        }
        return changes.applyTo(current)
    }

    private suspend fun downloadLegacy(legacy: suspend () -> List<JsonObject>): Map<String, CachedRow> {
        check(client.auth.currentUserOrNull()?.id == account)
        val values = legacy() // Use the store’s older full-list request.
        check(client.auth.currentUserOrNull()?.id == account)
        val idField = when (collection) {
            "profile" -> "user_id"
            "memberships" -> "organization_id"
            else -> "id"
        }
        return values.associate { it.getValue(idField).jsonPrimitive.content to CachedRow("", it) }
    }

    // An empty version asks the server to reconcile locally acknowledged writes.
    suspend fun saveLocal(id: String, data: JsonObject) = lock.withLock {
        val current = rows ?: return@withLock
        save(current + (id to CachedRow("", data))) // Keep the new row and use a blank version to check it on the next sync.
    }

    private fun save(value: Map<String, CachedRow>) {
        rows = value
        runCatching { storage.write(cacheJson.encodeToString(value)) } // The store can still use these rows if device storage fails.
    }
}
