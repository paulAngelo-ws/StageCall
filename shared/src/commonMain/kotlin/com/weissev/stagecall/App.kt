package com.weissev.stagecall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TheaterComedy
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.components.NavigationBar
import com.weissev.stagecall.components.StageCallTheme
import com.weissev.stagecall.components.TopHeader
import com.weissev.stagecall.components.StatusPanel
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.Student
import com.weissev.stagecall.data.ApplicationStore
import com.weissev.stagecall.data.OrganizationRoleStore
import com.weissev.stagecall.data.NotificationStore
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
    var signingIn by remember { mutableStateOf(false) }
    when (status) {
        is SessionStatus.Initializing -> {
            StageCallTheme { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            return
        }
        is SessionStatus.Authenticated -> Unit
        else -> {
            StageCallTheme {
                LoginScreen(authError, !signingIn, signingIn) {
                    scope.launch {
                        authError = null
                        signingIn = true
                        try { AuthSession.signIn() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { authError = e.message ?: "Could not start Google sign-in. Please try again." }
                        finally { signingIn = false }
                    }
                }
            }
            return
        }
    }
    val user = client.auth.currentUserOrNull()
    val userId = user?.id.orEmpty()
    val accountEmail = user?.email.orEmpty()
    val emailDomain = accountEmail.substringAfter('@', "").lowercase()
    if (accountEmail.count { it == '@' } != 1 || accountEmail.substringBefore('@').isEmpty() ||
        !(emailDomain == "ubaguio.edu" || emailDomain.endsWith(".ubaguio.edu"))) {
        StageCallTheme {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Please sign in with your University of Baguio Google account.", textAlign = TextAlign.Center)
                TextButton(onClick = { scope.launch { try { AuthSession.signOut() } catch (e: Exception) { authError = e.message ?: "Could not sign out. Please try again." } } }) { Text("Sign out") }
                authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        return
    }
    val accountName = user?.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull.orEmpty()
    val accountPicture = user?.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
        ?: user?.userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull.orEmpty()
    val profileStore = remember(client, userId) { com.weissev.stagecall.data.ProfileStore(client, userId) }
    LaunchedEffect(profileStore) { profileStore.load() }
    if (!profileStore.loaded) {
        StageCallTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (profileStore.loading || profileStore.error == null) {
                    StatusPanel("Loading your profile…", loading = true)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusPanel(profileStore.error!!, isError = true, actionLabel = "Retry", onAction = { scope.launch { profileStore.load() } })
                        TextButton(onClick = { scope.launch { AuthSession.signOut() } }) { Text("Sign out") }
                    }
                }
            }
        }
        return
    }
    val snackbarHost = remember(userId) { SnackbarHostState() }
    fun showSuccess(message: String) { scope.launch { snackbarHost.showSnackbar(message) } }
    var preferenceError by remember(userId) { mutableStateOf<String?>(null) }
    var darkTheme by remember(userId) { mutableStateOf(profileStore.darkTheme) }
    val repository = remember(userId) { StageCallRepository() }
    val cache = rememberRepositoryCache()
    val remote = remember(userId) { if (SupabaseConfig.isConfigured) SupabaseStateStore() else null }
    val persistence = remember(repository, cache, remote) {
        RepositoryPersistence(repository, cache, remote).also {
            repository.setAccount(Student(accountName.ifBlank { accountEmail }, accountEmail, userId))
        }
    }
    remember(persistence, profileStore) {
        repository.bookmarks.clear()
        repository.bookmarks.addAll(profileStore.bookmarks)
        repository.onBookmarksChanged = {
            val requested = repository.bookmarks.toList()
            scope.launch {
                if (!profileStore.saveBookmarks(requested)) {
                    if (repository.bookmarks.toList() == requested) {
                        repository.bookmarks.clear()
                        repository.bookmarks.addAll(profileStore.bookmarks)
                    }
                    preferenceError = "Could not save bookmarks. Check your connection and try again."
                }
            }
        }
    }
    val applicationStore = remember(client, repository) { ApplicationStore(client, repository) }
    val roleStore = remember(client, repository) { OrganizationRoleStore(client, repository) }
    val notificationStore = remember(client, userId) { NotificationStore(client) }
    LaunchedEffect(notificationStore) {
        while (true) {
            notificationStore.refresh()
            delay(10_000)
        }
    }
    LaunchedEffect(roleStore) {
        while (true) {
            roleStore.refreshRoles()
            if (roleStore.loaded) roleStore.refreshMemberPosts()
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

    StageCallTheme(darkTheme = darkTheme) {
        preferenceError?.let { error ->
            AlertDialog(onDismissRequest = { preferenceError = null }, title = { Text("Could not save preference") },
                text = { Text(error) }, confirmButton = { TextButton(onClick = { preferenceError = null }) { Text("OK") } })
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Scaffold(
                    Modifier.widthIn(max = 640.dp).fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHost) },
                    topBar = {
                        Column(Modifier.statusBarsPadding()) {
                            TopHeader(
                                unreadCount = notificationStore.unreadCount,
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
                            if (persistence.hasData && persistence.refreshFailed) {
                                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Connection lost. Showing previously loaded data.", Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = persistence::retry, enabled = !persistence.loading) { Text(if (persistence.loading) "Retrying…" else "Retry") }
                                }
                            }
                            if (page != Screen.Main) {
                                TextButton(
                                    onClick = { goBack() },
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp).size(18.dp),
                                    )
                                    Text("Back")
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Column {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            NavigationBar(if (page == Screen.ReviewApplications) 1 else selectedTab) {
                                selectedTab = it
                                page = Screen.Main
                                notificationDetailOpen = false
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        if (!persistence.hasData) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (persistence.loading) {
                                    StatusPanel("Loading campus data…", loading = true)
                                } else {
                                    StatusPanel(persistence.status, isError = true, actionLabel = "Retry connection", onAction = persistence::retry)
                                }
                            }
                        } else when (page) {
                            Screen.Notifications -> NotificationsPage(
                                repository = repository,
                                store = notificationStore,
                                onOpenNotification = { notification ->
                                    notificationDetailOpen = true
                                    if (notification.applicationId != null) {
                                        applicationId = notification.applicationId
                                        page = Screen.Status
                                    } else {
                                        scope.launch {
                                            if (notification.postId != null) {
                                                try { persistence.synchronize(); roleStore.refreshMemberPosts() }
                                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                                catch (_: Exception) { persistence.retry() }
                                            }
                                            organizationId = notification.organizationId
                                            organizationPostId = notification.postId
                                            page = Screen.Organization
                                        }
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
                                    roleStore = roleStore,
                                    onSettingsSaved = { showSuccess("Organization settings saved") },
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
                                    bannerColor = repository.organizations.firstOrNull { it.id == organizationId }?.bannerColor ?: "blue",
                                    onDecisionSaved = { accepted -> showSuccess(if (accepted) "Application accepted" else "Application rejected") },
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
                                        showSuccess("Application submitted")
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
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                val error = applicationStore.error
                                                if (error == null) StatusPanel("Loading applications…", loading = true)
                                                else StatusPanel(error, isError = true, actionLabel = "Retry", onAction = { scope.launch { applicationStore.refresh() } })
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
                                            onSaved = { showSuccess("Profile saved") },
                                            onSignOut = { scope.launch { try { AuthSession.signOut() } catch (e: Exception) { dialog = e.message ?: "Could not sign out." } } },
                                            darkTheme = darkTheme,
                                            onDarkThemeChange = { requested ->
                                                darkTheme = requested
                                                scope.launch {
                                                    if (!profileStore.saveTheme(requested)) {
                                                        if (darkTheme == requested) darkTheme = profileStore.darkTheme
                                                        preferenceError = "Could not save your theme. Check your connection and try again."
                                                    }
                                                }
                                            },
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
private fun LoginScreen(error: String?, enabled: Boolean, signingIn: Boolean = false, onSignIn: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFE7F3FF), Color(0xFFF8FBFF), Color.White)))
            val unit = minOf(size.width, size.height)
            drawCircle(Color(0xFF8DC8F4).copy(alpha = 0.22f), radius = unit * 0.44f, center = Offset(size.width * 0.94f, size.height * 0.05f))
            drawCircle(Color(0xFF6CAEE5).copy(alpha = 0.18f), radius = unit * 0.28f, center = Offset(size.width * 0.04f, size.height * 0.78f))
            drawCircle(Color(0xFF70B5ED).copy(alpha = 0.24f), radius = unit * 0.16f, center = Offset(size.width * 0.10f, size.height * 0.16f), style = Stroke(width = unit * 0.008f))
            drawCircle(Color(0xFF72B5E9).copy(alpha = 0.22f), radius = unit * 0.09f, center = Offset(size.width * 0.88f, size.height * 0.81f), style = Stroke(width = unit * 0.006f))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.TheaterComedy, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp).size(96.dp), tint = MaterialTheme.colorScheme.primary)
            Text("StageCall", style = MaterialTheme.typography.headlineLarge)
            Text("Discover your campus community", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Text("Use your University of Baguio Google account (for example, name@s.ubaguio.edu).", modifier = Modifier.padding(bottom = 16.dp), textAlign = TextAlign.Center)
            OutlinedButton(onClick = onSignIn, enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F))) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val googlePaths = remember {
                        listOf(
                            Color(0xFF4285F4) to "M21.35,12.25c0,-0.68 -0.06,-1.18 -0.19,-1.7H12v3.09h5.38c-0.11,0.77 -0.69,1.93 -1.98,2.71l-0.02,0.1 2.9,2.25 0.2,0.02c1.83,-1.69 2.87,-4.17 2.87,-6.47z",
                            Color(0xFF34A853) to "M12,21.75c2.62,0 4.83,-0.86 6.44,-2.35l-3.08,-2.37c-0.83,0.57 -1.94,0.97 -3.36,0.97 -2.57,0 -4.75,-1.69 -5.53,-4.02l-0.1,0.01 -3.02,2.34 -0.04,0.1A9.73,9.73 0,0 0,12 21.75z",
                            Color(0xFFFBBC05) to "M6.47,13.98a5.99,5.99 0,0 1,0 -3.96l-0.01,-0.11 -3.06,-2.38 -0.1,0.05a9.75,9.75 0,0 0,0 8.85z",
                            Color(0xFFEA4335) to "M12,6c1.82,0 3.05,0.79 3.75,1.45l2.74,-2.67A9.35,9.35 0,0 0,12 2.25a9.73,9.73 0,0 0,-8.7 5.33l3.17,2.44C7.25,7.69 9.43,6 12,6z",
                        ).map { (color, path) -> color to PathParser().parsePathString(path).toPath() }
                    }
                    Canvas(Modifier.size(20.dp)) {
                        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                            googlePaths.forEach { (color, path) -> drawPath(path, color) }
                        }
                    }
                    Text(if (signingIn) "Opening Google…" else "Continue with Google")
                }
            }
            if (error != null) Text(error, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error)
        }
        }
    }
}
