package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.remote.AdminOrganizationRow
import com.weissev.stagecall.data.remote.AdminOrganizationStore
import com.weissev.stagecall.data.remote.OrganizationRegistrationStore
import com.weissev.stagecall.ui.registrations.OrganizationRegistrationReviewPage
import kotlinx.coroutines.launch

@Composable
internal fun AdminPanelPage(
    store: AdminOrganizationStore,
    registrations: OrganizationRegistrationStore,
    onOpenOrganization: (String) -> Unit,
    onCatalogChanged: () -> Unit,
    onEditOrganization: (String?) -> Unit,
    onDecisionSaved: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var applications by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<AdminOrganizationRow?>(null) }
    var deletionNote by remember { mutableStateOf("") }
    LaunchedEffect(store) { store.refresh() }
    com.weissev.stagecall.ui.components.ErrorNotice(store.error)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 960.dp).fillMaxSize()) {
            TabRow(selectedTabIndex = if (applications) 1 else 0, containerColor = MaterialTheme.colorScheme.background) {
                Tab(selected = !applications, onClick = { applications = false }, text = { Text("Organizations") })
                Tab(selected = applications, onClick = { applications = true }, text = { Text("Applications") })
            }
            if (applications) {
                Box(Modifier.weight(1f)) {
                    OrganizationRegistrationReviewPage(registrations, onDecisionSaved)
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminMetric(if (store.loaded) store.organizations.size.toString() else "—", "Organizations", Modifier.weight(1f))
                            AdminMetric(if (store.loaded) store.organizations.count { it.recruiting }.toString() else "—", "Recruiting", Modifier.weight(1f))
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Manage organizations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    Text("Keep the campus directory up to date.", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(enabled = !store.loading && !store.working,
                                    onClick = { scope.launch { store.refresh() } }) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh organizations")
                                }
                            }
                            FilledTonalButton(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp), enabled = !store.working && !store.loading, onClick = {
                                    store.clearError()
                                    onEditOrganization(null)
                                }) {
                                Icon(Icons.Outlined.Add, null, Modifier.size(20.dp))
                                Text("Create organization", Modifier.padding(start = 8.dp))
                            }
                            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp), placeholder = { Text("Search name or category") },
                                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                } }, singleLine = true)
                        }
                    }
                    if (store.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    val results = store.organizations.filter { it.name.contains(search, true) || it.category.contains(search, true) }
                    if (store.loaded && results.isEmpty()) item {
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Groups, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(if (search.isBlank()) "Start your campus directory" else "No organizations found", style = MaterialTheme.typography.titleMedium)
                                Text(if (search.isBlank()) "Create an organization to get started." else "Try a different name or category.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(results, key = { it.id }) { organization ->
                        val org = com.weissev.stagecall.data.Organization(
                            organization.id, organization.name, organization.category, organization.description,
                            organization.tagline, organization.headline, organization.fullDescription,
                            organization.memberCount, organization.recruiting, organization.requirements,
                            "", "", organization.bannerColor, organization.photoPath, organization.bannerPath)
                        OrganizationDirectoryCard(org, store.organizations.indexOf(organization), { onOpenOrganization(organization.id) }) { contentColor ->
                            IconButton(enabled = !store.working && !store.loading, onClick = {
                                store.clearError(); onEditOrganization(organization.id)
                            }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit ${organization.name}", tint = contentColor)
                            }
                            IconButton(enabled = !store.working && !store.loading, onClick = {
                                store.clearError(); deletionNote = ""; deleting = organization
                            }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${organization.name}", tint = contentColor)
                            }
                        }
                    }
                }
            }
    }
    }
    deleting?.let { organization ->
        AlertDialog(onDismissRequest = { if (!store.working) deleting = null },
            title = { Text("Delete ${organization.name}?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This permanently deletes the organization, its posts, memberships, membership applications and related notifications. Registration history is kept. This cannot be undone.")
                OutlinedTextField(deletionNote, { if (it.length <= 1000) deletionNote = it },
                    Modifier.fillMaxWidth(), enabled = !store.working,
                    label = { Text("Reason / notes (required)") }, minLines = 3,
                    supportingText = { Text("Sent to the person who registered the organization. ${deletionNote.length}/1000") })
            } },
            dismissButton = { TextButton(enabled = !store.working, onClick = { deleting = null }) { Text("Cancel") } },
            confirmButton = { TextButton(enabled = !store.working && deletionNote.isNotBlank(), onClick = {
                scope.launch {
                    if (store.delete(organization.id, deletionNote)) { deleting = null; onCatalogChanged() }
                }
            }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(if (store.working) "Deleting..." else "Delete organization") } },
        )
    }
}

@Composable
private fun AdminMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
