package com.weissev.stagecall.data

import kotlinx.serialization.Serializable

import kotlin.time.Clock
import kotlin.time.Instant

// These small classes describe the information used by our screens.
@Serializable
enum class ApplicationStatus {
    Pending,
    Accepted,
    Rejected,
}

@Serializable
// App.kt creates the signed-in student from the Google session and puts it in StageCallRepository.kt.
data class Student(val name: String, val email: String, val studentId: String = "") // App.kt supplies the signed-in account ID as studentId.

// A student’s request to join an existing organization.
@Serializable
data class MembershipApplication(
    val id: Int,
    val organizationId: String, // Links this request to the organization shown in ApplicationsScreen.kt.
    val student: Student,
    val year: String,
    val reason: String,
    val status: ApplicationStatus = ApplicationStatus.Pending,
    val decision: String = "",
    val submittedAt: Instant = Clock.System.now(),
    val updatedAt: Instant? = null, // ApplicationsScreen.kt uses the submission date if no update date is saved.
    val studentNumber: String = "",
)

// An organization update with an optional image and a visibility setting.
@Serializable
// SupabaseStateStore.kt converts database rows to this model. FeedScreen.kt renders it, including posts opened from OrganizationScreen.kt.
data class CampusPost(
    val id: Int,
    val organizationId: String,
    val title: String,
    val message: String,
    val category: String,
    val membersOnly: Boolean = false,
    val schedule: String = "",
    val createdAt: Instant = Clock.System.now(),
    val imagePath: String? = null, // FeedScreen.kt gives this storage path to MediaCache.kt.
)

// Connects a student to an organization and records their role.
@Serializable
data class Member(
    val organizationId: String,
    val student: Student,
    val role: String = "Member",
    val active: Boolean = true,
)

@Serializable
enum class NotificationType {
    ApplicationReview,
    RegistrationReview,
    RegistrationDecision,
    Post,
    Activity,
    Organization,
    Application,
}

// A notification can link directly to a post or an application.
@Serializable
data class StageCallNotification(
    val id: Int,
    val organizationId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val postId: Int? = null,
    val applicationId: Int? = null,
    val createdAt: Instant = Clock.System.now(),
    val isRead: Boolean = false,
    val registrationId: Int? = null,
)

@Serializable
data class Organization(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val shortTagline: String,
    val aboutHeadline: String,
    val fullDescription: String,
    val memberCount: Int,
    val isRecruiting: Boolean,
    val requirements: List<String>,
    val nextActivityTitle: String,
    val nextActivitySchedule: String,
    val bannerColor: String = "blue",
    val photoPath: String? = null,
)
