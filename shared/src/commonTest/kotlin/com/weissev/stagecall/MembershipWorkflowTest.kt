package com.weissev.stagecall

import com.weissev.stagecall.data.*
import kotlin.test.*

class MembershipWorkflowTest {
    @Test
    fun submissionValidatesAndPreventsDuplicates() {
        val repo = populatedRepository()
        assertFalse(repo.submit("1", "2nd Year", "", true))
        assertFalse(repo.submit("1", "2nd Year", "Learn Kotlin", false))
        assertFalse(repo.submit("5", "2nd Year", "Learn games", true))
        assertTrue(repo.submit("1", "2nd Year", "Learn Kotlin", true))
        assertFalse(repo.submit("1", "2nd Year", "Again", true))
        assertEquals(ApplicationStatus.Pending, repo.applications.last().status)
    }

    @Test
    fun approvalAddsMemberExactlyOnce() {
        val repo = populatedRepository()
        repo.decide(1, true, "Welcome!")
        assertTrue(repo.isMember("2"))
        val count = repo.members.size
        repo.decide(1, true, "Again")
        assertEquals(count, repo.members.size)
        assertEquals(ApplicationStatus.Accepted, repo.applications.first().status)
    }

    @Test
    fun rejectionDoesNotAddMember() {
        val repo = populatedRepository()
        repo.decide(1, false, "Please complete orientation.")
        assertFalse(repo.isMember("2"))
        assertEquals(ApplicationStatus.Rejected, repo.applications.first().status)
    }

    @Test
    fun publishingRequiresEventSchedule() {
        val repo = populatedRepository()
        assertFalse(repo.publish("1", "Workshop", "Join us", "Events", false, ""))
        assertTrue(repo.publish("1", "Workshop", "Join us", "Events", false, "Nov 20, IT Lab"))
        assertEquals("Workshop", repo.posts.first().title)
    }
}
