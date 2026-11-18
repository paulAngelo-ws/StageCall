package com.weissev.stagecall.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.StatusPanel
import com.weissev.stagecall.components.organizationCardColors
import com.weissev.stagecall.data.ApplicationStatus
import com.weissev.stagecall.data.MembershipApplication
import com.weissev.stagecall.data.OrganizationRoleStore
import kotlinx.coroutines.launch
import kotlin.time.Clock

@Composable
internal fun ReviewApplicationsPage(store: OrganizationRoleStore, organizationId: String, organizationName: String, bannerColor: String, onDecisionSaved: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    val colorIndex = listOf("blue", "green", "purple", "orange", "indigo", "gold").indexOf(bannerColor).coerceAtLeast(0)
    val accentColor = organizationCardColors[colorIndex].second
    var selected by remember(organizationId) { mutableStateOf<MembershipApplication?>(null) }
    var details by remember(organizationId) { mutableStateOf<MembershipApplication?>(null) }
    var accept by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var filter by remember(organizationId) { mutableStateOf("Pending") }
    var query by remember(organizationId) { mutableStateOf("") }
    var sort by remember(organizationId) { mutableStateOf("Newest") }
    var filterMenu by remember { mutableStateOf(false) }
    LaunchedEffect(store, organizationId) { store.loadApplications(organizationId) }
    val matching = store.applications.filter { application ->
        (filter == "All" || application.status.name == filter) &&
            listOf(application.student.name, application.student.email, application.studentNumber)
                .any { it.contains(query.trim(), ignoreCase = true) }
    }
    val applications = when (sort) {
        "Oldest" -> matching.sortedBy { it.submittedAt }
        "Year level" -> matching.sortedWith(compareBy<MembershipApplication> { it.year }.thenByDescending { it.submittedAt })
        else -> matching.sortedByDescending { it.submittedAt }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Review applications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(organizationName, style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Row(
                Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (option in listOf("Pending", "Accepted", "Rejected", "All")) {
                    val count = if (option == "All") store.applications.size else store.applications.count { it.status.name == option }
                    val countLabel = if (count > 10) "10+" else count.toString()
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text("$option ($countLabel)") })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Search name, email, or student ID" },
                    decorationBox = { innerTextField ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) Text("Search applicants", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                innerTextField()
                            }
                        }
                    },
                )
                Box {
                    IconButton(onClick = { filterMenu = true }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Sort applications: $sort")
                    }
                    DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                        Text("Sort by", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                        for (option in listOf("Newest", "Oldest", "Year level")) {
                            DropdownMenuItem(text = { Text(option) }, onClick = { sort = option; filterMenu = false },
                                trailingIcon = { if (sort == option) Icon(Icons.Outlined.Check, contentDescription = "Selected") })
                        }
                    }
                }
            }
        }
        item { Text("Sort: $sort", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        store.reviewError?.let { error -> item {
            StatusPanel(error, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), isError = true,
                actionLabel = "Retry", onAction = { scope.launch { store.loadApplications(organizationId) } })
        } }
        if (store.reviewing && store.applications.isEmpty()) item {
            StatusPanel("Loading applications…", modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), loading = true)
        } else if (applications.isEmpty() && store.reviewError == null) item {
            StatusPanel(if (store.applications.isEmpty()) "No applications yet." else "No applications match these filters.",
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp))
        }
        items(applications, key = { it.id }) { application ->
            StageCallCard(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(accentColor))
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(application.student.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val badgeColor = when (application.status) {
                            ApplicationStatus.Pending -> androidx.compose.ui.graphics.Color(0xFF775A00)
                            ApplicationStatus.Accepted -> androidx.compose.ui.graphics.Color(0xFF226B3A)
                            ApplicationStatus.Rejected -> androidx.compose.ui.graphics.Color(0xFFAE3030)
                        }
                        Surface(color = badgeColor, shape = MaterialTheme.shapes.large) {
                            Text(application.status.name, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text("${application.year} • ID: ${application.studentNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(application.student.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Submitted ${relativePostTime(application.submittedAt, Clock.System.now())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    var overflowing by remember(application.id, application.reason) { mutableStateOf(false) }
                    Text(application.reason, modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                        .clickable(onClickLabel = "View full application") { details = application }
                        .padding(12.dp), maxLines = 3, overflow = TextOverflow.Ellipsis,
                        onTextLayout = { overflowing = it.hasVisualOverflow })
                    if (overflowing) TextButton(onClick = { details = application }, contentPadding = PaddingValues(0.dp)) { Text("Read more") }
                    if (application.decision.isNotBlank()) Text("Decision note: ${application.decision}", style = MaterialTheme.typography.bodySmall)
                    if (application.status == ApplicationStatus.Pending) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { selected = application; accept = true; message = "" }, modifier = Modifier.weight(1f), enabled = !store.reviewing) { Text("Accept") }
                            OutlinedButton(onClick = { selected = application; accept = false; message = "" }, modifier = Modifier.weight(1f), enabled = !store.reviewing) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
    details?.let { application ->
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text("Application details") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(application.student.name, style = MaterialTheme.typography.titleMedium)
                    Text(application.student.email)
                    Text("${application.year} • ID: ${application.studentNumber}")
                    Text("Status: ${application.status.name}")
                    Text("Submitted ${relativePostTime(application.submittedAt, Clock.System.now())}")
                    Text("Reason for joining", style = MaterialTheme.typography.titleSmall)
                    Text(application.reason)
                    if (application.decision.isNotBlank()) {
                        Text("Decision note", style = MaterialTheme.typography.titleSmall)
                        Text(application.decision)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { details = null }) { Text("Close") } },
        )
    }
    selected?.let { application ->
        AlertDialog(
            onDismissRequest = { if (!store.reviewing) selected = null },
            title = { Text(if (accept) "Accept application?" else "Reject application?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${application.student.name} · $organizationName")
                    OutlinedTextField(message, { if (it.length <= 2000) message = it }, label = { Text("Note to applicant (optional)") })
                    store.reviewError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = !store.reviewing, onClick = {
                    scope.launch {
                        if (store.decide(organizationId, application.id, accept, message)) {
                            selected = null
                            onDecisionSaved(accept)
                            store.loadApplications(organizationId)
                        }
                    }
                }) { Text(if (store.reviewing) "Saving…" else if (accept) "Accept" else "Reject") }
            },
            dismissButton = { TextButton(enabled = !store.reviewing, onClick = { selected = null }) { Text("Cancel") } },
        )
    }
}
