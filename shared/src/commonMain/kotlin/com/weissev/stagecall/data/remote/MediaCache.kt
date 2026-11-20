package com.weissev.stagecall.data.remote

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.weissev.stagecall.data.CacheStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.time.Clock

// App.kt provides one cache per signed-in account. FeedScreen.kt and ProfileScreen.kt use it to load images.
internal val LocalMediaCache = compositionLocalOf<MediaCache?> { null }

@Serializable
private data class MediaEntry(
    val slot: Int,
    val size: Int,
    val etag: String? = null,
    val modified: String? = null,
    val nextCheck: Long = 0,
)

internal class MediaCache(
    private val client: SupabaseClient,
    private val account: String,
    private val storage: CacheStorage,
) {
    private val prefix = "media-$cacheProjectKey-$account"
    private val index = storage.entry("$prefix-index")
    private val lock = Mutex()
    private val postLocks = List(16) { Mutex() }
    private val avatarLock = Mutex()
    private val entries = runCatching {
        index.read()?.let { cacheJson.decodeFromString<Map<String, MediaEntry>>(it) }
    }.getOrNull().orEmpty().toMutableMap()
    private var checkedAvatar: String? = null
    private var checkedAt = 0L

    // FeedScreen.kt sends the storage path from the post. Uploads in OrganizationRoleStore.kt get unique paths, so the same path can reuse its image.
    suspend fun post(path: String): ImageBitmap = storedImage("post-images", path, "post:$path")

    suspend fun organizationPhoto(path: String): ImageBitmap =
        storedImage("organization-photos", path, "organization-photo:$path")

    private suspend fun storedImage(bucket: String, path: String, key: String): ImageBitmap = withContext(Dispatchers.Default) {
        val transfer = postLocks[key.hashCode().mod(postLocks.size)] // Make requests for the same path wait for each other.
        transfer.withLock {
            check(client.auth.currentUserOrNull()?.id == account)
            lock.withLock { read(key) }?.let { return@withLock it } // Return the local image as soon as it can be read.
            val bytes = client.storage.from(bucket).downloadAuthenticated(path) // Download through the signed-in client if the image is absent.
            val image = bytes.decodeToImageBitmap()
            if (client.auth.currentUserOrNull()?.id == account) lock.withLock { save(key, bytes) }
            image
        }
    }

    suspend fun cachedAvatar(url: String): ImageBitmap? = withContext(Dispatchers.Default) {
        lock.withLock { read("avatar:$url") }
    }

    suspend fun avatar(url: String, cached: ImageBitmap?): ImageBitmap? = withContext(Dispatchers.Default) {
        avatarLock.withLock { refreshAvatar(url, cached) }
    }

    private suspend fun refreshAvatar(url: String, cached: ImageBitmap?): ImageBitmap? {
        if (!url.startsWith("https://") || client.auth.currentUserOrNull()?.id != account) return null
        val now = Clock.System.now().toEpochMilliseconds()
        if (cached != null && checkedAvatar == url && now - checkedAt in 0 until AVATAR_REFRESH_MS) {
            return cached
        }
        val key = "avatar:$url"
        val entry = lock.withLock { entries[key]?.takeIf { cached != null } }
        if (entry != null && now < entry.nextCheck) return cached
        val http = HttpClient()
        try {
            // Send the saved ETag or modification date to the photo host when available. A 304 response lets ProfileScreen.kt keep the cached photo.
            val response = http.get(url) {
                entry?.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                entry?.modified?.let { header(HttpHeaders.IfModifiedSince, it) }
            }
            val image = when {
                response.status == HttpStatusCode.NotModified -> cached // Reuse the picture when the host says it is unchanged.
                response.status.isSuccess() -> {
                    val bytes = response.body<ByteArray>()
                    bytes.decodeToImageBitmap().also {
                        if (client.auth.currentUserOrNull()?.id == account) {
                            lock.withLock {
                                val etag = response.headers[HttpHeaders.ETag]
                                val modified = response.headers[HttpHeaders.LastModified]
                                // Without a validator, reuse the photo for a short period across launches.
                                val nextCheck = if (etag == null && modified == null) now + AVATAR_REFRESH_MS else 0
                                save(key, bytes, etag, modified, nextCheck)
                            }
                        }
                    }
                }
                else -> return cached
            }
            checkedAvatar = url
            checkedAt = now
            return image
        } finally {
            http.close()
        }
    }

    private suspend fun read(key: String): ImageBitmap? {
        val entry = entries.remove(key) ?: return null
        // Move recently viewed entries to the end of the eviction order.
        entries[key] = entry // Move this image to the end so older images are removed first.
        return attempt {
            // Check the key saved with the image because an evicted slot can later be reused for a different image.
            val stored = storage.readMedia(fileKey(entry.slot)) ?: return null
            val header = "$key\n"
            if (stored.startsWith(header)) Base64.decode(stored.substring(header.length)).decodeToImageBitmap() else null
        }
    }

    private suspend fun save(
        key: String,
        bytes: ByteArray,
        etag: String? = null,
        modified: String? = null,
        nextCheck: Long = 0,
    ) {
        if (bytes.size > MAX_MEDIA_BYTES) return
        entries.remove(key)?.let { attempt { storage.removeMedia(fileKey(it.slot)) } }
        while (entries.size >= MAX_MEDIA_ENTRIES || entries.values.sumOf { it.size } + bytes.size > MAX_MEDIA_BYTES) { // Remove older photos until the new one fits the cache limits.
            evictOldest()
        }
        val occupied = entries.values.map { it.slot }.toSet()
        val slot = (0 until MAX_MEDIA_ENTRIES).first { it !in occupied }
        val encoded = "$key\n${Base64.encode(bytes)}"
        // Browsers can impose a lower quota than the app's media limit.
        while (true) {
            val saved = attempt { storage.writeMedia(fileKey(slot), encoded) } != null
            if (saved) entries[key] = MediaEntry(slot, bytes.size, etag, modified, nextCheck)
            if (saved || entries.isEmpty()) break
            evictOldest()
        }
        if (runCatching { index.write(cacheJson.encodeToString(entries.toMap())) }.isFailure) {
            // Slot numbers can be reused, so don't leave an old index pointing at new bytes.
            runCatching { index.remove() }
        }
    }

    private suspend fun evictOldest() {
        val key = entries.keys.first()
        entries.remove(key)?.let { attempt { storage.removeMedia(fileKey(it.slot)) } }
    }

    private fun fileKey(slot: Int) = "$prefix-$slot"

    private inline fun <T> attempt(operation: () -> T): T? = try {
        operation()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val MAX_MEDIA_ENTRIES = 48
        const val MAX_MEDIA_BYTES = 32 * 1024 * 1024
        const val AVATAR_REFRESH_MS = 15 * 60 * 1000L
    }
}
