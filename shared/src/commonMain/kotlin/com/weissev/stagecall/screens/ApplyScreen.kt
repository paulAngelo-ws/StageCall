package com.weissev.stagecall.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.organizationCardColors
import com.weissev.stagecall.data.APPLICATION_REASON_LIMIT
import com.weissev.stagecall.data.ApplicationStore
import com.weissev.stagecall.data.STUDENT_ID_LIMIT
import com.weissev.stagecall.data.StageCallRepository
import kotlinx.coroutines.launch

@Composable
internal fun ApplyPage(repository: StageCallRepository, id: String, store: ApplicationStore, onSubmitted: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var studentNumber by remember(id) { mutableStateOf("") }
    var year by remember(id) { mutableStateOf("2nd Year") }
    var yearMenu by remember(id) { mutableStateOf(false) }
    var reason by remember(id) { mutableStateOf("") }
    var consent by remember(id) { mutableStateOf(false) }
    var error by remember(id) { mutableStateOf("") }
    val organization = repository.organizations.firstOrNull { it.id == id }
    if (organization == null) {
        Text("This organization is no longer available.", Modifier.padding(24.dp))
        return
    }
    val colorIndex = listOf("blue", "green", "purple", "orange", "indigo", "gold").indexOf(organization.bannerColor).coerceAtLeast(0)
    val accentColor = organizationCardColors[colorIndex].second
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Apply for membership", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Tell the organization a little about yourself.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(6.dp).background(accentColor))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("APPLYING TO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(organization.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("General membership", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it.take(STUDENT_ID_LIMIT); error = "" },
                    modifier = Modifier.fillMaxWidth(), enabled = !store.submitting,
                    label = { RequiredLabel("Student ID") }, supportingText = { Text("${studentNumber.length}/$STUDENT_ID_LIMIT") },
                    singleLine = true,
                )
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { yearMenu = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !store.submitting) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            RequiredLabel("Year level: $year")
                        }
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                        listOf("1st Year", "2nd Year", "3rd Year", "4th Year").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { year = option; yearMenu = false })
                        }
                    }
                }
            }
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your motivation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Share why you would like to join.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it.take(APPLICATION_REASON_LIMIT); error = "" },
                    modifier = Modifier.fillMaxWidth(), enabled = !store.submitting,
                    label = { RequiredLabel("Reason for joining") }, supportingText = { Text("${reason.length}/$APPLICATION_REASON_LIMIT") },
                    minLines = 4,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(value = consent, enabled = !store.submitting, role = Role.Checkbox,
                onValueChange = { consent = it; error = "" }).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = consent, onCheckedChange = null, enabled = !store.submitting, modifier = Modifier.size(24.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("I agree to share these details with the organization for membership review.",
                    modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium)
                Text(" *", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        if (!repository.canApply(id)) Text("Applications are currently unavailable for this organization.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = {
                if (studentNumber.isBlank() || reason.isBlank() || !consent) {
                    error = "Enter your student ID and reason, and agree to share your details."
                } else {
                    scope.launch {
                        val applicationId = store.submit(id, studentNumber, year, reason)
                        if (applicationId != null) onSubmitted(applicationId) else error = store.error.orEmpty()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            enabled = !store.submitting && repository.canApply(id),
        ) {
            if (store.submitting) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (store.submitting) "Submitting…" else "Submit application")
        }
        Text("You can submit one application per organization. Please review your details before submitting.",
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RequiredLabel(label: String) {
    Row {
        Text(label)
        Text(" *", color = MaterialTheme.colorScheme.error)
    }
}
