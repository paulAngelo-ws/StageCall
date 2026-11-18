package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberCacheStorage(): CacheStorage = remember {
    CacheStorage { key ->
        // RepositoryCache.kt sends the key here. User defaults keep each saved value available across app launches.
        val storageKey = if (key == "state") "stagecall_state_v1" else "stagecall_$key"
        object : RepositoryCache {
            override fun read(): String? = NSUserDefaults.standardUserDefaults.stringForKey(storageKey)
            override fun remove() { NSUserDefaults.standardUserDefaults.removeObjectForKey(storageKey) }
            override fun write(value: String) {
                NSUserDefaults.standardUserDefaults.setObject(value, forKey = storageKey) // Keep the value under the key chosen by RepositoryCache.kt.
            }
        }
    }
}
