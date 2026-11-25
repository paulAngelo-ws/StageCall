package com.weissev.stagecall.ui.registrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.remote.OrganizationRegistration
import com.weissev.stagecall.data.remote.OrganizationRegistrationStore
import com.weissev.stagecall.data.remote.REGISTRATION_CATEGORY_LIMIT
import com.weissev.stagecall.data.remote.REGISTRATION_DECISION_LIMIT
import com.weissev.stagecall.data.remote.REGISTRATION_DESCRIPTION_LIMIT
import com.weissev.stagecall.data.remote.REGISTRATION_NAME_LIMIT
import com.weissev.stagecall.data.remote.REGISTRATION_TAGLINE_LIMIT
import com.weissev.stagecall.data.remote.RegistrationDraft
import com.weissev.stagecall.ui.applications.ApplicationCard
import com.weissev.stagecall.ui.applications.ApplicationReviewCard
import com.weissev.stagecall.ui.components.organizationCardColors
import kotlinx.coroutines.launch

@Composable
internal fun OrganizationRegistrationPage(
    store: OrganizationRegistrationStore,
    onSubmitted: () -> Unit,
    onOpenAdminReview: () -> Unit,
    onOpenRegistration: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // OrganizationRegistrationStore.kt owns saved registrations and the review queue. This screen keeps only its form and dialog state.
    // Keep form input while this screen is open. Start fresh when the account store changes.
    var draft by remember(store) { mutableStateOf(RegistrationDraft()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), // Allow scrolling and keep content 24.dp from the page edges.
        verticalArrangement = Arrangement.spacedBy(16.dp), // Gap between items in this column.
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Register an organization",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            // Only admins see the shortcut to the review queue.
            if (store.isAdmin) {
                FilledTonalButton(
                    onClick = onOpenAdminReview,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Admin review", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (store.working || (!store.ownLoaded && store.ownError == null)) LinearProgressIndicator(Modifier.fillMaxWidth())
        (store.error ?: store.ownError ?: store.accessError)?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }
        Text("An admin must approve your application before it appears in the directory.")
        RegistrationField("Organization name", draft.name, REGISTRATION_NAME_LIMIT, !store.working) { draft = draft.copy(name = it) }
        RegistrationField("Category", draft.category, REGISTRATION_CATEGORY_LIMIT, !store.working) { draft = draft.copy(category = it) }
        RegistrationField("Short tagline", draft.tagline, REGISTRATION_TAGLINE_LIMIT, !store.working) { draft = draft.copy(tagline = it) }
        RegistrationField("About the organization", draft.description, REGISTRATION_DESCRIPTION_LIMIT, !store.working, singleLine = false) {
            draft = draft.copy(description = it)
        }
        // Enable submission once the required fields are filled and no request is running.
        Button(enabled = draft.complete && !store.working, onClick = {
            scope.launch {
                // Send the form to OrganizationRegistrationStore.kt. Clear it after the database accepts the registration.
                if (store.submit(draft)) { // OrganizationRegistrationStore.kt receives the values typed into this form.
                    draft = RegistrationDraft()
                    onSubmitted()
                }
            }
        }) { Text("Submit for review") }

        Text("My registrations", style = MaterialTheme.typography.titleLarge)
        if (store.ownLoaded && store.own.isEmpty() && !store.working) Text("You haven’t submitted any registrations.")
        // ApplicationsScreen.kt provides the card. Its click sends the registration ID to App.kt for the details page.
        // Reuse the application card layout for the student’s past registrations.
        store.own.forEach { registration ->
            val (_, barColor) = organizationCardColors[registration.id % organizationCardColors.size] // Pick a repeatable accent from the registration ID.
            ApplicationCard(
                title = registration.name,
                subtitle = "Organization registration",
                barColor = barColor, // Color of the left strip on the registration card.
                status = registration.status,
                statusLabel = when (registration.status) {
                    ApplicationStatus.Pending -> "In review"
                    ApplicationStatus.Accepted -> "Approved"
                    ApplicationStatus.Rejected -> "Rejected"
                },
                submittedAt = registration.submittedAt,
                updatedAt = registration.reviewedAt,
                message = registration.decision.ifBlank {
                    when (registration.status) {
                        ApplicationStatus.Pending -> "Your registration has been submitted."
                        ApplicationStatus.Accepted -> "Your organization is listed in the directory."
                        ApplicationStatus.Rejected -> "Your registration has been rejected."
                    }
                },
                actionLabel = "View registration",
            ) { onOpenRegistration(registration.id) }
        }
    }
}

@Composable
internal fun OrganizationRegistrationReviewPage(
    store: OrganizationRegistrationStore,
    onDecisionSaved: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Track which registration is being reviewed and which one is open for reading.
    var review by remember(store) { mutableStateOf<OrganizationRegistration?>(null) }
    var details by remember(store) { mutableStateOf<OrganizationRegistration?>(null) }
    var approve by remember(store) { mutableStateOf(false) }
    var note by remember(store) { mutableStateOf("") }
    var history by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), // Allow scrolling and keep content 24.dp from the page edges.
        verticalArrangement = Arrangement.spacedBy(16.dp), // Gap between items in this column.
    ) {
        Text("Organization applications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Review new communities and keep track of your decisions.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !history, onClick = { history = false }, label = { Text("Pending (${store.pending.size})") })
            FilterChip(selected = history, onClick = { history = true }, label = { Text("History (${store.reviewed.size})") })
        }
        if (store.working || (!store.queueLoaded && store.queueError == null)) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (review == null) (store.error ?: store.queueError)?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }
        if (!history && store.queueLoaded && store.pending.isEmpty() && store.queueError == null) Text("You're all caught up. No applications awaiting review.")
        if (history && store.queueLoaded && store.reviewed.isEmpty() && store.queueError == null) Text("Reviewed applications will appear here.")
        // Show one review card for each registration waiting on an admin decision.
        (if (history) emptyList() else store.pending).forEach { registration ->
            ApplicationReviewCard(
                title = registration.name,
                metadata = listOf(registration.category, registration.applicantEmail.ifBlank { registration.applicantId }),
                submittedAt = registration.submittedAt,
                preview = registration.description,
                status = registration.status,
                accentColor = organizationCardColors[registration.id % organizationCardColors.size].second, // Color of the top strip on the admin review card.
                decision = registration.decision,
                acceptLabel = "Approve",
                working = store.working,
                onViewDetails = { details = registration },
                onDecide = { approved ->
                    approve = approved
                    note = ""
                    review = registration
                },
            )
        }
        if (history && store.reviewed.isNotEmpty()) {
            store.reviewed.forEach { registration ->
                ApplicationCard(
                    title = registration.name,
                    subtitle = registration.applicantEmail,
                    barColor = organizationCardColors[registration.id % organizationCardColors.size].second,
                    status = registration.status,
                    submittedAt = registration.submittedAt,
                    updatedAt = registration.reviewedAt,
                    message = registration.decision.ifBlank { "Registration ${registration.status.name.lowercase()}." },
                    actionLabel = "View registration",
                ) { details = registration }
            }
        }
    }

    details?.let { registration ->
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text("Registration details") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(registration.name, style = MaterialTheme.typography.titleMedium)
                    Text(registration.category)
                    Text(registration.applicantEmail.ifBlank { registration.applicantId })
                    Text(registration.tagline)
                    Text(registration.description)
                }
            },
            confirmButton = { TextButton(onClick = { details = null }) { Text("Close") } },
        )
    }

    review?.let { registration ->
        AlertDialog(
            onDismissRequest = { if (!store.working) review = null },
            title = { Text(if (approve) "Approve registration?" else "Reject registration?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(registration.name)
                    if (approve) Text("This publishes the organization and makes the applicant its president.")
                    RegistrationField("Message to applicant (optional)", note, REGISTRATION_DECISION_LIMIT, !store.working, singleLine = false) {
                        note = it
                    }
                    store.error?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }
                }
            },
            dismissButton = { TextButton(onClick = { review = null }, enabled = !store.working) { Text("Cancel") } },
            confirmButton = {
                TextButton(enabled = !store.working, onClick = {
                    scope.launch {
                        // Send the registration ID, decision, and note to OrganizationRegistrationStore.kt.
                        if (store.decide(registration.id, approve, note)) { // Send the admin’s choice and note to OrganizationRegistrationStore.kt.
                            review = null
                            onDecisionSaved(approve)
                        }
                    }
                }) { Text(if (approve) "Approve" else "Reject") }
            },
        )
    }
}

@Composable
private fun RegistrationField(
    label: String,
    value: String,
    limit: Int,
    enabled: Boolean,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= limit) onValueChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text("${value.length}/$limit") },
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 6,
    )
}
