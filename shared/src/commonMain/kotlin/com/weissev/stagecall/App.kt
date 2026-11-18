package com.weissev.stagecall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.NavigationBar
import com.weissev.stagecall.components.StageCallTheme
import com.weissev.stagecall.components.TopHeader
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.Student
import com.weissev.stagecall.data.ApplicationStore
import com.weissev.stagecall.data.OrganizationRoleStore
import com.weissev.stagecall.screens.ReviewApplicationsPage
import kotlinx.coroutines.delay
import com.weissev.stagecall.data.RepositoryPersistence
import com.weissev.stagecall.data.SupabaseConfig
import com.weissev.stagecall.data.SupabaseStateStore
import com.weissev.stagecall.data.rememberRepositoryCache
import com.weissev.stagecall.screens.ApplicationPage
import com.weissev.stagecall.screens.ApplicationsPage
import com.weissev.stagecall.screens.ApplicationHistoryPage
import com.weissev.stagecall.screens.ApplyPage
import com.weissev.stagecall.screens.DirectoryPage
import com.weissev.stagecall.screens.FeedPage
import com.weissev.stagecall.screens.OrganizationPage
import com.weissev.stagecall.screens.ProfilePage
import com.weissev.stagecall.screens.NotificationsPage

// Named pages prevent spelling mistakes when navigating between screens.
private enum class Screen {
    Main,
    Organization,
    Apply,
    Status,
    Notifications,
    ApplicationHistory,
    ReviewApplications,
}

private data class NotificationOrigin(
    val page: Screen = Screen.Main,
    val organizationId: String = "",
    val postId: Int? = null,
    val applicationId: Int = 0,
)

