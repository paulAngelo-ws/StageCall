package com.weissev.stagecall.screens

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
import com.weissev.stagecall.components.organizationCardColors
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.OrganizationRegistration
import com.weissev.stagecall.data.OrganizationRegistrationStore
import com.weissev.stagecall.data.REGISTRATION_CATEGORY_LIMIT
import com.weissev.stagecall.data.REGISTRATION_DECISION_LIMIT
import com.weissev.stagecall.data.REGISTRATION_DESCRIPTION_LIMIT
import com.weissev.stagecall.data.REGISTRATION_NAME_LIMIT
import com.weissev.stagecall.data.REGISTRATION_TAGLINE_LIMIT
import com.weissev.stagecall.data.RegistrationDraft
import kotlinx.coroutines.launch

@Composable
internal fun OrganizationRegistrationPage(
    store: OrganizationRegistrationStore,
    onSubmitted: () -> Unit,
    onOpenAdminReview: () -> Unit,
    onOpenRegistration: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var draft by remember(store) { mutableStateOf(RegistrationDraft()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
        (store.error ?: store.ownError ?: store.accessError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("An admin must approve your application before it appears in the directory.")
        RegistrationField("Organization name", draft.name, REGISTRATION_NAME_LIMIT, !store.working) { draft = draft.copy(name = it) }
        RegistrationField("Category", draft.category, REGISTRATION_CATEGORY_LIMIT, !store.working) { draft = draft.copy(category = it) }
        RegistrationField("Short tagline", draft.tagline, REGISTRATION_TAGLINE_LIMIT, !store.working) { draft = draft.copy(tagline = it) }
        RegistrationField("About the organization", draft.description, REGISTRATION_DESCRIPTION_LIMIT, !store.working, singleLine = false) {
            draft = draft.copy(description = it)
        }
        Button(enabled = draft.complete && !store.working, onClick = {
            scope.launch {
                if (store.submit(draft)) {
                    draft = RegistrationDraft()
                    onSubmitted()
                }
            }
        }) { Text("Submit for review") }

        Text("My registrations", style = MaterialTheme.typography.titleLarge)
        if (store.ownLoaded && store.own.isEmpty() && !store.working) Text("You haven’t submitted any registrations.")
        store.own.forEach { registration ->
            val (_, barColor) = organizationCardColors[registration.id % organizationCardColors.size]
            ApplicationCard(
                title = registration.name,
                subtitle = "Organization registration",
                barColor = barColor,
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
    var review by remember(store) { mutableStateOf<OrganizationRegistration?>(null) }
    var details by remember(store) { mutableStateOf<OrganizationRegistration?>(null) }
    var approve by remember(store) { mutableStateOf(false) }
    var note by remember(store) { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Review organization registrations", style = MaterialTheme.typography.headlineMedium)
        if (store.working || (!store.queueLoaded && store.queueError == null)) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (review == null) (store.error ?: store.queueError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (store.queueLoaded && store.pending.isEmpty() && store.queueError == null) Text("No registrations awaiting review.")
        store.pending.forEach { registration ->
            ApplicationReviewCard(
                title = registration.name,
                metadata = listOf(registration.category, registration.applicantEmail.ifBlank { registration.applicantId }),
                submittedAt = registration.submittedAt,
                preview = registration.description,
                status = registration.status,
                accentColor = organizationCardColors[registration.id % organizationCardColors.size].second,
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
                    store.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = { TextButton(onClick = { review = null }, enabled = !store.working) { Text("Cancel") } },
            confirmButton = {
                TextButton(enabled = !store.working, onClick = {
                    scope.launch {
                        if (store.decide(registration.id, approve, note)) {
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
