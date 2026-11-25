package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.CampusPost
import com.weissev.stagecall.data.Organization
import com.weissev.stagecall.data.remote.OrganizationRoleStore
import com.weissev.stagecall.ui.feed.PostImage
import kotlinx.coroutines.launch
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateOrganizationPostPage(
    organization: Organization,
    store: OrganizationRoleStore,
    post: CampusPost?,
    onCancel: () -> Unit,
    onPublished: (Int) -> Unit,
) {
    val initialSchedule = remember(post?.id) { parsePostSchedule(post?.schedule.orEmpty()) }
    var title by remember(post?.id) { mutableStateOf(post?.title.orEmpty()) }
    var message by remember(post?.id) { mutableStateOf(post?.message.orEmpty()) }
    var category by remember(post?.id) { mutableStateOf(post?.category ?: "Announcements") }
    var dateMillis by remember(post?.id) { mutableStateOf(initialSchedule.dateMillis) }
    var timeMinutes by remember(post?.id) { mutableStateOf(initialSchedule.timeMinutes) }
    var legacySchedule by remember(post?.id) { mutableStateOf(initialSchedule.legacyText) }
    var choosingDate by remember(organization.id) { mutableStateOf(false) }
    var choosingTime by remember(organization.id) { mutableStateOf(false) }
    val date = dateMillis?.let { Instant.fromEpochMilliseconds(it).toString().substringBefore('T') }
    val time = timeMinutes?.let { minutes ->
        val hour = minutes / 60
        val hourOfPeriod = (hour + 11) % 12 + 1
        val minute = (minutes % 60).toString().padStart(2, '0')
        val period = if (hour < 12) "AM" else "PM"
        "$hourOfPeriod:$minute $period"
    }
    var membersOnly by remember(post?.id) { mutableStateOf(post?.membersOnly ?: false) }
    var photo by remember(organization.id) { mutableStateOf<UploadPhoto?>(null) }
    var removePhoto by remember(post?.id) { mutableStateOf(false) }
    val existingPhoto = post?.imagePath.takeUnless { removePhoto }
    var photoError by remember(organization.id) { mutableStateOf<String?>(null) }
    var loadingPhoto by remember(organization.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val enabled = !store.creatingPost && !loadingPhoto

    val picker = rememberUploadPhotoPicker(
        onLoading = { loadingPhoto = it },
        onError = { photoError = it },
        onSelected = {
            photo = it
            removePhoto = false
        },
    )

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (post == null) "Create a post" else "Edit post", style = MaterialTheme.typography.headlineSmall)
            Text(organization.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(title, { if (it.length <= 120) title = it }, Modifier.fillMaxWidth(),
                label = { Text("Title (required)") }, singleLine = true, enabled = enabled)
            OutlinedTextField(message, { if (it.length <= 5000) message = it }, Modifier.fillMaxWidth(),
                label = { Text("Message") }, minLines = 4, enabled = enabled)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = enabled, onClick = { picker.launch() }) {
                    Text(if (photo == null && existingPhoto == null) "Add photo" else "Change photo")
                }
                if (photo != null || existingPhoto != null) {
                    TextButton(enabled = enabled, onClick = {
                        photo = null
                        removePhoto = true
                        photoError = null
                    }) { Text("Remove photo") }
                }
            }
            photo?.let {
                Image(it.preview, contentDescription = "Selected photo",
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop)
            }
            if (photo == null && existingPhoto != null) PostImage(existingPhoto)
            Text("JPEG or PNG · up to 5 MB", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            photoError?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }

            Text("Category", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Recruitment", "Events", "Announcements").forEach { choice ->
                    FilterChip(selected = category == choice, onClick = { category = choice },
                        label = { Text(choice) }, enabled = enabled)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Date and time (optional)", style = MaterialTheme.typography.titleSmall)
                legacySchedule?.let { text ->
                    Text("Existing schedule · $text", style = MaterialTheme.typography.bodyMedium)
                    TextButton(enabled = enabled, onClick = { legacySchedule = null }) { Text("Clear schedule") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        OutlinedButton(enabled = enabled, onClick = { choosingDate = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(date ?: "Choose date")
                        }
                        if (date != null) {
                            TextButton(enabled = enabled, onClick = { dateMillis = null }) { Text("Clear date") }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        OutlinedButton(enabled = enabled, onClick = { choosingTime = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(time ?: "Choose time")
                        }
                        if (time != null) {
                            TextButton(enabled = enabled, onClick = { timeMinutes = null }) { Text("Clear time") }
                        }
                    }
                }
            }

            Text("Audience", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !membersOnly, onClick = { membersOnly = false },
                    label = { Text("Public") }, enabled = enabled)
                FilterChip(selected = membersOnly, onClick = { membersOnly = true },
                    label = { Text("Members only") }, enabled = enabled)
            }
        }
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
            store.postError?.let { com.weissev.stagecall.ui.components.ErrorNotice(it) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(enabled = !store.creatingPost, onClick = onCancel) { Text("Cancel") }
                Button(enabled = enabled && title.isNotBlank() && message.isNotBlank(), onClick = {
                    scope.launch {
                        val attachment = photo
                        val schedule = legacySchedule ?: listOfNotNull(date, time).joinToString(" · ")
                        val postId = store.savePost(organization.id, title, message, category, schedule,
                            membersOnly, attachment?.bytes, attachment?.contentType, post?.id, removePhoto)
                        if (postId != null) onPublished(postId)
                    }
                }) {
                    Text(when {
                        post == null -> if (store.creatingPost) "Publishing…" else "Publish"
                        store.creatingPost -> "Saving…"
                        else -> "Save changes"
                    })
                }
            }
        }
    }

    if (choosingDate) {
        val datePicker = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { choosingDate = false },
            confirmButton = {
                TextButton(enabled = datePicker.selectedDateMillis != null, onClick = {
                    dateMillis = datePicker.selectedDateMillis
                    legacySchedule = null
                    choosingDate = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = { choosingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePicker) }
    }

    if (choosingTime) {
        val initialTime = timeMinutes ?: 9 * 60
        val timePicker = rememberTimePickerState(
            initialHour = initialTime / 60,
            initialMinute = initialTime % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { choosingTime = false },
            title = { Text("Choose time") },
            text = { TimePicker(state = timePicker, layoutType = TimePickerLayoutType.Vertical) },
            confirmButton = {
                TextButton(onClick = {
                    timeMinutes = timePicker.hour * 60 + timePicker.minute
                    legacySchedule = null
                    choosingTime = false
                }) { Text("Set time") }
            },
            dismissButton = { TextButton(onClick = { choosingTime = false }) { Text("Cancel") } },
        )
    }
}

private data class PostSchedule(
    val dateMillis: Long? = null,
    val timeMinutes: Int? = null,
    val legacyText: String? = null,
)

private val PostDatePattern = Regex("""\d{4}-\d{2}-\d{2}""")
private val PostTimePattern = Regex("""(1[0-2]|[1-9]):([0-5]\d) (AM|PM)""")

private fun parsePostSchedule(schedule: String): PostSchedule {
    if (schedule.isBlank()) return PostSchedule()
    val parts = schedule.split(" · ")
    val dateText = parts.firstOrNull { PostDatePattern.matches(it) }
    val timeMatch = parts.mapNotNull(PostTimePattern::matchEntire).firstOrNull()
    if (parts != listOfNotNull(dateText, timeMatch?.value)) return PostSchedule(legacyText = schedule)

    val dateMillis = dateText?.let {
        runCatching { Instant.parse("${it}T00:00:00Z").toEpochMilliseconds() }.getOrNull()
            ?: return PostSchedule(legacyText = schedule)
    }
    val timeMinutes = timeMatch?.destructured?.let { (hours, minutes, period) ->
        (hours.toInt() % 12 + if (period == "PM") 12 else 0) * 60 + minutes.toInt()
    }
    return PostSchedule(dateMillis, timeMinutes)
}
