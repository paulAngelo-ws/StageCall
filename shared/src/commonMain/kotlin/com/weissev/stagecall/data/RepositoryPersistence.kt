package com.weissev.stagecall.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** Loads the shared catalog and caches successful reads. Guest clients never upload data. */
internal class RepositoryPersistence(
    private val repository: StageCallRepository,
    private val cache: RepositoryCache,
    private val remote: RemoteStateStore?,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val requests = Channel<Unit>(Channel.CONFLATED)
    var loading by mutableStateOf(remote != null)
        private set
    var refreshFailed by mutableStateOf(false)
        private set
    var hasData by mutableStateOf(false)
        private set
    var status by mutableStateOf(
        if (remote == null) "Supabase is not configured" else "Connecting to Supabase..."
    )
        private set

    init {
        try {
            val cached = cache.read()?.let { json.decodeFromString<CachedRepository>(it) }
                ?.takeIf { it.databaseVerified && it.source == PUBLIC_CATALOG_SOURCE }
            cached?.let {
                require(it.state.version == 1) { "Unsupported cache version" }
                repository.restore(publicCatalogSnapshot(it.state.organizations, it.state.posts))
                if (it.localBookmarks.isNotEmpty()) {
                    saveCache(publicCatalogSnapshot(it.state.organizations, it.state.posts))
                }
                hasData = true
                status = "Showing cached database data"
            }
        } catch (_: Exception) {
            status = "Cache unavailable - connect to Supabase"
        }
    }

    private fun saveCache(state: RepositorySnapshot): Boolean = runCatching {
        cache.write(json.encodeToString(CachedRepository(
            state, databaseVerified = true, source = PUBLIC_CATALOG_SOURCE,
        )))
    }.isSuccess

    internal suspend fun synchronize() {
        val store = remote ?: return
        val fetched = store.load()
        require(fetched.version == 1) { "Unsupported remote state version" }
        val state = publicCatalogSnapshot(fetched.organizations, fetched.posts)
        val bookmarks = repository.bookmarks.toList()
        // A storage failure must not hide a successful database response.
        val cacheSaved = saveCache(state)
        // Catalog refresh must not erase the authenticated user's application state.
        val personal = repository.snapshot()
        repository.restore(state.copy(posts = state.posts + personal.posts.filter { it.membersOnly }, profile = personal.profile, applications = personal.applications,
            members = personal.members, notifications = personal.notifications))
        repository.bookmarks.addAll(bookmarks)
        hasData = true
        refreshFailed = false
        status = if (cacheSaved) "Loaded from Supabase" else "Loaded from Supabase - cache unavailable"
    }

    fun retry() { requests.trySend(Unit) }

    suspend fun run() = coroutineScope {
        if (remote == null) return@coroutineScope
        launch {
            while (true) {
                retry()
                delay(30_000)
            }
        }
        for (request in requests) {
            loading = true
            status = if (hasData) "Refreshing from Supabase..." else "Connecting to Supabase..."
            try {
                synchronize()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                refreshFailed = true
                status = if (hasData) "Refresh failed - showing cached data" else "Connection failed - tap to retry"
            } finally {
                loading = false
            }
        }
    }
}
