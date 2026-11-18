package com.weissev.stagecall.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.organizationCardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun OrganizationPage(
    repository: StageCallRepository,
    id: String,
    onApply: () -> Unit,
    initialPostId: Int? = null,
) {
    val org = repository.organizations.firstOrNull { it.id == id }
    if (org == null) {
        Text("This organization is no longer available.", Modifier.padding(24.dp))
        return
    }
    val (_, orgColor) = organizationCardColors[
        repository.organizations.indexOf(org) % organizationCardColors.size
    ]
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
        StageCallCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = orgColor,
                contentColor = Color.White,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = org.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${org.memberCount + repository.members.count { it.organizationId == id }} members",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        // Each chip updates the selected value when clicked.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
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
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    enabled = repository.canApply(id),
                ) {
                    Text(
                        text = when {
                            repository.profile.studentId.isBlank() -> "Sign in to apply"
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
