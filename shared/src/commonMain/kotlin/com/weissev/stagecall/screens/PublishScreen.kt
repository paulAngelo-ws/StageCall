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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun PublishPage(repository: StageCallRepository, orgId: String, onDone: () -> Unit) {
    var title by remember { mutableStateOf(repository.draftTitle) }
    var message by remember { mutableStateOf(repository.draftMessage) }
    var category by remember { mutableStateOf("Announcements") }
    var audience by remember { mutableStateOf("Public") }
    var schedule by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Create announcement",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(repository.organization(orgId).name)
        // Each chip updates the selected value when clicked.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (option in listOf("Announcements", "Events", "Recruitment")) {
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option) },
                )
            }
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title *") },
            singleLine = true,
        )
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message *") },
            singleLine = false,
            minLines = 3,
        )
        if (category == "Events") {
            OutlinedTextField(
                value = schedule,
                onValueChange = { schedule = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Date, time, and location *") },
                singleLine = true,
            )
        }
        // Each chip updates the selected value when clicked.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (option in listOf("Public", "Members only")) {
                FilterChip(
                    selected = audience == option,
                    onClick = { audience = option },
                    label = { Text(option) },
                )
            }
        }
        if (feedback.isNotEmpty()) Text(feedback)
        OutlinedButton(
            onClick = {
                repository.draftTitle = title
                repository.draftMessage = message
                feedback = "Draft saved for this session."
            }
        ) {
            Text("Save as draft")
        }
        Button(
            onClick = {
                val published = repository.publish(
                    orgId = orgId,
                    title = title,
                    message = message,
                    category = category,
                    privatePost = audience == "Members only",
                    schedule = schedule,
                )
                if (published) {
                    onDone()
                } else {
                    feedback = "Complete the title, message, and event schedule if needed."
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
        ) {
            Text(text = "Publish ${if (category == "Events") "activity" else "announcement"}")
        }
    }
}
