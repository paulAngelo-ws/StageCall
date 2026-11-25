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
    LaunchedEffect(store) { store.refresh() }

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
                    if (deleting == null) store.error?.let { message -> item {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                            Text(message, Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    } }
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
                        val accent = when (organization.bannerColor) {
                            "green" -> Color(0xFF27734B)
                            "purple" -> Color(0xFF7A45B3)
                            "orange" -> Color(0xFFAF571D)
                            "indigo" -> Color(0xFF4D54A6)
                            "gold" -> Color(0xFF8C6B13)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        StageCallCard(Modifier.fillMaxWidth()) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(4.dp).background(accent))
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OrganizationThumbnail(photoPath = organization.photoPath, size = 48.dp)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(organization.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                            Text(organization.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(organization.tagline, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = if (organization.recruiting) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
                                            Text(if (organization.recruiting) "Recruiting" else "Closed", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium)
                                        }
                                        Icon(Icons.Outlined.Groups, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${organization.memberCount} members", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { onOpenOrganization(organization.id) }) { Text("View") }
                                        FilledTonalButton(enabled = !store.working && !store.loading, onClick = {
                                            store.clearError()
                                            onEditOrganization(organization.id)
                                        }) {
                                            Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp))
                                            Text("Edit", Modifier.padding(start = 6.dp))
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(enabled = !store.working && !store.loading, onClick = { store.clearError(); deleting = organization }) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${organization.name}", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
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
                store.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            dismissButton = { TextButton(enabled = !store.working, onClick = { deleting = null }) { Text("Cancel") } },
            confirmButton = { TextButton(enabled = !store.working, onClick = {
                scope.launch {
                    if (store.delete(organization.id)) { deleting = null; onCatalogChanged() }
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
