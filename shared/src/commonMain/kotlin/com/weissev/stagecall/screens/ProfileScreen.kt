package com.weissev.stagecall.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weissev.stagecall.components.OrganizationThumbnail
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.organizationCardColors
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository

@Composable
fun ProfilePage(
    repository: StageCallRepository,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onOpenOrganization: (String) -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenApplications: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var showingAbout by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                repository.profile.name.ifBlank { "Guest" },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Box(
                Modifier.padding(top = 4.dp).size(88.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Row(Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(vertical = 4.dp)) {
                ProfileStat(memberships.size, "Organizations", Modifier.weight(1f)) {
                    showAllOrganizations = !showAllOrganizations
                }
                ProfileStat(repository.bookmarks.size, "Saved", Modifier.weight(1f), onOpenDirectory)
                ProfileStat(activeApplications, "Active applications", Modifier.weight(1f), onOpenApplications)
            }
            Row(
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    enabled = repository.profile.studentId.isNotBlank(),
                    onClick = {
                        name = repository.profile.name
                        email = repository.profile.email
                        error = ""
                        editing = true
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp))
                    Text("Edit profile", Modifier.padding(start = 6.dp))
                }
                FilledTonalButton(
                    enabled = repository.profile.studentId.isNotBlank(),
                    onClick = { showingAbout = true },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text("About")
                }
            }
            Text(
                text = if (repository.profile.studentId.isBlank()) "Browsing as a guest" else "",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Account", style = MaterialTheme.typography.titleMedium)
            StageCallCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(value = darkTheme, role = Role.Switch, onValueChange = onDarkThemeChange)
                        .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (darkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Theme", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (darkTheme) "Dark" else "Light",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = darkTheme, onCheckedChange = null)
                }
            }
        }
    }
    if (showingAbout) {
        AlertDialog(
            onDismissRequest = { showingAbout = false },
            title = { Text("About ${repository.profile.name}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Student ID", style = MaterialTheme.typography.labelMedium, color = muted)
                        Text(repository.profile.studentId, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Course / Year", style = MaterialTheme.typography.labelMedium, color = muted)
                        Text("BS Information Technology · Year 2", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Email", style = MaterialTheme.typography.labelMedium, color = muted)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.Email, null, Modifier.size(18.dp), tint = muted)
                            Text(repository.profile.email, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showingAbout = false }) { Text("Close") } },
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
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
