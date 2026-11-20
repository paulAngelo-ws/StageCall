package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.remote.LocalMediaCache
import com.weissev.stagecall.data.remote.OrganizationMemberDetails
import com.weissev.stagecall.data.remote.OrganizationMemberProfile
import com.weissev.stagecall.data.remote.OrganizationRoleStore
import com.weissev.stagecall.ui.components.ImageLightbox
import com.weissev.stagecall.ui.components.StageCallCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun OrganizationMembersPage(
    organizationId: String,
    organizationName: String,
    selectedMemberId: String?,
    viewerId: String,
    store: OrganizationRoleStore,
    onOpenMember: (String) -> Unit,
    onMembershipChanged: () -> Unit,
) {
    var members by remember(organizationId) { mutableStateOf<List<OrganizationMemberProfile>>(emptyList()) }
    var loading by remember(organizationId) { mutableStateOf(true) }
    var loadError by remember(organizationId) { mutableStateOf(false) }
    var refresh by remember(organizationId) { mutableStateOf(0) }
    var memberToRemove by remember(organizationId) { mutableStateOf<OrganizationMemberProfile?>(null) }
    var removing by remember(organizationId) { mutableStateOf(false) }
    var removalError by remember(organizationId) { mutableStateOf(false) }
    var confirmingLeave by remember(organizationId) { mutableStateOf(false) }
    var leaving by remember(organizationId) { mutableStateOf(false) }
    var leaveError by remember(organizationId) { mutableStateOf(false) }
    val listState = key(organizationId) { rememberLazyListState() }
    val scope = rememberCoroutineScope()
    val isPresident = store.loaded && store.roles[organizationId] == "President"

    LaunchedEffect(store, organizationId, refresh) {
        loading = true
        loadError = false
        try {
            members = store.loadMembers(organizationId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadError = true
        } finally {
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selectedMemberId == null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Members", style = MaterialTheme.typography.headlineSmall)
                    Text(organizationName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (store.loaded && organizationId in store.roles) {
                    TextButton(
                        onClick = {
                            leaveError = false
                            confirmingLeave = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Leave organization") }
                }
            }
        }
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            loadError -> {
                Text("Could not load organization members.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { refresh++ }) { Text("Retry") }
            }
            selectedMemberId != null -> {
                val member = members.firstOrNull { it.userId == selectedMemberId }
                if (member == null) Text("This member is no longer in the organization.")
                else MemberProfile(member, organizationId, organizationName, viewerId, store)
            }
            members.isEmpty() -> Text("This organization has no members yet.")
            else -> LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(members, key = { it.userId }) { member ->
                    StageCallCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                Modifier.weight(1f).clickable(
                                    role = Role.Button,
                                    onClickLabel = "View ${member.name}'s profile",
                                    onClick = { onOpenMember(member.userId) },
                                ).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MemberAvatar(member.picture, size = 40.dp)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(member.name, style = MaterialTheme.typography.titleMedium)
                                    Text(member.role, style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                            }
                            if (isPresident && member.role == "Member") {
                                IconButton(onClick = {
                                    removalError = false
                                    memberToRemove = member
                                }) {
                                    Icon(
                                        Icons.Outlined.PersonRemove,
                                        contentDescription = "Remove ${member.name}",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmingLeave) {
        AlertDialog(
            onDismissRequest = { if (!leaving) confirmingLeave = false },
            title = { Text("Leave organization?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to leave $organizationName?")
                    Text(if (isPresident) "You’ll lose president access and access to members-only posts."
                        else "You’ll lose access to members-only posts.")
                    if (leaveError) {
                        Text("Could not leave the organization. Please try again.",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !leaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        leaving = true
                        leaveError = false
                        scope.launch {
                            try {
                                store.leaveOrganization(organizationId)
                                confirmingLeave = false
                                refresh++
                                onMembershipChanged()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                leaveError = true
                            } finally {
                                leaving = false
                            }
                        }
                    },
                ) { Text(if (leaving) "Leaving…" else "Leave") }
            },
            dismissButton = {
                TextButton(enabled = !leaving, onClick = { confirmingLeave = false }) { Text("Cancel") }
            },
        )
    }

    memberToRemove?.let { member ->
        if (isPresident || removing) {
            AlertDialog(
                onDismissRequest = { if (!removing) memberToRemove = null },
                title = { Text("Remove ${member.name}?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${member.name} will lose access to members-only posts in $organizationName.")
                        if (removalError) {
                            Text("Could not remove this member. Check your connection and president access.",
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = isPresident && !removing, onClick = {
                        removing = true
                        removalError = false
                        scope.launch {
                            try {
                                store.removeMember(organizationId, member.userId)
                                memberToRemove = null
                                refresh++
                                onMembershipChanged()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                removalError = true
                            } finally {
                                removing = false
                            }
                        }
                    }) { Text(if (removing) "Removing…" else "Remove") }
                },
                dismissButton = {
                    TextButton(enabled = !removing, onClick = { memberToRemove = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun MemberProfile(
    member: OrganizationMemberProfile,
    organizationId: String,
    organizationName: String,
    viewerId: String,
    store: OrganizationRoleStore,
) {
    var retry by remember(organizationId, member.userId) { mutableStateOf(0) }
    val result by key(organizationId, member.userId, retry) {
        produceState<Result<OrganizationMemberDetails?>?>(null, store) {
            value = try {
                Result.success(store.loadMemberDetails(organizationId, member.userId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MemberAvatar(member.picture, size = 88.dp, canEnlarge = true)
        Text(member.name, style = MaterialTheme.typography.headlineSmall)
        Text("${member.role} · $organizationName")
        Text("Joined ${member.joinedAt.toString().substringBefore('T')}",
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        val loaded = result
        val details = loaded?.getOrNull()
        when {
            loaded == null -> CircularProgressIndicator()
            loaded.isFailure -> {
                Text("Could not load member details.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { retry++ }) { Text("Retry") }
            }
            details == null -> Text("This member is no longer in the organization.")
            else -> {
                Text(details.email?.takeIf { it.isNotBlank() } ?: "Email unavailable")
                if (member.userId != viewerId) {
                    StageCallCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Organizations in common", style = MaterialTheme.typography.titleMedium)
                            if (details.sharedOrganizations.isEmpty()) Text("No shared organizations.")
                            details.sharedOrganizations.forEach { Text(it) }
                        }
                    }
                }
            }
        }
        if (member.bio.isNotBlank()) {
            StageCallCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bio", style = MaterialTheme.typography.titleMedium)
                    Text(member.bio)
                }
            }
        }
    }
}

@Composable
private fun MemberAvatar(url: String, size: Dp, canEnlarge: Boolean = false) {
    val media = LocalMediaCache.current
    val picture by produceState<ImageBitmap?>(null, media, url) {
        value = null
        if (media != null && url.startsWith("https://lh3.googleusercontent.com/")) {
            val cached = media.cachedAvatar(url)
            value = cached
            try {
                value = media.avatar(url, cached)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the cached photo when the network is unavailable.
            }
        }
    }
    var viewingPicture by remember(url) { mutableStateOf(false) }
    val photo = picture
    if (viewingPicture && photo != null) ImageLightbox(photo, onClose = { viewingPicture = false })

    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photo == null) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = null, Modifier.size(size * 0.55f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            val imageModifier = Modifier.fillMaxSize()
            Image(
                photo,
                contentDescription = if (canEnlarge) "View profile photo" else null,
                modifier = if (canEnlarge) imageModifier.clickable(role = Role.Button) { viewingPicture = true } else imageModifier,
                contentScale = ContentScale.Crop,
            )
        }
    }
}
