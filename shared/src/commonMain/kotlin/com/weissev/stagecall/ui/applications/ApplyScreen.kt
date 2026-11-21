package com.weissev.stagecall.ui.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.remote.APPLICATION_REASON_LIMIT
import com.weissev.stagecall.data.remote.ApplicationStore
import com.weissev.stagecall.data.remote.STUDENT_ID_LIMIT
import com.weissev.stagecall.ui.components.YearLevelDropdown
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.organizationCardColors
import kotlinx.coroutines.launch

@Composable
internal fun ApplyPage(
    repository: StageCallRepository,
    id: String,
    store: ApplicationStore,
    defaultStudentNumber: String,
    defaultYearLevel: String,
    onSubmitted: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var editedStudentNumber by remember(id) { mutableStateOf<String?>(null) }
    val studentNumber = editedStudentNumber ?: defaultStudentNumber
    var selectedYear by remember(id) { mutableStateOf<String?>(null) }
    val year = selectedYear ?: defaultYearLevel
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
            // Use the organization’s accent color for the strip above its summary.
            Box(Modifier.fillMaxWidth().height(6.dp).background(accentColor)) // Change 6.dp to make the strip thicker or thinner.
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
                    onValueChange = { editedStudentNumber = it.take(STUDENT_ID_LIMIT); error = "" },
                    modifier = Modifier.fillMaxWidth(), enabled = !store.submitting,
                    label = { RequiredLabel("Student ID") }, supportingText = { Text("${studentNumber.length}/$STUDENT_ID_LIMIT") },
                    singleLine = true,
                )
                YearLevelDropdown(
                    year = year,
                    enabled = !store.submitting,
                    onSelected = { selectedYear = it },
                    label = { RequiredLabel("Year level: $year") },
                )
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
                        // Send the form to ApplicationStore.kt. App.kt uses the returned ID to open ApplicationStatusScreen.kt.
                        val applicationId = store.submit(id, studentNumber, year, reason) // ApplicationStore.kt receives the organization ID, student number, year, and reason.
                        if (applicationId != null) onSubmitted(applicationId) else error = store.error.orEmpty() // App.kt receives the new ID and opens ApplicationStatusScreen.kt.
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            enabled = !store.submitting && repository.canApply(id), // Block a second tap while the request is being sent.
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
