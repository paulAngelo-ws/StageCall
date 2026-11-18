package com.weissev.stagecall.ui.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.remote.OrganizationRegistration
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.StatusPanel
import com.weissev.stagecall.ui.components.organizationCardColors
import com.weissev.stagecall.ui.format.relativeTime
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
// App.kt sends the application ID. Find the latest matching record in StageCallRepository.kt.
fun ApplicationPage(repository: StageCallRepository, id: Int) {
    val app = repository.applications.firstOrNull { it.id == id }
    if (app == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            StatusPanel("This application is no longer available.")
        }
        return
    }
    val organization = repository.organizations.firstOrNull { it.id == app.organizationId }
    val colorIndex = listOf("blue", "green", "purple", "orange", "indigo", "gold")
        .indexOf(organization?.bannerColor).coerceAtLeast(0)
    val statusMessage = when (app.status) {
        ApplicationStatus.Pending -> "Your application has been submitted and is awaiting review."
        ApplicationStatus.Accepted -> "Your application has been accepted."
        ApplicationStatus.Rejected -> "Your application was not accepted."
    }
    ApplicationDetailsLayout(
        title = "Your application",
        subtitle = "Membership application details",
        organizationName = organization?.name ?: "Organization unavailable",
        barColor = organizationCardColors[colorIndex].second,
        status = app.status,
        statusLabel = app.status.name,
        statusMessage = statusMessage,
        submittedAt = app.submittedAt,
        decision = app.decision,
        decisionLabel = "Note from the organization",
    ) {
        StageCallCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ApplicationDetail("Name", app.student.name)
                ApplicationDetail("Email", app.student.email)
                ApplicationDetail("Student ID", app.studentNumber)
                ApplicationDetail("Year level", app.year)
            }
        }
        ApplicationDescription("Reason for joining", app.reason)
        Text("Check Applications for updates from the organization.", Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
// App.kt supplies the saved registration from OrganizationRegistrationStore.kt. The two details pages reuse the same layout.
internal fun OrganizationRegistrationDetailsPage(
    registration: OrganizationRegistration?,
    onOpenOrganization: suspend (String) -> Boolean,
) {
    if (registration == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            StatusPanel("This registration is no longer available.")
        }
        return
    }
    ApplicationDetailsLayout(
        title = "Your registration",
        subtitle = "Organization registration details",
        organizationName = registration.name,
        barColor = organizationCardColors[registration.id % organizationCardColors.size].second,
        status = registration.status,
        statusLabel = if (registration.status == ApplicationStatus.Accepted) "Approved" else registration.status.name,
        statusMessage = when (registration.status) {
            ApplicationStatus.Pending -> "Your registration has been submitted and is awaiting admin review."
            ApplicationStatus.Accepted -> "Your registration has been approved."
            ApplicationStatus.Rejected -> "Your registration was not approved."
        },
        submittedAt = registration.submittedAt,
        decision = registration.decision,
        decisionLabel = "Note from the admin",
    ) {
        StageCallCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Organization details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ApplicationDetail("Category", registration.category)
                ApplicationDetail("Short tagline", registration.tagline)
            }
        }
        ApplicationDescription("About the organization", registration.description)
        registration.organizationId?.let { id ->
            val scope = rememberCoroutineScope()
            var opening by remember(id) { mutableStateOf(false) }
            var openFailed by remember(id) { mutableStateOf(false) }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !opening,
                onClick = {
                    opening = true
                    openFailed = false
                    scope.launch {
                        try {
                            openFailed = !onOpenOrganization(id) // Ask App.kt to open OrganizationScreen.kt and keep track of whether it worked.
                        } finally {
                            opening = false
                        }
                    }
                },
            ) { Text(if (opening) "Opening…" else "Open organization") }
            if (openFailed) {
                Text("Could not load your organization and role. Try again.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ApplicationDetailsLayout(
    title: String,
    subtitle: String,
    organizationName: String,
    barColor: Color,
    status: ApplicationStatus,
    statusLabel: String,
    statusMessage: String,
    submittedAt: Instant,
    decision: String,
    decisionLabel: String,
    content: @Composable () -> Unit,
) {
    val badgeColor = when (status) {
        ApplicationStatus.Pending -> Color(0xFF775A00)
        ApplicationStatus.Accepted -> Color(0xFF226B3A)
        ApplicationStatus.Rejected -> Color(0xFFAE3030)
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            // Add a thin accent bar across the top of the details card.
            Box(Modifier.fillMaxWidth().height(6.dp).background(barColor)) // 6.dp controls the strip thickness. barColor sets its fill.
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ORGANIZATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(organizationName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(color = badgeColor, shape = MaterialTheme.shapes.large) {
                        Text(statusLabel, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
                Text("Submitted ${relativeTime(submittedAt, Clock.System.now())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (decision.isNotBlank()) {
                    HorizontalDivider()
                    Text(decisionLabel, style = MaterialTheme.typography.titleSmall)
                    Text(decision, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        content()
    }
}

@Composable
private fun ApplicationDescription(title: String, description: String) {
    StageCallCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                .padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ApplicationDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
