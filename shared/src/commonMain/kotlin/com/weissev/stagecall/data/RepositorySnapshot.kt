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

// RepositoryPersistence.kt uses this when reading or writing catalog data. Keep private account fields out of that cache.
internal fun publicCatalogSnapshot(organizations: List<Organization>, posts: List<CampusPost>): RepositorySnapshot {
    val organizationIds = organizations.map { it.id }.toSet() // Collect the valid organization IDs before filtering the public feed.
    return RepositorySnapshot(
        organizations = organizations,
        profile = Student("", "", ""),
        bookmarks = emptyList(),
        applications = emptyList(),
        members = emptyList(),
        posts = posts.filter { !it.membersOnly && it.organizationId in organizationIds }, // Only save public posts that reference an organization in this snapshot.
        notifications = emptyList(),
    )
}
