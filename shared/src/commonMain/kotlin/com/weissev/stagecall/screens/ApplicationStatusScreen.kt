package com.weissev.stagecall.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.StatusPanel
import com.weissev.stagecall.components.organizationCardColors
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository
import kotlin.time.Clock

@Composable
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
    val badgeColor = when (app.status) {
        ApplicationStatus.Pending -> Color(0xFF775A00)
        ApplicationStatus.Accepted -> Color(0xFF226B3A)
        ApplicationStatus.Rejected -> Color(0xFFAE3030)
    }
    val statusMessage = when (app.status) {
        ApplicationStatus.Pending -> "Your application has been submitted and is awaiting review."
        ApplicationStatus.Accepted -> "Your application has been accepted."
        ApplicationStatus.Rejected -> "Your application was not accepted."
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your application", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Membership application details", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(6.dp).background(organizationCardColors[colorIndex].second))
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ORGANIZATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(organization?.name ?: "Organization unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(color = badgeColor, shape = MaterialTheme.shapes.large) {
                        Text(app.status.name, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
                Text("Submitted ${relativePostTime(app.submittedAt, Clock.System.now())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (app.decision.isNotBlank()) {
                    HorizontalDivider()
                    Text("Note from the organization", style = MaterialTheme.typography.titleSmall)
                    Text(app.decision, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ApplicationDetail("Name", app.student.name)
                ApplicationDetail("Email", app.student.email)
                ApplicationDetail("Student ID", app.studentNumber)
                ApplicationDetail("Year level", app.year)
            }
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reason for joining", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(app.reason, modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                    .padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("Check Applications for updates from the organization.", Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ApplicationDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
