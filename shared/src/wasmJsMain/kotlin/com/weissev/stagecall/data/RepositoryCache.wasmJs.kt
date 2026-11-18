@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlin.js.JsString
import kotlin.js.js

private fun readState(key: String): String? = js("localStorage.getItem(key)")
private fun writeState(key: String, value: String): Unit = js("localStorage.setItem(key, value)")

private fun removeState(key: String): Unit = js("localStorage.removeItem(key)")

// Use the same browser keys as RepositoryCache.js.kt so either web target can read the saved images.
private fun readMediaEntry(key: String): Promise<JsString?> = js("""
    caches.open('stagecall-media-v1').then(function(cache) {
        return cache.match(location.origin + '/.stagecall-media/' + encodeURIComponent(key));
    }).then(function(response) {
        return response ? response.text() : null;
    })
""")

private fun writeMediaEntry(key: String, value: String): Promise<JsString?> = js("""
    caches.open('stagecall-media-v1').then(function(cache) {
        return cache.put(location.origin + '/.stagecall-media/' + encodeURIComponent(key), new Response(value));
    }).then(function() { return null; })
""")

private fun removeMediaEntry(key: String): Promise<JsString?> = js("""
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

        override suspend fun readMedia(key: String): String? = readMediaEntry(key).await()?.toString() // Convert the saved JavaScript string into Kotlin text.
        override suspend fun writeMedia(key: String, value: String) { writeMediaEntry(key, value).await() }
        override suspend fun removeMedia(key: String) { removeMediaEntry(key).await() }
    }
}
