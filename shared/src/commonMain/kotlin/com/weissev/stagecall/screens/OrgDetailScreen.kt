package com.weissev.stagecall.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.Button
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.organizationCardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.OrganizationRoleStore
import kotlinx.coroutines.launch
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.time.Clock

// repository contains the data shared by all screens.
@Composable
internal fun OrganizationPage(
    repository: StageCallRepository,
    roleStore: OrganizationRoleStore,
    onSettingsSaved: () -> Unit,
    applicationsReady: Boolean,
    rolesReady: Boolean,
    roleError: String?,
    isPresident: Boolean,
    onRetryRoles: () -> Unit,
    onReview: () -> Unit,
    id: String,
    onApply: () -> Unit,
    initialPostId: Int? = null,
) {
    val org = repository.organizations.firstOrNull { it.id == id }
    if (org == null) {
        Text("This organization is no longer available.", Modifier.padding(24.dp))
        return
    }
    val colors = listOf("blue", "green", "purple", "orange", "indigo", "gold")
    val orgColor = organizationCardColors[colors.indexOf(org.bannerColor).coerceAtLeast(0)].second
    var editing by remember(id) { mutableStateOf(false) }
    var creatingPost by remember(id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var tab by remember(id, initialPostId) {
        mutableStateOf(repository.posts.firstOrNull { it.id == initialPostId && it.organizationId == id }?.category
            ?.takeIf { it in listOf("Recruitment", "Events", "Announcements") } ?: "About")
    }
    if (creatingPost && isPresident) {
        var title by remember(id) { mutableStateOf("") }
        var message by remember(id) { mutableStateOf("") }
        var category by remember(id) { mutableStateOf("Announcements") }
        var schedule by remember(id) { mutableStateOf("") }
        var membersOnly by remember(id) { mutableStateOf(false) }
        var imageBytes by remember(id) { mutableStateOf<ByteArray?>(null) }
        var imageType by remember(id) { mutableStateOf<String?>(null) }
        var imageError by remember(id) { mutableStateOf<String?>(null) }
        val picker = rememberFilePickerLauncher(type = PickerType.Image) { file ->
            if (file != null) scope.launch {
                try {
                    if ((file.getSize() ?: 0L) > 5 * 1024 * 1024) error("Choose an image under 5 MB.")
                    val bytes = file.readBytes()
                    val type = when {
                        bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "image/jpeg"
                        bytes.size >= 8 && bytes.take(8) == listOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a).map { it.toByte() } -> "image/png"
                        else -> error("Choose a JPEG or PNG image.")
                    }
                    require(bytes.size <= 5 * 1024 * 1024) { "Choose an image under 5 MB." }
                    bytes.decodeToImageBitmap()
                    imageBytes = bytes
                    imageType = type
                    imageError = null
                } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                    imageError = e.message ?: "Could not open this image."
                }
            }
        }
        Dialog(onDismissRequest = { if (!roleStore.creatingPost) creatingPost = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Column(Modifier.fillMaxWidth().heightIn(max = 700.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Column(Modifier.fillMaxWidth().background(orgColor).padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ORGANIZATION UPDATE", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        Text("Create a post", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(org.name, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Share something with your campus community.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(title, { if (it.length <= 120) title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(message, { if (it.length <= 5000) message = it }, label = { Text("Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    imageBytes?.let { bytes ->
                        val preview = remember(bytes) { runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
                        preview?.let { Image(it, contentDescription = "Selected photo", modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !roleStore.creatingPost, onClick = { picker.launch() }) { Text(if (imageBytes == null) "Add photo" else "Change photo") }
                        if (imageBytes != null) TextButton(enabled = !roleStore.creatingPost, onClick = { imageBytes = null; imageType = null; imageError = null }) { Text("Remove") }
                    }
                    Text("JPEG or PNG · up to 5 MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Recruitment", "Events", "Announcements").forEach { choice ->
                            FilterChip(selected = category == choice, onClick = { category = choice }, label = { Text(choice) })
                        }
                    }
                    OutlinedTextField(schedule, { if (it.length <= 200) schedule = it }, label = { Text("Date, time or venue (optional)") }, placeholder = { Text("e.g. Dec 5, 2 PM · Main Hall") }, modifier = Modifier.fillMaxWidth())
                    Text("Audience", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !membersOnly, onClick = { membersOnly = false }, label = { Text("Public") })
                        FilterChip(selected = membersOnly, onClick = { membersOnly = true }, label = { Text("Members only") })
                    }
                    Text(if (membersOnly) "Only members of ${org.name} can see this post." else "Everyone can see this post.", style = MaterialTheme.typography.bodySmall)
                    roleStore.postError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !roleStore.creatingPost, onClick = { creatingPost = false }) { Text("Cancel") }
                Button(enabled = !roleStore.creatingPost && title.isNotBlank() && message.isNotBlank(), onClick = {
                    scope.launch {
                        if (roleStore.createPost(id, title, message, category, schedule, membersOnly, imageBytes, imageType)) {
                            tab = category
                            creatingPost = false
                        }
                    }
                }) { Text(if (roleStore.creatingPost) "Publishing…" else "Publish") }
            }
                }
            }
        }
    }
    if (editing && isPresident) {
        var headline by remember(id) { mutableStateOf(org.aboutHeadline) }
        var description by remember(id) { mutableStateOf(org.fullDescription) }
        var recruiting by remember(id) { mutableStateOf(org.isRecruiting) }
        var color by remember(id) { mutableStateOf(org.bannerColor) }
        AlertDialog(
            onDismissRequest = { if (!roleStore.savingSettings) editing = false },
            title = { Text("Edit organization") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(headline, { if (it.length <= 120) headline = it }, label = { Text("About heading") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { if (it.length <= 5000) description = it }, label = { Text("About description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(checked = recruiting, onCheckedChange = { recruiting = it })
                        Text(if (recruiting) "Registration open" else "Registration closed")
                    }
                    Text("Banner color", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup()) {
                        colors.forEachIndexed { index, choice ->
                            Box(
                                modifier = Modifier.size(48.dp).selectable(
                                    selected = color == choice,
                                    onClick = { color = choice },
                                    enabled = !roleStore.savingSettings,
                                    role = Role.RadioButton,
                                ).semantics { contentDescription = choice.replaceFirstChar { it.uppercase() } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.size(34.dp).background(organizationCardColors[index].second, CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    if (color == choice) Icon(Icons.Filled.Check, contentDescription = null,
                                        tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    roleStore.settingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !roleStore.savingSettings && headline.isNotBlank() && description.isNotBlank(), onClick = {
                    scope.launch { if (roleStore.saveSettings(org, headline, description, recruiting, color)) { editing = false; onSettingsSaved() } }
                }) { Text(if (roleStore.savingSettings) "Saving…" else "Save") }
            },
            dismissButton = { TextButton(enabled = !roleStore.savingSettings, onClick = { editing = false }) { Text("Cancel") } },
        )
    }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StageCallCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = orgColor,
                contentColor = Color.White,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = org.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${org.memberCount} members",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (roleError != null) {
            Text(roleError, color = MaterialTheme.colorScheme.error)
            androidx.compose.material3.OutlinedButton(onClick = onRetryRoles) { Text("Retry") }
        }
        if (isPresident) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReview, modifier = Modifier.weight(1f)) { Text("Review applications") }
                OutlinedButton(onClick = { editing = true }, modifier = Modifier.weight(1f)) { Text("Edit organization") }
            }
            Button(onClick = { roleStore.clearPostError(); creatingPost = true }, modifier = Modifier.fillMaxWidth()) { Text("Create post") }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (option in listOf("About", "Recruitment", "Events", "Announcements")) {
                Column(
                    modifier = Modifier.selectable(selected = tab == option, role = Role.Tab, onClick = { tab = option }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(option, modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        color = if (tab == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (tab == option) FontWeight.SemiBold else FontWeight.Normal)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(
                        if (tab == option) MaterialTheme.colorScheme.primary else Color.Transparent
                    ))
                }
            }
        }
        when (tab) {
            "About" -> {
                Text(org.aboutHeadline, style = MaterialTheme.typography.titleLarge)
                Text(org.fullDescription)
                Text("Membership requirements", style = MaterialTheme.typography.titleMedium)
                org.requirements.forEach { Text("- $it") }
                Text("Application deadline: ${org.deadline}")
                StageCallCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("NEXT ACTIVITY")
                        Text(org.nextActivityTitle)
                        Text(org.nextActivitySchedule)
                    }
                }
                if (!repository.isMember(id)) Button(
                    onClick = onApply,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    enabled = applicationsReady && rolesReady && repository.canApply(id),
                ) {
                    Text(
                        text = when {
                            !rolesReady -> "Loading organization membership"
                            !applicationsReady -> "Applications unavailable — check Applications to retry"
                            !org.isRecruiting -> "Recruitment closed"
                            !repository.canApply(id) -> "Application already submitted"
                            else -> "Apply"
                        }
                    )
                }
            }
            else -> {
                val posts =
                    repository.posts.filter {
                        it.organizationId == id && it.category == tab &&
                            (!it.membersOnly || repository.isMember(id))
                    }.sortedByDescending { it.id == initialPostId }
                if (posts.isEmpty()) Text("No posts in this category yet.")
                posts.forEach {
                    PostCard(it, org, Clock.System.now())
                }
            }
        }
    }
}
