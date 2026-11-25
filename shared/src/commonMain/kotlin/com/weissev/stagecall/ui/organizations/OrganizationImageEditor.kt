package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.ui.components.OrganizationBanner
import com.weissev.stagecall.ui.components.OrganizationThumbnail

@Stable
internal class OrganizationImageSelection {
    var photo by mutableStateOf<UploadPhoto?>(null)
    var removed by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

@Composable
internal fun OrganizationImageEditor(
    title: String,
    path: String?,
    selection: OrganizationImageSelection,
    enabled: Boolean,
    banner: Boolean = false,
) {
    val picker = rememberUploadPhotoPicker(
        onLoading = { selection.loading = it }, onError = { selection.error = it },
        onSelected = { selection.photo = it; selection.removed = false },
    )
    val existing = path.takeUnless { selection.removed }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (banner) OrganizationBanner(existing, preview = selection.photo?.preview)
        else OrganizationThumbnail(existing, size = 72.dp, preview = selection.photo?.preview)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(enabled = enabled && !selection.loading, onClick = { picker.launch() }) {
                Text(if (selection.loading) "Opening..." else if (existing != null || selection.photo != null) "Change image" else "Add image")
            }
            if (existing != null || selection.photo != null) {
                TextButton(enabled = enabled && !selection.loading, onClick = {
                    selection.photo = null; selection.removed = true; selection.error = null
                }) { Text("Remove") }
            }
        }
        Text(if (banner) "JPEG or PNG, up to 5 MB. A wide 16:9 image works best."
            else "JPEG or PNG, up to 5 MB. Use a square logo or profile photo.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        selection.error?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }
    }
}
