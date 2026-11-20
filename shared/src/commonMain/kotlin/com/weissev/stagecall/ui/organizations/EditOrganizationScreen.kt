package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.Organization
import com.weissev.stagecall.data.remote.OrganizationRoleStore
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import com.weissev.stagecall.ui.components.organizationCardColors
import kotlinx.coroutines.launch

internal val organizationBannerColors = listOf("blue", "green", "purple", "orange", "indigo", "gold")

@Composable
internal fun EditOrganizationPage(
    organization: Organization,
    store: OrganizationRoleStore,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    var headline by remember(organization.id) { mutableStateOf(organization.aboutHeadline) }
    var description by remember(organization.id) { mutableStateOf(organization.fullDescription) }
    var requirements by remember(organization.id) { mutableStateOf(organization.requirements.joinToString("\n")) }
    var recruiting by remember(organization.id) { mutableStateOf(organization.isRecruiting) }
    var color by remember(organization.id) { mutableStateOf(organization.bannerColor) }
    val scope = rememberCoroutineScope()
    var photo by remember(organization.id) { mutableStateOf<UploadPhoto?>(null) }
    var removedPhoto by remember(organization.id) { mutableStateOf(false) }
    var photoError by remember(organization.id) { mutableStateOf<String?>(null) }
    var loadingPhoto by remember(organization.id) { mutableStateOf(false) }
    val enabled = !store.savingSettings && !loadingPhoto
    val existingPhoto = organization.photoPath.takeUnless { removedPhoto }
    val picker = rememberUploadPhotoPicker(
        onLoading = { loadingPhoto = it },
        onError = { photoError = it },
        onSelected = { photo = it; removedPhoto = false },
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Edit organization", style = MaterialTheme.typography.headlineSmall)
        Text(organization.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Organization photo", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OrganizationThumbnail(existingPhoto, size = 72.dp, preview = photo?.preview)
            Column {
                OutlinedButton(enabled = enabled, onClick = { picker.launch() }) {
                    Text(if (existingPhoto == null && photo == null) "Add photo" else "Change photo")
                }
                if (existingPhoto != null || photo != null) {
                    TextButton(enabled = enabled, onClick = {
                        photo = null
                        removedPhoto = true
                        photoError = null
                    }) { Text("Remove photo") }
                }
            }
        }
        Text("JPEG or PNG · up to 5 MB", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        photoError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(headline, { if (it.length <= 120) headline = it }, Modifier.fillMaxWidth(),
            label = { Text("About heading") }, singleLine = true, enabled = enabled)
        OutlinedTextField(description, { if (it.length <= 5000) description = it }, Modifier.fillMaxWidth(),
            label = { Text("About description") }, minLines = 3, enabled = enabled)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = recruiting, onCheckedChange = { recruiting = it }, enabled = enabled)
            Text(if (recruiting) "Recruitment open" else "Recruitment closed")
        }
        OutlinedTextField(requirements, { requirements = it }, Modifier.fillMaxWidth(),
            label = { Text("Membership requirements") }, supportingText = { Text("Enter one requirement per line.") },
            minLines = 3, enabled = enabled)
        Text("Banner color", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup()) {
            organizationBannerColors.forEachIndexed { index, choice ->
                Box(
                    Modifier.size(48.dp).selectable(choice == color, enabled = enabled, role = Role.RadioButton) {
                        color = choice
                    }.semantics { contentDescription = choice.replaceFirstChar { it.uppercase() } },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(34.dp).background(organizationCardColors[index].second, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (color == choice) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = Color.White)
                    }
                }
            }
        }
        store.settingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(enabled = enabled, onClick = onCancel) { Text("Cancel") }
            Button(enabled = enabled && headline.isNotBlank() && description.isNotBlank(), onClick = {
                scope.launch {
                    val requirementList = requirements.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
                    val attachment = photo
                    if (store.saveSettings(organization.id, headline, description, recruiting, color,
                            requirementList, attachment?.bytes, attachment?.contentType, removedPhoto)
                    ) {
                        onSaved()
                    }
                }
            }) { Text(if (store.savingSettings) "Saving…" else "Save") }
        }
    }
}
