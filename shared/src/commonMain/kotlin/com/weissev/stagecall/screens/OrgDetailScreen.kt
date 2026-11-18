package com.weissev.stagecall.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.OrganizationRoleStore
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var tab by remember(id, initialPostId) {
        mutableStateOf(repository.posts.firstOrNull { it.id == initialPostId && it.organizationId == id }?.category
            ?.takeIf { it in listOf("Recruitment", "Events", "Announcements") } ?: "About")
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
                    StageCallCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(it.title, style = MaterialTheme.typography.titleMedium)
                            Text(it.message)
                            if (it.schedule.isNotEmpty()) Text(it.schedule)
                        }
                    }
                }
            }
        }
    }
}
