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
data class Student(val name: String, val email: String, val studentId: String = "")

@Serializable
data class MembershipApplication(
    val id: Int,
    val organizationId: String,
    val student: Student,
    val year: String,
    val reason: String,
    val status: ApplicationStatus = ApplicationStatus.Pending,
    val decision: String = "",
    val submittedAt: Instant = Clock.System.now(),
    val updatedAt: Instant? = null,
    val studentNumber: String = "",
)

@Serializable
data class CampusPost(
    val id: Int,
    val organizationId: String,
    val title: String,
    val message: String,
    val category: String,
    val membersOnly: Boolean = false,
    val schedule: String = "",
    val createdAt: Instant = Clock.System.now(),
)

@Serializable
data class Member(
    val organizationId: String,
    val student: Student,
    val role: String = "Member",
    val active: Boolean = true,
)

@Serializable
enum class NotificationType {
    Post,
    Activity,
    Organization,
    Application,
}

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
)
