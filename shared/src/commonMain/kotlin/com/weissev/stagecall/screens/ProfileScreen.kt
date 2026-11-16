package com.weissev.stagecall.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.OrganizationThumbnail
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.organizationCardColors
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository

@Composable
fun ProfilePage(
    repository: StageCallRepository,
    onOpenOfficerPortal: () -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenOrganization: (String) -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenApplications: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var editingInterests by remember { mutableStateOf(false) }
    var selectedInterests by remember { mutableStateOf(emptyList<String>()) }
    var showAllOrganizations by remember { mutableStateOf(false) }
    val memberships = repository.members.filter {
        it.student.studentId == repository.profile.studentId && it.active
    }
    val activeApplications = repository.applications.count {
        it.student.studentId == repository.profile.studentId && it.status == ApplicationStatus.Pending
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier.size(72.dp).clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PersonOutline, null, Modifier.size(36.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(repository.profile.name, style = MaterialTheme.typography.headlineSmall)
                    Text("BS Information Technology · Year 2", style = MaterialTheme.typography.bodySmall, color = muted)
                    Text("Student ID: ${repository.profile.studentId}", style = MaterialTheme.typography.bodySmall, color = muted)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(repository.profile.email, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = muted)
                OutlinedButton(onClick = {
                    name = repository.profile.name
                    email = repository.profile.email
                    error = ""
                    editing = true
                }) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp))
                    Text("Edit profile", Modifier.padding(start = 6.dp))
                }
            }
        }
        StageCallCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ProfileStat(memberships.size, "Organizations", Modifier.weight(1f)) {
                    showAllOrganizations = !showAllOrganizations
                }
                ProfileStat(repository.bookmarks.size, "Saved", Modifier.weight(1f), onOpenDirectory)
                ProfileStat(activeApplications, "Active applications", Modifier.weight(1f), onOpenApplications)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My organizations", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (memberships.size > 1) {
                    TextButton(onClick = { showAllOrganizations = !showAllOrganizations }) {
                        Text(if (showAllOrganizations) "Show less" else "View all")
                    }
                }
            }
            if (memberships.isEmpty()) {
                StageCallCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Find your campus community.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onOpenDirectory) { Text("Explore organizations") }
                    }
                }
            }
            for (member in if (showAllOrganizations) memberships else memberships.take(1)) {
                val organization = repository.organization(member.organizationId)
                val (_, accent) = organizationCardColors[
                    repository.organizations.indexOf(organization) % organizationCardColors.size
                ]
                StageCallCard(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .clickable(role = Role.Button, onClickLabel = "Open ${organization.name}") {
                            onOpenOrganization(organization.id)
                        },
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .drawBehind { drawRect(accent, size = Size(6.dp.toPx(), size.height)) }
                            .padding(start = 20.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OrganizationThumbnail()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(organization.name, style = MaterialTheme.typography.titleSmall)
                            Text(member.role, style = MaterialTheme.typography.bodySmall, color = muted)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp))
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My interests", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    selectedInterests = repository.interests.toList()
                    editingInterests = true
                }) { Text("Edit") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (interest in repository.interests) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Text(interest, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text(
                if (repository.interests.isEmpty()) "Add topics you're interested in." else "Topics you're interested in.",
                style = MaterialTheme.typography.bodySmall, color = muted,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            StageCallCard(Modifier.fillMaxWidth()) {
                ProfileAccountRow(Icons.Outlined.Notifications, "Notifications", if (repository.notificationsEnabled) "On" else "Off") {
                    repository.notificationsEnabled = !repository.notificationsEnabled
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileAccountRow(Icons.Outlined.Shield, "Privacy & visibility") {
                    onShowMessage("Application details are shared with organization officers. This prototype uses session-only demo data; real accounts and permissions will be added with the backend.")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileAccountRow(Icons.Outlined.HelpOutline, "Help & support") {
                    onShowMessage("Explore Directory, apply for membership, and track progress in Applications. Use the demo Officer Portal to accept or reject applications and publish posts.")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProfileAccountRow(Icons.Outlined.AdminPanelSettings, "Demo Officer Portal", onClick = onOpenOfficerPortal)
            }
        }
        Text(
            "StageCall · Your campus. Your community.", Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall, color = muted, textAlign = TextAlign.Center,
        )
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it; error = "" }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(email, { email = it; error = "" }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                    if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (repository.updateProfile(name, email)) editing = false
                    else error = "Enter a name and valid email address."
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
        )
    }
    if (editingInterests) {
        AlertDialog(
            onDismissRequest = { editingInterests = false },
            title = { Text("My interests") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (interest in listOf("Mobile dev", "Cybersecurity", "UI/UX", "Web dev", "AI", "Game dev", "Community service")) {
                        FilterChip(
                            selected = interest in selectedInterests,
                            onClick = {
                                selectedInterests = if (interest in selectedInterests)
                                    selectedInterests - interest else selectedInterests + interest
                            },
                            label = { Text(interest) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.interests.clear()
                    repository.interests.addAll(selectedInterests)
                    editingInterests = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingInterests = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileStat(count: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileAccountRow(icon: ImageVector, label: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (value != null) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp))
    }
}
