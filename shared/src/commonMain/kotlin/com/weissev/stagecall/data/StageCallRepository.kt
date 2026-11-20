package com.weissev.stagecall.data

import androidx.compose.runtime.*
import kotlin.time.Clock

class StageCallRepository {
    // RepositoryPersistence.kt takes this snapshot to preserve personal data while refreshing the catalog.
    internal fun snapshot() = RepositorySnapshot(
        organizations.toList(), profile, bookmarks.toList(), applications.toList(),
        members.toList(), posts.toList(), notificationHistory.toList(),
    )

    // Replacing values in these Compose state lists updates the screens that read the repository.
    internal fun restore(snapshot: RepositorySnapshot) {
        organizations.clear()
        organizations.addAll(snapshot.organizations)
        profile = snapshot.profile
        bookmarks.clear()
        bookmarks.addAll(snapshot.bookmarks) // Restore bookmark IDs from the supplied snapshot.
        applications.clear()
        applications.addAll(snapshot.applications)
        members.clear()
        members.addAll(snapshot.members)
        posts.clear()
        posts.addAll(snapshot.posts)
        notificationHistory.clear()
        notificationHistory.addAll(snapshot.notifications)
        nextNotificationId = (snapshot.notifications.maxOfOrNull { it.id } ?: 0) + 1
    }

    val organizations = mutableStateListOf<Organization>()
    var profile by mutableStateOf(Student("", "", ""))
        private set
    internal fun setAccount(student: Student) { profile = student }
    // ApplicationStore.kt sends the signed-in student’s saved and refreshed applications here.
    internal fun setApplications(value: List<MembershipApplication>) {
        applications.clear()
        applications.addAll(value) // Replace the old applications with the list from ApplicationStore.kt.
    }
    // OrganizationRoleStore.kt sends confirmed memberships here. ProfileScreen.kt uses them to show joined organizations.
    internal fun setMemberships(value: List<Member>) {
        members.clear()
        members.addAll(value) // Replace the old memberships with the confirmed list from OrganizationRoleStore.kt.
    }
    val bookmarks = mutableStateListOf<String>()
    internal var onBookmarksChanged: () -> Unit = {}
    val applications = mutableStateListOf<MembershipApplication>() // Compose screens update when ApplicationStore.kt changes this list.
    val members = mutableStateListOf<Member>()
    val posts = mutableStateListOf<CampusPost>()
    private val notificationHistory = mutableStateListOf<StageCallNotification>()
    private var nextNotificationId = 1

    val notifications: List<StageCallNotification>
        get() = notificationHistory.filter {
            if (it.type == NotificationType.Application) {
                applications.any { application ->
                    application.id == it.applicationId &&
                        application.student.studentId == profile.studentId
                }
            } else isMember(it.organizationId)
        }.sortedByDescending { it.id }

    val unreadNotificationCount: Int
        get() = notifications.count { !it.isRead }

    private fun notify(
        organizationId: String,
        type: NotificationType,
        title: String,
        message: String,
        postId: Int? = null,
        applicationId: Int? = null,
    ) {
        if (type != NotificationType.Application && !isMember(organizationId)) return
        notificationHistory.add(
            StageCallNotification(
                id = nextNotificationId++,
                organizationId = organizationId,
                type = type,
                title = title,
                message = message,
                postId = postId,
                applicationId = applicationId,
            )
        )
    }

    fun markNotificationRead(id: Int) {
        if (notifications.none { it.id == id }) return
        val index = notificationHistory.indexOfFirst { it.id == id }
        notificationHistory[index] = notificationHistory[index].copy(isRead = true)
    }

    fun markAllNotificationsRead() {
        notifications.filter { !it.isRead }.forEach { markNotificationRead(it.id) }
    }

