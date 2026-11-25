package com.weissev.stagecall.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal interface RemoteStateStore {
    suspend fun load(): RepositorySnapshot
    suspend fun close()
}

/** Loads the shared catalog and caches successful reads. Guest clients never upload data. */
internal class RepositoryPersistence(
    private val repository: StageCallRepository,
    private val cache: RepositoryCache,
    private val remote: RemoteStateStore?,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var savedCatalog: RepositorySnapshot? = null
    // A notification can request a refresh while the timer is running. Finish one catalog refresh before starting the next.
    private val syncLock = Mutex()
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
            // Only restore a verified public snapshot from RepositoryCache.kt before contacting SupabaseStateStore.kt.
            val cached = cache.read()?.let { json.decodeFromString<CachedRepository>(it) }
                ?.takeIf { it.databaseVerified && it.source == PUBLIC_CATALOG_SOURCE }
            cached?.let {
                require(it.state.version == 1) { "Unsupported cache version" }
                val catalog = publicCatalogSnapshot(it.state.organizations, it.state.posts)
                repository.restore(catalog)
                savedCatalog = catalog
                if (it.localBookmarks.isNotEmpty()) saveCache(catalog)
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
        savedCatalog = state
    }.isSuccess

    internal suspend fun synchronize() = syncLock.withLock {
        val store = remote ?: return@withLock
        // SupabaseStateStore.kt returns the catalog snapshot. The merged data goes to StageCallRepository.kt for the screens.
        val fetched = store.load() // SupabaseStateStore.kt supplies organizations and public posts.
        require(fetched.version == 1) { "Unsupported remote state version" }
        val state = publicCatalogSnapshot(fetched.organizations, fetched.posts) // Build a snapshot that can be stored without personal records.
        val bookmarks = repository.bookmarks.toList()
        // A storage failure must not hide a successful database response.
        val cacheSaved = state == savedCatalog || saveCache(state)
        // Catalog refresh must not erase the authenticated user's application state.
        val personal = repository.snapshot() // Take the current application, membership, and profile data from StageCallRepository.kt.
        val organizationIds = state.organizations.map { it.id }.toSet()
        val merged = state.copy(
            posts = state.posts + personal.posts.filter { it.membersOnly && it.organizationId in organizationIds },
            profile = personal.profile,
            bookmarks = bookmarks,
            applications = personal.applications.filter { it.organizationId in organizationIds },
            members = personal.members.filter { it.organizationId in organizationIds },
            notifications = personal.notifications.filter { it.organizationId in organizationIds },
        )
        if (merged != personal) repository.restore(merged) // Update StageCallRepository.kt only when the result differs.
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
                delay(30_000) // Wait 30 seconds before asking for another refresh.
            }
        }
        // A manual retry and the timer share this loop. The conflated channel keeps only one waiting request.
        for (request in requests) {
            loading = true
            status = if (hasData) "Refreshing from Supabase..." else "Connecting to Supabase..."
            try {
                synchronize()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                refreshFailed = true // App.kt can show the saved feed with a retry message.
                status = if (hasData) "Refresh failed - showing cached data" else "Connection failed - tap to retry"
            } finally {
                loading = false
            }
        }
    }
}
