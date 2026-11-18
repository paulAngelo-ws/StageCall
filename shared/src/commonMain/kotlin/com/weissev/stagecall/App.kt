package com.weissev.stagecall

import androidx.compose.foundation.layout.Box
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
    val repository = remember { StageCallRepository() }
    val cache = rememberRepositoryCache()
    val remote = remember { if (SupabaseConfig.isConfigured) SupabaseStateStore() else null }
    val persistence = remember(repository, cache, remote) { RepositoryPersistence(repository, cache, remote) }
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
                Screen.Apply -> Screen.Organization
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
                            if (page != Screen.Main) {
                                TextButton(onClick = { goBack() }) { Text("Back") }
                            }
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
                                    id = organizationId,
                                    initialPostId = organizationPostId,
                                    onApply = { page = Screen.Apply },
                                )
                            Screen.Apply ->
                                ApplyPage(
                                    repository = repository,
                                    id = organizationId,
                                    onSubmitted = { id ->
                                        applicationId = id
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
                                        ApplicationsPage(
                                            repository = repository,
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