    fun updateOrganization(updated: Organization): Boolean {
        val index = organizations.indexOfFirst { it.id == updated.id }
        if (index < 0 || updated.name.isBlank() || updated.description.isBlank()) return false
        val previous = organizations[index]
        if (previous == updated) return true // Treat a save with no edits as successful without creating an update.
        organizations[index] = updated
        val activityChanged = previous.nextActivityTitle != updated.nextActivityTitle ||
            previous.nextActivitySchedule != updated.nextActivitySchedule
        val changes = buildList { // Collect the specific changes that should appear in notifications.
            if (previous.name != updated.name) add("Organization name updated to ${updated.name}.")
            if (previous.isRecruiting != updated.isRecruiting)
                add(if (updated.isRecruiting) "Recruitment is now open." else "Recruitment is now closed.")
            if (activityChanged) {
                add("Activity updated: ${updated.nextActivityTitle}.")
                if (updated.nextActivitySchedule.isNotBlank()) add(updated.nextActivitySchedule)
            }
            if (previous.description != updated.description || previous.fullDescription != updated.fullDescription)
                add("Organization information has been updated.")
        }
        notify(
            updated.id,
            if (activityChanged) NotificationType.Activity else NotificationType.Organization,
            if (activityChanged) "Activity details updated" else "Organization updated",
            changes.joinToString(" ").ifBlank { "Organization details have changed." },
        )
        return true
    }

    fun organization(id: String) = organizations.first { it.id == id }

    fun isMember(id: String) =
        members.any {
            it.organizationId == id && it.student.studentId == profile.studentId && it.active
        }

    // A student can apply only when all three membership rules pass.
    fun canApply(id: String): Boolean {
        if (organizations.none { it.id == id && it.isRecruiting }) return false
        if (profile.studentId.isBlank()) return false
        if (isMember(id)) return false

        val alreadyApplied =
            applications.any { application ->
                application.organizationId == id &&
                    application.student.studentId == profile.studentId
            }
        return !alreadyApplied
    }

    fun toggleBookmark(id: String) {
        if (organizations.none { it.id == id }) return
        if (id in bookmarks) {
            bookmarks.remove(id)
        } else {
            bookmarks.add(id)
        }
        onBookmarksChanged()
    }

    // Return false when the form is incomplete or an application already exists.
    fun submit(id: String, year: String, reason: String, consent: Boolean): Boolean {
        if (!canApply(id) || reason.isBlank() || !consent) return false
        applications.add(
            MembershipApplication(
                (applications.maxOfOrNull { it.id } ?: 0) + 1,
                id,
                profile,
                year,
                reason.trim(),
            )
        )
        notify(
            id, NotificationType.Application, "Application submitted",
            "Your membership application has been sent for review.",
            applicationId = applications.last().id,
        )
        return true
    }

    fun decide(id: Int, accepted: Boolean, message: String) {
        val index = applications.indexOfFirst { it.id == id }
        if (index < 0 || applications[index].status != ApplicationStatus.Pending) return
        // Replace the item in the state list so Compose refreshes the screens.
        val app = applications[index]
        applications[index] =
            app.copy(
                status = if (accepted) ApplicationStatus.Accepted else ApplicationStatus.Rejected,
                decision = message,
                updatedAt = Clock.System.now(),
            )
        if (
            accepted &&
                members.none {
                    it.organizationId == app.organizationId &&
                        it.student.studentId == app.student.studentId
                }
        ) {
            members.add(Member(app.organizationId, app.student))
        }
        if (app.student.studentId == profile.studentId)
            notify(
                app.organizationId, NotificationType.Application,
                "Application ${applications[index].status.name.lowercase()}",
                message.ifBlank { "Your membership application has been ${applications[index].status.name.lowercase()}." },
                applicationId = app.id,
            )
    }

    fun publish(
        orgId: String,
        title: String,
        message: String,
        category: String,
        privatePost: Boolean,
        schedule: String,
    ): Boolean {
        if (title.isBlank() || message.isBlank())
            return false
        posts.add(
            0,
            CampusPost(
                (posts.maxOfOrNull { it.id } ?: 0) + 1,
                orgId,
                title.trim(),
                message.trim(),
                category,
                privatePost,
                schedule,
            ),
        )
        val post = posts.first()
        notify(
            orgId,
            if (category == "Events") NotificationType.Activity else NotificationType.Post,
            when (category) {
                "Events" -> "New activity: ${post.title}"
                "Announcements" -> "New update: ${post.title}"
                else -> "New post: ${post.title}"
            },
            if (schedule.isNotBlank()) "$schedule · ${post.message}" else post.message,
            postId = post.id,
        )
        return true
    }

    fun updateProfile(name: String, email: String): Boolean {
        if (profile.studentId.isBlank()) return false
        if (name.isBlank() || !email.contains('@')) return false
        profile = profile.copy(name = name.trim(), email = email.trim())
        return true
    }
}
