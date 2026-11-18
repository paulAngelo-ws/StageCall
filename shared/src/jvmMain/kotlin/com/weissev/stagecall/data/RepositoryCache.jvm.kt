package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

@Composable
internal actual fun rememberCacheStorage(): CacheStorage = remember {
    CacheStorage { key ->
        // Preferences has a small per-value limit; snapshots belong in a file.
        val directory = Path.of(System.getProperty("user.home"), ".stagecall")
        // Resolve the key from RepositoryCache.kt to its own desktop file. Use the existing name for the legacy catalog.
        val target = directory.resolve(if (key == "state") "state-v1.json" else "$key.json")
        object : RepositoryCache {
            override fun read(): String? = if (Files.exists(target)) Files.readString(target) else null
            override fun remove() { Files.deleteIfExists(target) }
            override fun write(value: String) {
                Files.createDirectories(directory) // Create the cache folder on the first save.
                // Write a temporary file before replacing the saved copy so a partial write does not corrupt the cache.
                val temporary = Files.createTempFile(directory, "state-", ".tmp")
                try {
                    Files.writeString(temporary, value)
                    try {
                        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                } finally {
                    Files.deleteIfExists(temporary) // Clean up the temporary file after a successful or failed write.
                }
            }
        }
    }
}
