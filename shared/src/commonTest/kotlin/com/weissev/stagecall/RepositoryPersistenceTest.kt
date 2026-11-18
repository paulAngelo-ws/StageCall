package com.weissev.stagecall

import com.weissev.stagecall.data.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class RepositoryPersistenceTest {
    private class Cache : RepositoryCache {
        var value: String? = null
        var failWrites = false
        override fun read() = value
        override fun write(value: String) {
            if (failWrites) error("Storage unavailable")
            this.value = value
        }
    }

    private class Remote : RemoteStateStore {
        var state = populatedRepository().snapshot()
        var fail = false
        var loads = 0
        override suspend fun load(): RepositorySnapshot {
            loads++
            if (fail) error("offline")
            return state
        }
        override suspend fun close() = Unit
    }

    @Test
    fun repositoryStartsWithoutSampleData() {
        val snapshot = StageCallRepository().snapshot()
        assertTrue(snapshot.organizations.isEmpty())
        assertTrue(snapshot.posts.isEmpty())
        assertTrue(snapshot.applications.isEmpty())
        assertTrue(snapshot.members.isEmpty())
        assertTrue(snapshot.bookmarks.isEmpty())
        assertTrue(snapshot.notifications.isEmpty())
        assertTrue(snapshot.profile.name.isEmpty())
        assertTrue(snapshot.profile.email.isEmpty())
        assertTrue(snapshot.profile.studentId.isEmpty())
    }

    @Test
    fun databaseLoadsPublicCatalogWithoutPersonalData() = runTest {
        val repository = StageCallRepository()
        val remote = Remote()
        val persistence = RepositoryPersistence(repository, Cache(), remote)
        assertFalse(persistence.hasData)
        persistence.synchronize()
        assertEquals(remote.state.organizations, repository.organizations.toList())
        assertEquals(remote.state.posts, repository.posts.toList())
        assertTrue(repository.profile.studentId.isEmpty())
        assertTrue(repository.members.isEmpty())
        assertTrue(repository.applications.isEmpty())
        assertTrue(repository.bookmarks.isEmpty())
        assertEquals("Loaded from Supabase", persistence.status)
    }

    @Test
    fun catalogRefreshPreservesSignedInApplications() = runTest {
        val repository = StageCallRepository()
        val persistence = RepositoryPersistence(repository, Cache(), Remote())
        val user = Student("Student", "student@example.com", "user-id")
        val application = MembershipApplication(42, "1", user, "2nd Year", "To learn")
        repository.setAccount(user)
        repository.setApplications(listOf(application))
        val membership = Member("1", user, "President")
        repository.setMemberships(listOf(membership))
        persistence.synchronize()
        assertEquals(user, repository.profile)
        assertEquals(listOf(application), repository.applications.toList())
        assertEquals(listOf(membership), repository.members.toList())
        assertTrue(repository.isMember("1"))
        assertFalse(repository.isMember("2"))
        assertFalse(repository.canApply("1"))
    }

    @Test
    fun verifiedCatalogCacheRestoresOffline() = runTest {
        val cache = Cache()
        val repository = StageCallRepository()
        RepositoryPersistence(repository, cache, Remote()).synchronize()
        val restored = StageCallRepository()
        val reopened = RepositoryPersistence(restored, cache, null)
        assertTrue(reopened.hasData)
        assertEquals("Showing cached database data", reopened.status)
        assertEquals(repository.snapshot(), restored.snapshot())
    }

    @Test
    fun privateSnapshotCacheIsIgnored() {
        val cache = Cache().apply {
            value = Json.encodeToString(CachedRepository(populatedRepository().snapshot(), databaseVerified = true))
        }
        val repository = StageCallRepository()
        val persistence = RepositoryPersistence(repository, cache, Remote())
        assertFalse(persistence.hasData)
        assertTrue(repository.organizations.isEmpty())
        assertTrue(repository.profile.name.isEmpty())
    }

    @Test
    fun failedFirstConnectionLeavesTheAppEmpty() = runTest {
        val repository = StageCallRepository()
        val persistence = RepositoryPersistence(repository, Cache(), Remote().apply { fail = true })
        assertFailsWith<IllegalStateException> { persistence.synchronize() }
        assertFalse(persistence.hasData)
        assertTrue(repository.organizations.isEmpty())
    }

    @Test
    fun subsequentFetchReflectsDatabaseEdits() = runTest {
        val repository = StageCallRepository()
        val remote = Remote()
        val persistence = RepositoryPersistence(repository, Cache(), remote)
        persistence.synchronize()
        remote.state = remote.state.copy(organizations = remote.state.organizations.map {
            if (it.id == "1") it.copy(name = "Database edit") else it
        })
        persistence.synchronize()
        assertEquals("Database edit", repository.organization("1").name)
        assertEquals(2, remote.loads)
    }

    @Test
    fun emptyTablesDoNotGenerateFallbackRecords() = runTest {
        val remote = Remote().apply { state = StageCallRepository().snapshot() }
        val repository = StageCallRepository()
        val persistence = RepositoryPersistence(repository, Cache(), remote)
        persistence.synchronize()
        assertTrue(persistence.hasData)
        assertTrue(repository.organizations.isEmpty())
        assertTrue(repository.posts.isEmpty())
        assertEquals("Loaded from Supabase", persistence.status)
    }

    @Test
    fun cacheFailureStillDisplaysSuccessfulDatabaseRead() = runTest {
        val repository = StageCallRepository()
        val persistence = RepositoryPersistence(repository, Cache().apply { failWrites = true }, Remote())
        persistence.synchronize()
        assertTrue(persistence.hasData)
        assertEquals(5, repository.organizations.size)
        assertEquals("Loaded from Supabase - cache unavailable", persistence.status)
    }

    @Test
    fun privatePostsAreExcludedFromGuestDataAndCache() = runTest {
        val cache = Cache()
        val remote = Remote().apply {
            state = state.copy(posts = state.posts + state.posts.first().copy(id = 99, membersOnly = true))
        }
        val repository = StageCallRepository()
        RepositoryPersistence(repository, cache, remote).synchronize()
        assertTrue(repository.posts.none { it.membersOnly || it.id == 99 })
        val cached = Json.decodeFromString<CachedRepository>(cache.value!!)
        assertTrue(cached.state.posts.none { it.membersOnly || it.id == 99 })
    }

    @Test
    fun guestsCannotEditPersonalData() = runTest {
        val repository = StageCallRepository()
        RepositoryPersistence(repository, Cache(), Remote()).synchronize()
        assertFalse(repository.canApply("1"))
        assertFalse(repository.submit("1", "2nd Year", "Join", true))
        assertFalse(repository.updateProfile("Guest edit", "guest@example.com"))
    }

    @Test
    fun guestBookmarksToggleAndIgnoreUnknownOrganizations() = runTest {
        val repository = StageCallRepository()
        RepositoryPersistence(repository, Cache(), Remote()).synchronize()
        repository.toggleBookmark("1")
        assertEquals(listOf("1"), repository.bookmarks.toList())
        repository.toggleBookmark("missing")
        assertEquals(listOf("1"), repository.bookmarks.toList())
        repository.toggleBookmark("1")
        assertTrue(repository.bookmarks.isEmpty())
    }

    @Test
    fun localBookmarksSurviveRefreshAndRestartAndPersistRemoval() = runTest {
        val cache = Cache()
        val repository = StageCallRepository()
        val persistence = RepositoryPersistence(repository, cache, Remote())
        persistence.synchronize()
        repository.toggleBookmark("2")
        persistence.synchronize()
        assertEquals(listOf("2"), repository.bookmarks.toList())

        val reopened = StageCallRepository()
        RepositoryPersistence(reopened, cache, null)
        assertEquals(listOf("2"), reopened.bookmarks.toList())
        assertTrue(reopened.profile.studentId.isEmpty())
        reopened.toggleBookmark("2")

        val restored = StageCallRepository()
        RepositoryPersistence(restored, cache, null)
        assertTrue(restored.bookmarks.isEmpty())
    }

    @Test
    fun refreshDropsBookmarksForDeletedOrganizations() = runTest {
        val repository = StageCallRepository()
        val remote = Remote()
        val persistence = RepositoryPersistence(repository, Cache(), remote)
        persistence.synchronize()
        repository.toggleBookmark("1")
        remote.state = remote.state.copy(organizations = remote.state.organizations.filter { it.id != "1" })
        persistence.synchronize()
        assertTrue(repository.bookmarks.isEmpty())
    }

    @Test
    fun malformedCacheDoesNotCrashStartup() {
        val repository = StageCallRepository()
        RepositoryPersistence(repository, Cache().apply { value = "broken json" }, null)
        assertTrue(repository.organizations.isEmpty())
    }
}
