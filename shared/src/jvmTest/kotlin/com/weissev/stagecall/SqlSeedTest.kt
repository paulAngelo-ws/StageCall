package com.weissev.stagecall

import com.weissev.stagecall.data.CampusPost
import com.weissev.stagecall.data.Organization
import com.weissev.stagecall.data.RepositorySnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

// Compare 202611180003_public_catalog.sql with the data models and DemoFixtures.kt.
class SqlSeedTest {
    @Test
    fun publicCatalogMigrationSeedsRowsWithoutPersonalData() {
        val relative = Path.of("supabase/migrations/202611180003_public_catalog.sql")
        val path = sequenceOf(relative, Path.of("..").resolve(relative))
            .first { Files.exists(it) }
        val sql = Files.readString(path)
        val delimiter = "\$catalog_seed\$"
        val catalog = Json.parseToJsonElement(sql.substringAfter(delimiter).substringBefore(delimiter)).jsonObject
        assertEquals(setOf("organizations", "posts"), catalog.keys)
        val organizations = catalog.getValue("organizations").jsonArray.map {
            Json.decodeFromJsonElement<Organization>(it)
        }
        val posts = catalog.getValue("posts").jsonArray.map { Json.decodeFromJsonElement<CampusPost>(it) }
        val fixture = populatedRepository()
        assertEquals(fixture.organizations.toList(), organizations)
        val fixedTime = Instant.parse("2026-11-18T00:00:00Z")
        assertEquals(
            fixture.posts.map { it.copy(createdAt = fixedTime) },
            posts.map { it.copy(createdAt = fixedTime) },
        )
    }

    @Test
    fun databaseSeedMatchesModelContractAndTestFixtures() {
        val relative = Path.of("supabase/migrations/202611180002_seed_demo_data.sql")
        val path = sequenceOf(relative, Path.of("..").resolve(relative))
            .first { Files.exists(it) }
        val sql = Files.readString(path)
        val delimiter = "\$stagecall_seed\$"
        val json = sql.substringAfter(delimiter).substringBefore(delimiter)
        val seeded = Json.decodeFromString<RepositorySnapshot>(json)
        val fixture = populatedRepository().snapshot()

        // The SQL function assigns timestamps when it initializes each account;
        // test fixtures assign timestamps when constructed in the test process.
        val fixedTime = Instant.parse("2026-11-18T00:00:00Z")
        fun RepositorySnapshot.withFixedTimes() = copy(
            applications = applications.map { it.copy(submittedAt = fixedTime) },
            posts = posts.map { it.copy(createdAt = fixedTime) },
        )
        assertEquals(fixture.withFixedTimes(), seeded.withFixedTimes())
        assertEquals(5, seeded.organizations.size)
        assertEquals(3, seeded.applications.size)
        assertEquals(2, seeded.members.size)
        assertEquals(3, seeded.posts.size)
        assertTrue(seeded.notifications.isEmpty())
    }
}
