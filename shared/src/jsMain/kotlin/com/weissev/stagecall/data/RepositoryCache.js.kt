package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.js.Promise
import kotlinx.coroutines.await

private fun readState(key: String): String? = js("localStorage.getItem(key)") as String?
private fun writeState(key: String, value: String) { js("localStorage.setItem(key, value)") }

private fun removeState(key: String): Unit = js("localStorage.removeItem(key)")

// Store images from MediaCache.kt in the browser Cache API. Keep the smaller row snapshots in local storage.
private fun readMediaEntry(key: String): Promise<String?> = js("""
    caches.open('stagecall-media-v1').then(function(cache) {
        return cache.match(location.origin + '/.stagecall-media/' + encodeURIComponent(key));
    }).then(function(response) {
        return response ? response.text() : null;
    })
""")

private fun writeMediaEntry(key: String, value: String): Promise<String?> = js("""
    caches.open('stagecall-media-v1').then(function(cache) {
        return cache.put(location.origin + '/.stagecall-media/' + encodeURIComponent(key), new Response(value));
    }).then(function() { return null; })
""")

private fun removeMediaEntry(key: String): Promise<String?> = js("""
    caches.open('stagecall-media-v1').then(function(cache) {
        return cache.delete(location.origin + '/.stagecall-media/' + encodeURIComponent(key));
    }).then(function() { return null; })
""")

@Composable
internal actual fun rememberCacheStorage(): CacheStorage = remember {
    object : CacheStorage {
        override fun entry(key: String): RepositoryCache {
            val storageKey = if (key == "state") "stagecall_state_v1" else "stagecall_$key"
            return object : RepositoryCache {
                override fun read(): String? = readState(storageKey)
                override fun remove() = removeState(storageKey)
                override fun write(value: String) = writeState(storageKey, value)
            }
        }

        override suspend fun readMedia(key: String): String? = readMediaEntry(key).await() // Read a saved image through the browser Cache API.
        override suspend fun writeMedia(key: String, value: String) { writeMediaEntry(key, value).await() }
        override suspend fun removeMedia(key: String) { removeMediaEntry(key).await() }
    }
}
