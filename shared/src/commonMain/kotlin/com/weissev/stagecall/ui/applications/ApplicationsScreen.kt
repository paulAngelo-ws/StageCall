package com.weissev.stagecall.ui.applications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.organizationCardColors
import kotlin.time.Instant

// App.kt passes the repository here. ApplicationStore.kt fills it with this student’s saved and refreshed applications.
@Composable
fun ApplicationsPage(
    repository: StageCallRepository,
    onOpenApplication: (Int) -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenOrganization: (String) -> Unit,
    onOpenHistory: () -> Unit,
    refreshError: String? = null,
    onRetry: () -> Unit = {},
) = ApplicationsContent(repository, onOpenApplication, onOpenDirectory, onOpenOrganization, onOpenHistory,
    refreshError = refreshError, onRetry = onRetry)

@Composable
fun ApplicationHistoryPage(
    repository: StageCallRepository,
    onOpenApplication: (Int) -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenOrganization: (String) -> Unit,
) = ApplicationsContent(repository, onOpenApplication, onOpenDirectory, onOpenOrganization, historyOnly = true)

@Composable
private fun ApplicationsContent(
    repository: StageCallRepository,
    onOpenApplication: (Int) -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenOrganization: (String) -> Unit,
    onOpenHistory: (() -> Unit)? = null,
    historyOnly: Boolean = false,
    refreshError: String? = null,
    onRetry: () -> Unit = {},
) {
    // Use the signed-in student’s applications for the counts and lists.
    val userApplications = repository.applications.filter {
        it.student.studentId == repository.profile.studentId // Match the request’s student ID to the signed-in student.
    }
    val activeCount = userApplications.count { it.status == ApplicationStatus.Pending } // Count waiting requests as active.
    val pastCount = userApplications.size - activeCount // Everything outside the active count already has a decision.
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Remember how far the student has scrolled.
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (historyOnly) "Application history" else "Applications",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (onOpenHistory != null) IconButton(onClick = onOpenHistory) {
                BadgedBox(badge = {
                    if (pastCount > 0) Badge { Text(pastCount.toString()) } // Hide the number when there is no history.
                }) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = "Open application history ($pastCount past applications)",
                    )
                }
            }
        }
        Text(
            text = if (historyOnly) "$pastCount past applications"
                else "${userApplications.size} applications found · $activeCount active",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (refreshError != null) {
            Text("Could not refresh applications. Showing the last loaded results.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry) { Text("Retry") } // App.kt handles the retry through ApplicationStore.kt.
        }
        // Build the list shown below. The main page includes every request. History includes only decided requests.
        val apps =
            userApplications.filter { application ->
                !historyOnly || application.status != ApplicationStatus.Pending // In history, leave out requests still waiting for a decision.
            }
        if (apps.isEmpty()) {
            StageCallCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("No applications here yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (historyOnly) "Your completed membership requests will appear here."
                        else "Explore the directory and find a community to join.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // Turn each request into a card with the organization’s name, color, and the right destination when tapped.
        for (app in apps) {
            val organization = repository.organization(app.organizationId) // Look up the organization by the ID saved on the request.
            val (_, barColor) = organizationCardColors[
                repository.organizations.indexOf(organization) % organizationCardColors.size // Use its place in the directory to pick a color from OrganizationCardStyle.kt.
            ]
            val accepted = app.status == ApplicationStatus.Accepted // An accepted request links to the organization instead of the request details.
            ApplicationCard(
                title = organization.name,
                subtitle = "General membership",
                barColor = barColor,
                status = app.status,
                submittedAt = app.submittedAt,
                updatedAt = app.updatedAt,
                message = if (app.status == ApplicationStatus.Pending) "Your application has been submitted."
                    else app.decision.ifBlank { // Show the reviewer’s note when one was saved.
                        if (accepted) "You're in! Get to know your new community."
                        else "Your application has been rejected."
                    },
                actionLabel = if (accepted) "Open organization" else "View application",
            ) {
                // App.kt receives either the organization ID or the application ID and opens the matching screen.
                if (accepted) onOpenOrganization(app.organizationId) else onOpenApplication(app.id) // Send the ID to the matching route in App.kt.
            }
        }
        OutlinedButton(
            onClick = onOpenDirectory, // Ask App.kt to switch to DirectoryScreen.kt.
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Explore organizations") }
    }
}

@Composable
internal fun ApplicationCard(
    title: String,
    subtitle: String,
    barColor: Color,
    status: ApplicationStatus,
    submittedAt: Instant,
    updatedAt: Instant?,
    message: String,
    actionLabel: String,
    statusLabel: String = if (status == ApplicationStatus.Pending) "In review" else status.name,
    onClick: () -> Unit,
) {
    val cardContentColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    // Choose an icon that matches the decision shown on the card.
    val statusIcon = when (status) {
        ApplicationStatus.Pending -> Icons.Outlined.Schedule // A clock shows the request is still waiting.
        ApplicationStatus.Accepted -> Icons.Outlined.CheckCircle // A check mark shows the request was accepted.
        ApplicationStatus.Rejected -> Icons.Outlined.Cancel // A cross shows the request was rejected.
    }
    StageCallCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick), // Make the whole card act as a button.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = cardContentColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Draw the colored strip down the full left edge of the card.
                    drawRect(barColor, size = Size(6.dp.toPx(), size.height)) // 6.dp controls the width of the left strip.
                }
                .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OrganizationThumbnail() // Use the shared thumbnail from OrganizationCardStyle.kt.
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = cardContentColor,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(statusIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Box(Modifier.weight(1f)) // Push the date to the right side of the row.
                Text(
                    text = updatedAt?.let { "Updated ${applicationCardDate(it)}" }
                        ?: "Applied ${applicationCardDate(submittedAt)}", // Use the submission date when there is no update date.
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor,
                )
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor,
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    actionLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun applicationCardDate(instant: Instant): String {
    // Make a short date such as Nov 19 from the year, month, and day in the timestamp.
    val date = instant.toString().take(10) // Keep the date and leave off the time.
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${months[date.substring(5, 7).toInt() - 1]} ${date.substring(8, 10).toInt()}" // Subtract one because the month list starts at index zero.
}
