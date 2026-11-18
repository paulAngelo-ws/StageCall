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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.weissev.stagecall.data.ApplicationStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
internal fun ApplyPage(repository: StageCallRepository, id: String, store: ApplicationStore, onSubmitted: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var studentNumber by remember(id) { mutableStateOf("") }
    var year by remember(id) { mutableStateOf("2nd Year") }
    var reason by remember(id) { mutableStateOf("") }
    var consent by remember(id) { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
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
                Text(repository.organization(id).name)
                Text("General membership")
            }
        }
        Text("Join the community.", style = MaterialTheme.typography.headlineSmall)
        Text("Tell the team a little about yourself.")
        OutlinedTextField(
            studentNumber,
            { studentNumber = it; error = "" },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Student ID") },
            singleLine = true,
        )
        Text("Year level")
        // Each chip updates the selected value when clicked.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (option in listOf("1st Year", "2nd Year", "3rd Year", "4th Year")) {
                FilterChip(
                    selected = year == option,
                    onClick = { year = option },
                    label = { Text(option) },
                )
            }
        }
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Reason for joining") },
            singleLine = false,
            minLines = 3,
        )
        Row {
            Checkbox(consent, { consent = it })
            Text(
                "I agree to submit these details for membership review."
            )
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                if (studentNumber.isBlank() || reason.isBlank() || !consent) {
                    error = "Enter your student ID and reason, and agree to share your details."
                } else if (studentNumber.trim().length > 100 || reason.trim().length > 5000) {
                    error = "Student ID or reason is too long."
                } else {
                    scope.launch {
                        val applicationId = store.submit(id, studentNumber, year, reason)
                        if (applicationId != null) onSubmitted(applicationId)
                        else error = store.error.orEmpty()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            enabled = !store.submitting && repository.canApply(id),
        ) {
            Text(text = if (store.submitting) "Submitting..." else "Submit application")
        }
        Text(
            "Review your details before submitting. Each organization accepts one application per account."
        )
    }
}
