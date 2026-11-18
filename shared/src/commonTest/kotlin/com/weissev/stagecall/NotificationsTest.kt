package com.weissev.stagecall

import com.weissev.stagecall.data.NotificationType
import com.weissev.stagecall.data.StageCallRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Exercise the local notification model in StageCallRepository.kt using the organizations from DemoFixtures.kt.
class NotificationsTest {
    @Test
    fun onlyJoinedOrganizationsSendPostAndActivityNotifications() {
        val repo = populatedRepository()
        assertTrue(repo.publish("1", "Other club", "Update", "Announcements", false, ""))
        assertTrue(repo.notifications.isEmpty())
        assertFalse(repo.publish("4", "Incomplete event", "Update", "Events", false, ""))
        assertTrue(repo.notifications.isEmpty())
        assertTrue(repo.publish("4", "Coding sprint", "Join us", "Events", true, "Nov 21, IT Lab"))
        val notification = repo.notifications.single()
        assertEquals(NotificationType.Activity, notification.type)
        assertEquals(repo.posts.first().id, notification.postId)
        assertEquals(1, repo.unreadNotificationCount)
        repo.markNotificationRead(notification.id)
        assertEquals(0, repo.unreadNotificationCount)
        assertTrue(repo.notifications.single().isRead)
    }

    @Test
    fun organizationChangesNotifyMembersAndIgnoreUnchangedSaves() {
        val repo = populatedRepository()
        val organization = repo.organization("4")
        assertTrue(repo.updateOrganization(organization.copy(isRecruiting = false)))
        assertEquals(NotificationType.Organization, repo.notifications.single().type)
        assertTrue(repo.notifications.single().message.contains("closed"))
        assertTrue(repo.updateOrganization(repo.organization("4")))
        assertEquals(1, repo.notifications.size)
        assertTrue(repo.updateOrganization(repo.organization("4").copy(nextActivitySchedule = "Nov 22, IT Lab")))
        assertEquals(NotificationType.Activity, repo.notifications.first().type)
        repo.markAllNotificationsRead()
        assertEquals(0, repo.unreadNotificationCount)
        repo.members.removeAll { it.organizationId == "4" }
        assertTrue(repo.notifications.isEmpty())
    }

    @Test
    fun unrelatedOrganizationChangesAreSilent() {
        val repo = populatedRepository()
        assertTrue(repo.updateOrganization(repo.organization("1").copy(isRecruiting = false)))
        assertTrue(repo.notifications.isEmpty())
    }

    @Test
    fun applicationUpdatesLinkToTheCurrentStudentsApplication() {
        val repo = populatedRepository()
        repo.decide(2, true, "Welcome, Alex Rivera")
        assertTrue(repo.notifications.isEmpty())
        repo.decide(1, true, "Welcome!")
        val notification = repo.notifications.single()
        assertEquals(NotificationType.Application, notification.type)
        assertEquals(1, notification.applicationId)
        assertTrue(repo.isMember("2"))
        assertTrue(repo.publish("2", "Members update", "Hello", "Announcements", false, ""))
        assertEquals(NotificationType.Post, repo.notifications.first().type)
        assertEquals(2, repo.unreadNotificationCount)
        repo.markAllNotificationsRead()
        assertEquals(0, repo.unreadNotificationCount)
    }
}
