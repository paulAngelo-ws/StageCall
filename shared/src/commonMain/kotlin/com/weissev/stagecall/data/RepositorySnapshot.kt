package com.weissev.stagecall.data

import kotlinx.serialization.Serializable

/** Repository state shared by screens; guest catalog reads leave personal fields empty. */
@Serializable
internal data class RepositorySnapshot(
    val organizations: List<Organization>,
    val profile: Student,
    val bookmarks: List<String>,
    val applications: List<MembershipApplication>,
    val members: List<Member>,
    val posts: List<CampusPost>,
    val notifications: List<StageCallNotification>,
    val version: Int = 1,
)

@Serializable
internal data class CachedRepository(
    val state: RepositorySnapshot,
    val databaseVerified: Boolean = false,
    val source: String = "",
    val localBookmarks: List<String> = emptyList(),
)

internal const val PUBLIC_CATALOG_SOURCE = "public_catalog_v1"

internal fun publicCatalogSnapshot(organizations: List<Organization>, posts: List<CampusPost>): RepositorySnapshot {
    val organizationIds = organizations.map { it.id }.toSet()
    return RepositorySnapshot(
        organizations = organizations,
        profile = Student("", "", ""),
        bookmarks = emptyList(),
        applications = emptyList(),
        members = emptyList(),
        posts = posts.filter { !it.membersOnly && it.organizationId in organizationIds },
        notifications = emptyList(),
    )
}
