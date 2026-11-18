package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.Serializable

// RepositoryPersistence.kt uses this interface for catalog snapshots. CollectionCache.kt uses it for rows and their saved versions.
internal interface RepositoryCache {
    fun read(): String? // Return null when the device has no saved value yet.
    fun write(value: String) // The caller supplies serialized text for one cache entry.
    fun remove() {}
}

// The platform files named RepositoryCache.*.kt choose where each device stores these entries.
internal fun interface CacheStorage {
    fun entry(key: String): RepositoryCache // Choose a storage entry by name so different accounts and lists can have separate copies.

    suspend fun readMedia(key: String): String? = entry(key).read()
    suspend fun writeMedia(key: String, value: String) = entry(key).write(value)
    suspend fun removeMedia(key: String) = entry(key).remove()
}

@Composable
internal expect fun rememberCacheStorage(): CacheStorage

@Composable
internal fun rememberRepositoryCache(key: String = "state"): RepositoryCache {
    val storage = rememberCacheStorage() // Compose picks the RepositoryCache platform file for the current device.
    return remember(storage, key) { storage.entry(key) } // Reuse the same entry while the storage and key stay the same.
}

@Serializable
internal data class CachedRepository(
    val state: RepositorySnapshot,
    val databaseVerified: Boolean = false,
    val source: String = "",
    val localBookmarks: List<String> = emptyList(),
)

internal const val PUBLIC_CATALOG_SOURCE = "public_catalog_v1"
