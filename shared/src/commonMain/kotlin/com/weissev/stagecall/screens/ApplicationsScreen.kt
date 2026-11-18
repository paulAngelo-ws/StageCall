package com.weissev.stagecall.screens

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
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.OrganizationThumbnail
import com.weissev.stagecall.components.organizationCardColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository
import kotlin.time.Instant

// repository contains the data shared by all screens.
@Composable
fun ApplicationsPage(
    repository: StageCallRepository,
    onOpenApplication: (Int) -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenOrganization: (String) -> Unit,
    onOpenHistory: () -> Unit,
) = ApplicationsContent(repository, onOpenApplication, onOpenDirectory, onOpenOrganization, onOpenHistory)

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
) {
    val userApplications = repository.applications.filter {
        it.student.studentId == repository.profile.studentId
    }
    val activeCount = userApplications.count { it.status == ApplicationStatus.Pending }
    val pastCount = userApplications.size - activeCount
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                    if (pastCount > 0) Badge { Text(pastCount.toString()) }
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
        val apps =
            userApplications.filter { application ->
                !historyOnly || application.status != ApplicationStatus.Pending
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
        for (app in apps) {
            val organization = repository.organization(app.organizationId)
            val (_, barColor) = organizationCardColors[
                repository.organizations.indexOf(organization) % organizationCardColors.size
            ]
            val cardContentColor = MaterialTheme.colorScheme.onSurface
            val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
            val statusIcon = when (app.status) {
                ApplicationStatus.Pending -> Icons.Outlined.Schedule
                ApplicationStatus.Accepted -> Icons.Outlined.CheckCircle
                ApplicationStatus.Rejected -> Icons.Outlined.Cancel
            }
            StageCallCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (app.status == ApplicationStatus.Accepted)
                            "Open ${organization.name}" else "View application for ${organization.name}",
                    ) {
                        if (app.status == ApplicationStatus.Accepted)
                            onOpenOrganization(app.organizationId)
                        else onOpenApplication(app.id)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = cardContentColor,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(barColor, size = Size(6.dp.toPx(), size.height))
                        }
                        .padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OrganizationThumbnail()
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(organization.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "General membership",
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
                                    if (app.status == ApplicationStatus.Pending) "In review" else app.status.name,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        Box(Modifier.weight(1f))
                        Text(
                            text = app.updatedAt?.let { "Updated ${applicationCardDate(it)}" }
                                ?: "Applied ${applicationCardDate(app.submittedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor,
                        )
                    }
                    Text(
                        if (app.status == ApplicationStatus.Pending)
                            "The team is reviewing your application."
                        else
                            app.decision.ifBlank {
                                if (app.status == ApplicationStatus.Accepted)
                                    "You're in! Get to know your new community."
                                else "Your application has been rejected."
                            },
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
                            if (app.status == ApplicationStatus.Accepted) "Open organization" else "View application",
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
        OutlinedButton(
            onClick = onOpenDirectory,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Explore organizations") }
    }
}

private fun applicationCardDate(instant: Instant): String {
    val date = instant.toString().take(10)
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${months[date.substring(5, 7).toInt() - 1]} ${date.substring(8, 10).toInt()}"
}
