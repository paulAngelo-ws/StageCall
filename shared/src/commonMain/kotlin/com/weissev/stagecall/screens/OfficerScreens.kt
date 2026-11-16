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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import com.weissev.stagecall.components.StageCallCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun OfficerPage(
    repository: StageCallRepository,
    orgId: String,
    onSelectOrganization: (String) -> Unit,
    onReviewApplication: (Int) -> Unit,
    onOpenMembers: () -> Unit,
    onPublish: () -> Unit,
) {
    var editingOrganization by remember(orgId) { mutableStateOf(false) }
    if (editingOrganization) {
        EditOrganizationDialog(repository, orgId, onDismiss = { editingOrganization = false })
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
            text = "Officer Portal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        // Each chip updates the selected value when clicked.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (organization in repository.organizations) {
                FilterChip(
                    selected = orgId == organization.id,
                    onClick = { onSelectOrganization(organization.id) },
                    label = { Text(organization.name) },
                )
            }
        }
        val pending =
            repository.applications.filter {
                it.organizationId == orgId && it.status == ApplicationStatus.Pending
            }
        StageCallCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${pending.size} pending applications")
                Text("${repository.members.count { it.organizationId == orgId }} members")
                Text("${repository.posts.count { it.organizationId == orgId }} posts")
            }
        }
        OutlinedButton(onClick = { editingOrganization = true }) { Text("Edit organization") }
        OutlinedButton(onClick = onOpenMembers) { Text("Member directory") }
        Button(
            onClick = onPublish,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
        ) {
            Text(text = "Create announcement / activity")
        }
        Text("Applicant review", style = MaterialTheme.typography.titleLarge)
        if (pending.isEmpty()) Text("All caught up. No pending applications.")
        for (app in pending) {
            StageCallCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(app.student.name, style = MaterialTheme.typography.titleMedium)
                    Text(app.year)
                    OutlinedButton(onClick = { onReviewApplication(app.id) }) {
                        Text("Review application")
                    }
                }
            }
        }
        Text("Recent decisions")
        repository.applications
            .filter { it.organizationId == orgId && it.status != ApplicationStatus.Pending }
            .forEach { Text("${it.student.name} - ${it.status}") }
    }
}

@Composable
private fun EditOrganizationDialog(repository: StageCallRepository, orgId: String, onDismiss: () -> Unit) {
    val organization = repository.organization(orgId)
    var name by remember { mutableStateOf(organization.name) }
    var description by remember { mutableStateOf(organization.fullDescription) }
    var activity by remember { mutableStateOf(organization.nextActivityTitle) }
    var schedule by remember { mutableStateOf(organization.nextActivitySchedule) }
    var recruiting by remember { mutableStateOf(organization.isRecruiting) }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit organization") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Organization name") }, singleLine = true)
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("About the organization") }, minLines = 3)
                OutlinedTextField(activity, { activity = it }, Modifier.fillMaxWidth(), label = { Text("Next activity") })
                OutlinedTextField(schedule, { schedule = it }, Modifier.fillMaxWidth(), label = { Text("Activity schedule & location") })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recruitment open", Modifier.weight(1f))
                    Switch(checked = recruiting, onCheckedChange = { recruiting = it })
                }
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || description.isBlank()) {
                    error = "Enter an organization name and description."
                } else if (activity.isBlank() != schedule.isBlank()) {
                    error = "Enter both an activity and its schedule, or leave both empty."
                } else if (repository.updateOrganization(
                    organization.copy(
                        name = name.trim(),
                        fullDescription = description.trim(),
                        nextActivityTitle = activity.trim(),
                        nextActivitySchedule = schedule.trim(),
                        isRecruiting = recruiting,
                    )
                )) onDismiss()
            }) { Text("Save changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
