package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.remote.AdminOrganizationDraft
import com.weissev.stagecall.data.remote.AdminOrganizationRow
import com.weissev.stagecall.data.remote.AdminOrganizationStore
import kotlinx.coroutines.launch

@Composable
internal fun AdminOrganizationEditorPage(
    organization: AdminOrganizationRow?,
    store: AdminOrganizationStore,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var draft by remember(organization?.id) { mutableStateOf(organization?.draft() ?: AdminOrganizationDraft()) }
    var requirements by remember(organization?.id) { mutableStateOf(draft.requirements.joinToString("\n")) }
    val profileImage = remember(organization?.id) { OrganizationImageSelection() }
    val bannerImage = remember(organization?.id) { OrganizationImageSelection() }
    val enabled = !store.working && !profileImage.loading && !bannerImage.loading
    val currentDraft = draft.copy(requirements = requirements.lines().map { it.trim() }.filter { it.isNotEmpty() })
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (organization == null) "Create organization" else "Edit organization", style = MaterialTheme.typography.headlineSmall)
        if (organization != null) Text(organization.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OrganizationImageEditor("Profile photo", organization?.photoPath, profileImage, enabled)
        OrganizationImageEditor("Banner image", organization?.bannerPath, bannerImage, enabled, banner = true)
        Text("Organization details", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        AdminField("Name", draft.name, 100, enabled) { draft = draft.copy(name = it) }
        AdminField("Category", draft.category, 60, enabled) { draft = draft.copy(category = it) }
        AdminField("Tagline", draft.tagline, 200, enabled) { draft = draft.copy(tagline = it) }
        AdminField("Summary", draft.description, 2000, enabled, true) { draft = draft.copy(description = it) }
        HorizontalDivider()
        Text("About the organization", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        AdminField("About heading", draft.headline, 200, enabled) { draft = draft.copy(headline = it) }
        AdminField("Full description", draft.fullDescription, 2000, enabled, true) { draft = draft.copy(fullDescription = it) }
        HorizontalDivider()
        Text("Recruitment and appearance", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Open recruitment", Modifier.weight(1f))
            Switch(draft.recruiting, { draft = draft.copy(recruiting = it) }, enabled = enabled)
        }
        Text("Banner color", style = MaterialTheme.typography.labelLarge)
        listOf(listOf("blue", "green", "purple"), listOf("orange", "indigo", "gold")).forEach { colors ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.forEach { color -> FilterChip(selected = draft.bannerColor == color,
                    enabled = enabled, onClick = { draft = draft.copy(bannerColor = color) }, label = { Text(color) }) }
            }
        }
        OutlinedTextField(requirements, { requirements = it }, Modifier.fillMaxWidth(), enabled = enabled,
            label = { Text("Requirements (optional)") }, minLines = 3,
            supportingText = { Text("One per line. Up to 30 items, 200 characters each.") })
        if (!currentDraft.valid) Text("Complete the required fields and keep requirements within the limits.", style = MaterialTheme.typography.bodySmall)
        store.error?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }
        if (store.working) LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = enabled, onClick = onCancel) { Text("Cancel") }
            Button(modifier = Modifier.weight(1f), enabled = currentDraft.valid && enabled, onClick = {
                scope.launch { if (store.save(organization?.id, currentDraft,
                    profileImage.photo?.bytes, profileImage.photo?.contentType, profileImage.removed,
                    bannerImage.photo?.bytes, bannerImage.photo?.contentType, bannerImage.removed)) onSaved() }
            }) { Text(if (store.working) "Saving..." else "Save changes") }
        }
    }
}
@Composable
private fun AdminField(label: String, value: String, limit: Int, enabled: Boolean, multiline: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, { if (it.length <= limit) onChange(it) }, Modifier.fillMaxWidth(),
        label = { Text(label) }, enabled = enabled, singleLine = !multiline, minLines = if (multiline) 3 else 1,
        supportingText = { Text("${value.length}/$limit") })
}

