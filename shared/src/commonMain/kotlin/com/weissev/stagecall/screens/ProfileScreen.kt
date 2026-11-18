package com.weissev.stagecall.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
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
import com.weissev.stagecall.data.ProfileStore
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.jetbrains.compose.resources.decodeToImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
internal fun ProfilePage(
    repository: StageCallRepository,
    accountEmail: String,
    accountName: String,
    accountPicture: String,
    profileStore: ProfileStore,
    onSaved: () -> Unit,
    onSignOut: () -> Unit,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onOpenOrganization: (String) -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenApplications: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picture by produceState<ImageBitmap?>(null, accountPicture) {
        if (accountPicture.startsWith("https://")) {
            val http = HttpClient()
            try { value = http.get(accountPicture).body<ByteArray>().decodeToImageBitmap() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { value = null }
            finally { http.close() }
        }
    }
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
                accountName.ifBlank { accountEmail.ifBlank { "Account" } },
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
                if (picture != null) {
                    Image(picture!!, contentDescription = "Google profile picture", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Row(Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(vertical = 4.dp)) {
                ProfileStat(memberships.size, "Organizations", Modifier.weight(1f)) {
                    showAllOrganizations = !showAllOrganizations
                }
                ProfileStat(repository.bookmarks.size, "Saved", Modifier.weight(1f), onOpenDirectory)
                ProfileStat(activeApplications, "Active applications", Modifier.weight(1f), onOpenApplications)
            }
            Button(
                enabled = profileStore.loaded,
                onClick = { bio = profileStore.bio; editing = true },
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp))
                Text("Edit profile", Modifier.padding(start = 6.dp))
            }
            Text(
                text = if (accountEmail.isNotBlank()) accountEmail else "Signed in with Google",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (profileStore.bio.isNotBlank()) {
                Text("\u201c${profileStore.bio}\u201d", modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = muted)
            }
            if (!profileStore.loaded && profileStore.error != null) {
                Text(profileStore.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                TextButton(onClick = { scope.launch { profileStore.load() } }) { Text("Retry") }
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
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Sign out") }
        }
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { if (!profileStore.saving) editing = false },
            title = { Text("Edit profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(bio, { if (it.length <= 500) bio = it }, Modifier.fillMaxWidth(),
                        label = { Text("Bio") }, supportingText = { Text("${bio.length}/500") }, minLines = 3)
                    profileStore.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !profileStore.saving, onClick = {
                    scope.launch { if (profileStore.save(bio)) { editing = false; onSaved() } }
                }) { Text(if (profileStore.saving) "Saving..." else "Save") }
            },
            dismissButton = { TextButton(enabled = !profileStore.saving, onClick = { editing = false }) { Text("Cancel") } },
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