/** One shared entry point for Android, iOS, desktop and web. */
@Composable
fun App() {
    val client = AuthSession.client
    if (client == null) {
        StageCallTheme { LoginScreen("Supabase is not configured. Check local.properties and rebuild.", false) {} }
        return
    }
    val status by client.auth.sessionStatus.collectAsState()
    val scope = rememberCoroutineScope()
    var authError by remember { mutableStateOf<String?>(null) }
    when (status) {
        is SessionStatus.Initializing -> {
            StageCallTheme { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            return
        }
        is SessionStatus.Authenticated -> Unit
        else -> {
            StageCallTheme {
                LoginScreen(authError, true) {
                    scope.launch {
                        authError = null
                        try { AuthSession.signIn() } catch (e: Exception) { authError = e.message ?: "Could not start Google sign-in. Please try again." }
                    }
                }
            }
            return
        }
    }
    val user = client.auth.currentUserOrNull()
    val userId = user?.id.orEmpty()
    val accountEmail = user?.email.orEmpty()
    val accountName = user?.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull.orEmpty()
    val accountPicture = user?.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
        ?: user?.userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull.orEmpty()
    val profileStore = remember(client, userId) { com.weissev.stagecall.data.ProfileStore(client, userId) }
    LaunchedEffect(profileStore) { profileStore.load() }
    val repository = remember(userId) { StageCallRepository() }
    val cache = rememberRepositoryCache()
    val remote = remember { if (SupabaseConfig.isConfigured) SupabaseStateStore() else null }
    val persistence = remember(repository, cache, remote) {
        RepositoryPersistence(repository, cache, remote).also {
            repository.setAccount(Student(accountName.ifBlank { accountEmail }, accountEmail, userId))
        }
    }
    val applicationStore = remember(client, repository) { ApplicationStore(client, repository) }
    val roleStore = remember(client, repository) { OrganizationRoleStore(client, repository) }
    LaunchedEffect(roleStore) {
        while (true) {
            roleStore.refreshRoles()
            delay(30_000)
        }
    }
    LaunchedEffect(applicationStore) {
        while (true) {
            applicationStore.refresh()
            delay(30_000)
        }
    }
    LaunchedEffect(persistence) {
        try {
            persistence.run()
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { remote?.close() }
        }
    }
    val themeSettings = rememberThemeSettings()
    var selectedTab by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(Screen.Main) }
    var organizationId by remember { mutableStateOf("") }
    var organizationPostId by remember { mutableStateOf<Int?>(null) }
    var applicationId by remember { mutableStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var notificationOrigin by remember { mutableStateOf(NotificationOrigin()) }
    var notificationDetailOpen by remember { mutableStateOf(false) }
    fun openOrganization(id: String) {
        organizationId = id
        organizationPostId = null
        page = Screen.Organization
    }

    fun goBack() {
        if (page == Screen.Notifications) {
            organizationId = notificationOrigin.organizationId
            organizationPostId = notificationOrigin.postId
            applicationId = notificationOrigin.applicationId
            page = notificationOrigin.page
            notificationDetailOpen = false
            return
        }
        if (notificationDetailOpen && (page == Screen.Organization || page == Screen.Status)) {
            page = Screen.Notifications
            notificationDetailOpen = false
            return
        }
        page =
            when (page) {
                Screen.Apply, Screen.ReviewApplications -> Screen.Organization
                else -> Screen.Main
            }
    }

    StageCallTheme(darkTheme = themeSettings.isDarkTheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Scaffold(
                    Modifier.widthIn(max = 640.dp).fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        Column(Modifier.statusBarsPadding()) {
                            TopHeader(
                                unreadCount = repository.unreadNotificationCount,
                                onSearchClick = {
                                    selectedTab = 1
                                    page = Screen.Main
                                    notificationDetailOpen = false
                                },
                            ) {
                                if (page != Screen.Notifications && !notificationDetailOpen) {
                                    notificationOrigin = NotificationOrigin(page, organizationId, organizationPostId, applicationId)
                                }
                                notificationDetailOpen = false
                                page = Screen.Notifications
                            }
                            HorizontalDivider()
                            if (page != Screen.Main) {
                                TextButton(
                                    onClick = { goBack() },
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                                ) {
                                    Text("Back")
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 8.dp).size(18.dp),
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            NavigationBar(selectedTab) {
                                selectedTab = it
                                page = Screen.Main
                                notificationDetailOpen = false
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        if (!persistence.hasData) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            ) {
                                Text("No database data loaded", style = MaterialTheme.typography.titleMedium)
                                Text(persistence.status)
                                TextButton(onClick = persistence::retry) { Text("Retry connection") }
                            }
                        } else when (page) {
                            Screen.Notifications -> NotificationsPage(
                                repository = repository,
                                onOpenNotification = { notification ->
                                    notificationDetailOpen = true
                                    if (notification.applicationId != null) {
                                        applicationId = notification.applicationId
                                        page = Screen.Status
                                    } else {
                                        organizationId = notification.organizationId
                                        organizationPostId = notification.postId
                                        page = Screen.Organization
                                    }
                                },
                                onOpenDirectory = {
                                    selectedTab = 1
                                    page = Screen.Main
                                    notificationDetailOpen = false
                                },
                            )
                            Screen.Organization ->
                                OrganizationPage(
                                    repository = repository,
                                    applicationsReady = applicationStore.loaded,
                                    rolesReady = roleStore.loaded,
                                    roleError = roleStore.error,
                                    isPresident = roleStore.roles[organizationId] == "President",
                                    onRetryRoles = { scope.launch { roleStore.refreshRoles() } },
                                    onReview = { page = Screen.ReviewApplications },
                                    id = organizationId,
                                    initialPostId = organizationPostId,
                                    onApply = { page = Screen.Apply },
                                )
                            Screen.ReviewApplications -> if (roleStore.loaded && roleStore.roles[organizationId] == "President") {
                                ReviewApplicationsPage(
                                    store = roleStore,
                                    organizationId = organizationId,
                                    organizationName = repository.organizations.firstOrNull { it.id == organizationId }?.name.orEmpty(),
                                )
                            } else {
                                Column(Modifier.padding(24.dp)) {
                                    Text(roleStore.error ?: "You do not have permission to review applications for this organization.")
                                    if (roleStore.error != null) TextButton(onClick = { scope.launch { roleStore.refreshRoles() } }) { Text("Retry") }
                                }
                            }
                            Screen.Apply ->
                                ApplyPage(
                                    repository = repository,
                                    id = organizationId,
                                    store = applicationStore,
                                    onSubmitted = { id ->
                                        applicationId = id
                                        selectedTab = 2
                                        notificationDetailOpen = false
                                        page = Screen.Status
                                    },
                                )
                            Screen.ApplicationHistory -> ApplicationHistoryPage(
                                repository = repository,
                                onOpenApplication = { id ->
                                    applicationId = id
                                    page = Screen.Status
                                },
                                onOpenDirectory = {
                                    selectedTab = 1
                                    page = Screen.Main
                                },
                                onOpenOrganization = { id -> openOrganization(id) },
                            )
                            Screen.Status -> ApplicationPage(repository = repository, id = applicationId)
                            Screen.Main ->
                                when (selectedTab) {
                                    0 -> FeedPage(
                                        repository = repository,
                                        onOpenOrganization = { id -> openOrganization(id) },
                                        onOpenPost = { id, postId ->
                                            organizationId = id
                                            organizationPostId = postId
                                            page = Screen.Organization
                                        },
                                    )
                                    1 ->
                                        DirectoryPage(
                                            repository = repository,
                                            onOpenOrganization = { id -> openOrganization(id) },
                                        )
                                    2 ->
                                        if (!applicationStore.loaded) {
                                            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                                                Text(applicationStore.error ?: "Loading applications...")
                                                if (applicationStore.error != null) TextButton(onClick = { scope.launch { applicationStore.refresh() } }) { Text("Retry") }
                                            }
                                        } else ApplicationsPage(
                                            repository = repository,
                                            refreshError = applicationStore.error,
                                            onRetry = { scope.launch { applicationStore.refresh() } },
                                            onOpenHistory = { page = Screen.ApplicationHistory },
                                            onOpenApplication = { id ->
                                                applicationId = id
                                                page = Screen.Status
                                            },
                                            onOpenDirectory = { selectedTab = 1 },
                                            onOpenOrganization = { id -> openOrganization(id) },
                                        )
                                    3 ->
                                        ProfilePage(
                                            repository = repository,
                                            accountEmail = accountEmail,
                                            accountName = accountName,
                                            accountPicture = accountPicture,
                                            profileStore = profileStore,
                                            onSignOut = { scope.launch { try { AuthSession.signOut() } catch (e: Exception) { dialog = e.message ?: "Could not sign out." } } },
                                            darkTheme = themeSettings.isDarkTheme,
                                            onDarkThemeChange = themeSettings::updateDarkTheme,
                                            onOpenOrganization = { id -> openOrganization(id) },
                                            onOpenDirectory = { selectedTab = 1 },
                                            onOpenApplications = { selectedTab = 2 },
                                        )
                                }
                        }
                    }
                }
            }
        }
        if (dialog != null) {
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("StageCall") },
                text = { Text(dialog.orEmpty()) },
                confirmButton = { TextButton(onClick = { dialog = null }) { Text("Close") } },
            )
        }
    }
}


@Composable
private fun LoginScreen(error: String?, enabled: Boolean, onSignIn: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("StageCall", style = MaterialTheme.typography.headlineLarge)
            Text("Discover your campus community", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onSignIn, enabled = enabled) { Text("Continue with Google") }
            if (error != null) Text(error, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error)
        }
    }
}
