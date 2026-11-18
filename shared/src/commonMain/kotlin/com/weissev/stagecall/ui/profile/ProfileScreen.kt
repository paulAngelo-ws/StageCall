package com.weissev.stagecall.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.remote.ProfileStore
import com.weissev.stagecall.data.remote.LocalMediaCache
import com.weissev.stagecall.ui.components.ImageLightbox
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.organizationCardColors
import kotlinx.coroutines.launch

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
    // Keep the draft bio in memory while the edit dialog is open.
    var editing by remember { mutableStateOf(false) }
    var bio by remember { mutableStateOf("") }
    var viewingPicture by remember(accountPicture) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Give MediaCache.kt the Google photo URL from App.kt. Show its saved photo while the host checks for a newer one.
    val media = LocalMediaCache.current
    val picture by produceState<ImageBitmap?>(null, media, accountPicture) {
        value = null
        if (media != null && accountPicture.startsWith("https://")) {
            val cached = media.cachedAvatar(accountPicture) // MediaCache.kt gets the saved photo for this exact URL.
            value = cached // Show that photo while the network check runs.
            try { value = media.avatar(accountPicture, cached) } // Ask MediaCache.kt to see whether the photo has changed.
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { /* Keep the cached photo on network failure. */ }
        }
    }
    var showAllOrganizations by remember { mutableStateOf(false) }
    // StageCallRepository.kt supplies applications and memberships. ProfileStore.kt supplies the bio and saves changes to it.
    // Find the signed-in student’s active memberships for the count and cards below.
    val memberships = repository.members.filter {
        it.student.studentId == repository.profile.studentId && it.active
    }
    val activeApplications = repository.applications.count {
        it.student.studentId == repository.profile.studentId && it.status == ApplicationStatus.Pending
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    if (viewingPicture) picture?.let { ImageLightbox(it, onClose = { viewingPicture = false }) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp), // Outer margin for the profile page.
        verticalArrangement = Arrangement.spacedBy(20.dp), // Vertical gap between the main profile sections.
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
            // Show a circular profile photo or the default person icon.
            Box(
                Modifier.padding(top = 4.dp).size(88.dp).clip(CircleShape) // 88.dp sets the photo diameter. CircleShape crops it into a circle.
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (picture != null) {
                    Image(picture!!, contentDescription = "Open Google profile picture", modifier = Modifier.fillMaxSize().clickable(role = Role.Button) { viewingPicture = true }, contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) // Size of the fallback person icon inside the circle.
                }
            }
            // Give all three profile shortcuts equal space.
            Row(Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(vertical = 4.dp)) { // Caps the shortcut row width on wider screens.
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
            // Show one membership by default, with the option to show the rest.
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
                            // Draw the full-height left accent bar before adding the content padding.
                            .drawBehind { drawRect(accent, size = Size(6.dp.toPx(), size.height)) } // 6.dp sets the bar width. size.height fills the row vertically.
                            .padding(start = 20.dp, top = 14.dp, end = 14.dp, bottom = 14.dp), // Start padding leaves room between the bar and thumbnail.
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
                        // Make the entire row respond to taps on the theme setting.
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
                    Switch(checked = darkTheme, onCheckedChange = null) // The row handles taps while the switch shows the current state.
                }
            }
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Sign out") }
        }
    }
    // Keep the dialog open until the bio saves successfully or the user cancels.
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
