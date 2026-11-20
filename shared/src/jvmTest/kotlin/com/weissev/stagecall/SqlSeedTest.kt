package com.weissev.stagecall

import com.weissev.stagecall.data.CampusPost
import com.weissev.stagecall.data.Organization
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SqlSeedTest {
    @Test
    fun sampleCatalogSeedsRowsWithoutPersonalData() {
        val relative = Path.of("supabase/seed.sql")
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
        assertEquals(fixture.organizations.map { it.copy(memberCount = 0) }, organizations)
        val fixedTime = Instant.parse("2026-11-18T00:00:00Z")
        assertEquals(
            fixture.posts.map { it.copy(createdAt = fixedTime) },
            posts.map { it.copy(createdAt = fixedTime) },
        )
    }
}
