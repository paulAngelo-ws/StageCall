package com.weissev.stagecall.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.MembershipApplication
import com.weissev.stagecall.data.OrganizationRoleStore
import kotlinx.coroutines.launch

@Composable
internal fun ReviewApplicationsPage(store: OrganizationRoleStore, organizationId: String, organizationName: String) {
    val scope = rememberCoroutineScope()
    var selected by remember(organizationId) { mutableStateOf<MembershipApplication?>(null) }
    var accept by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(store, organizationId) { store.loadApplications(organizationId) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Review applications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(organizationName, style = MaterialTheme.typography.bodyLarge)
        store.reviewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (store.reviewing && store.applications.isEmpty()) Text("Loading applications...")
        if (!store.reviewing && store.reviewError == null && store.applications.isEmpty()) Text("No applications yet.")
        OutlinedButton(onClick = { scope.launch { store.loadApplications(organizationId) } }, enabled = !store.reviewing) {
            Text("Refresh")
        }
        store.applications.forEach { application ->
            StageCallCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(application.student.name, style = MaterialTheme.typography.titleMedium)
                    Text(application.student.email)
                    Text("Student ID: ${application.studentNumber}")
                    Text("Year level: ${application.year}")
                    Text("Reason: ${application.reason}")
                    Text("Status: ${application.status}", fontWeight = FontWeight.SemiBold)
                    if (application.decision.isNotBlank()) Text("Decision note: ${application.decision}")
                    if (application.status == ApplicationStatus.Pending) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { selected = application; accept = true; message = "" }, enabled = !store.reviewing) { Text("Accept") }
                            OutlinedButton(onClick = { selected = application; accept = false; message = "" }, enabled = !store.reviewing) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
    selected?.let { application ->
        AlertDialog(
            onDismissRequest = { if (!store.reviewing) selected = null },
            title = { Text(if (accept) "Accept application?" else "Reject application?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${application.student.name} · $organizationName")
                    OutlinedTextField(message, { if (it.length <= 2000) message = it }, label = { Text("Note to applicant (optional)") })
                    store.reviewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !store.reviewing, onClick = {
                    scope.launch {
                        if (store.decide(organizationId, application.id, accept, message)) {
                            selected = null
                            store.loadApplications(organizationId)
                        }
                    }
                }) { Text(if (accept) "Accept" else "Reject") }
            },
            dismissButton = { TextButton(enabled = !store.reviewing, onClick = { selected = null }) { Text("Cancel") } },
        )
    }
}
