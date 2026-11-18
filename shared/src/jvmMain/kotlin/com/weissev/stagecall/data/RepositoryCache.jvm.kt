package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

@Composable
internal actual fun rememberRepositoryCache(): RepositoryCache = remember {
    // Preferences has a small per-value limit; snapshots belong in a file.
    val directory = Path.of(System.getProperty("user.home"), ".stagecall")
    val target = directory.resolve("state-v1.json")
    object : RepositoryCache {
        override fun read(): String? = if (Files.exists(target)) Files.readString(target) else null
        override fun write(value: String) {
            Files.createDirectories(directory)
            val temporary = Files.createTempFile(directory, "state-", ".tmp")
            try {
                Files.writeString(temporary, value)
                try {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
    }
}
