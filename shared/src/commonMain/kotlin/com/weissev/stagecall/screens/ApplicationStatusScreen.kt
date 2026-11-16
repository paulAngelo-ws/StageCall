package com.weissev.stagecall.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.weissev.stagecall.components.StageCallCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun ApplicationPage(repository: StageCallRepository, id: Int) {
    val app = repository.applications.first { it.id == id }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Membership Application",
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
                Text(repository.organization(app.organizationId).name)
                Text("Application status: ${app.status}")
                Text(
                    if (app.status == ApplicationStatus.Pending)
                        "The team is reviewing your application."
                    else app.decision.ifBlank { "Decision recorded." }
                )
            }
        }
        StageCallCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Application summary", style = MaterialTheme.typography.titleLarge)
                Text(app.student.name)
                Text(app.student.email)
                Text("Student ID: ${app.student.studentId}")
                Text("Year level: ${app.year}")
                HorizontalDivider()
                Text("Reason for joining")
                Text(app.reason)
            }
        }
        Text("Check Applications for updates from the organization.")
    }
}
