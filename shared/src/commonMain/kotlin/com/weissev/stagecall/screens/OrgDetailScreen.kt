package com.weissev.stagecall.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.weissev.stagecall.components.StageCallCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun OrganizationPage(
    repository: StageCallRepository,
    id: String,
    onApply: () -> Unit,
    onShowMessage: (String) -> Unit,
    initialPostId: Int? = null,
) {
    val org = repository.organization(id)
    var tab by remember(id, initialPostId) {
        mutableStateOf(if (initialPostId != null) "Updates" else "About")
    }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Organization details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        StageCallCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(org.category.uppercase() + " - UNIVERSITY RECOGNIZED")
                Text(org.name, style = MaterialTheme.typography.headlineSmall)
                Text(org.shortTagline)
                Text(
                    "${org.memberCount + repository.members.count { it.organizationId == id }} members - ${if (org.isRecruiting) "Applications open" else "Recruitment closed"}"
                )
                TextButton(onClick = { repository.toggleBookmark(id) }) {
                    Text(
                        if (id in repository.bookmarks) "Saved to bookmarks"
                        else "Bookmark organization"
                    )
                }
            }
        }
        // Each chip updates the selected value when clicked.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (option in listOf("About", "Activities", "Updates")) {
                FilterChip(
                    selected = tab == option,
                    onClick = { tab = option },
                    label = { Text(option) },
                )
            }
        }
        when (tab) {
            "About" -> {
                Text(org.aboutHeadline, style = MaterialTheme.typography.titleLarge)
                Text(org.fullDescription)
                Text("Membership requirements", style = MaterialTheme.typography.titleMedium)
                org.requirements.forEach { Text("- $it") }
                Text("Application deadline: ${org.deadline}")
                StageCallCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("NEXT ACTIVITY")
                        Text(org.nextActivityTitle)
                        Text(org.nextActivitySchedule)
                    }
                }
                OutlinedButton(
                    onClick = {
                        onShowMessage(
                            "Contact ${org.name}\nVisit the Student Affairs office to reach an organization officer."
                        )
                    }
                ) {
                    Text("Contact an officer")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    enabled = repository.canApply(id),
                ) {
                    Text(
                        text = when {
                            repository.isMember(id) -> "Already a member"
                            !org.isRecruiting -> "Recruitment closed"
                            !repository.canApply(id) -> "Application already submitted"
                            else -> "Apply for membership"
                        }
                    )
                }
            }
            "Activities" -> {
                StageCallCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(org.nextActivityTitle)
                        Text(org.nextActivitySchedule)
                    }
                }
            }
            "Updates" -> {
                val posts =
                    repository.posts.filter {
                        it.organizationId == id && (!it.membersOnly || repository.isMember(id))
                    }.sortedByDescending { it.id == initialPostId }
                if (posts.isEmpty()) Text("No updates yet.")
                posts.forEach {
                    StageCallCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(it.title, style = MaterialTheme.typography.titleMedium)
                            Text(it.message)
                            if (it.schedule.isNotEmpty()) Text(it.schedule)
                        }
                    }
                }
            }
        }
    }
}
