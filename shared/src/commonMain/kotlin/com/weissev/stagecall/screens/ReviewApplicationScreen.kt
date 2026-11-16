package com.weissev.stagecall.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun ReviewPage(repository: StageCallRepository, id: Int, onDone: () -> Unit) {
    val app = repository.applications.first { it.id == id }
    var message by remember(id) { mutableStateOf("") }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Review application",
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
                Text(app.status.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(app.student.name, style = MaterialTheme.typography.titleLarge)
                Text(app.student.email)
                Text("${app.student.studentId} - ${app.year}")
            }
        }
        Text("Why would you like to join?", style = MaterialTheme.typography.titleMedium)
        Text(app.reason, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("Membership requirements", style = MaterialTheme.typography.titleMedium)
        repository.organization(app.organizationId).requirements.forEach {
            Text("\u2022 $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message to applicant (optional)") },
            singleLine = false,
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    repository.decide(id, true, message)
                    onDone()
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                enabled = app.status == ApplicationStatus.Pending,
            ) {
                Text(text = "Accept")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                onClick = {
                    repository.decide(id, false, message)
                    onDone()
                },
                enabled = app.status == ApplicationStatus.Pending,
            ) {
                Text("Reject")
            }
        }
        Text("Accepting adds the student to the member directory.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
