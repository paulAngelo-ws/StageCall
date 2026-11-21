package com.weissev.stagecall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.NotificationType
import com.weissev.stagecall.data.RepositoryPersistence
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.data.Student
import com.weissev.stagecall.data.auth.AuthSession
import com.weissev.stagecall.data.rememberRepositoryCache
import com.weissev.stagecall.data.rememberCacheStorage
import com.weissev.stagecall.data.remote.ApplicationStore
import com.weissev.stagecall.data.remote.NotificationStore
import com.weissev.stagecall.data.remote.LocalMediaCache
import com.weissev.stagecall.data.remote.MediaCache
import com.weissev.stagecall.data.remote.OrganizationRegistrationStore
import com.weissev.stagecall.data.remote.OrganizationRoleStore
import com.weissev.stagecall.data.remote.ProfileStore
import com.weissev.stagecall.data.remote.cacheProjectKey
import com.weissev.stagecall.data.remote.defaultStudentNumber
import com.weissev.stagecall.data.remote.rememberCollectionCache
import com.weissev.stagecall.data.remote.SupabaseStateStore
import com.weissev.stagecall.ui.applications.ApplicationHistoryPage
import com.weissev.stagecall.ui.applications.ApplicationPage
import com.weissev.stagecall.ui.applications.ApplicationsPage
import com.weissev.stagecall.ui.applications.ApplyPage
import com.weissev.stagecall.ui.applications.OrganizationRegistrationDetailsPage
import com.weissev.stagecall.ui.applications.ReviewApplicationsPage
import com.weissev.stagecall.ui.auth.LoginScreen
import com.weissev.stagecall.ui.auth.UniversityAccountRequiredScreen
import com.weissev.stagecall.ui.components.NavigationBar
import com.weissev.stagecall.ui.components.StageCallLoadingScreen
import com.weissev.stagecall.ui.components.StatusPanel
import com.weissev.stagecall.ui.components.TopHeader
import com.weissev.stagecall.ui.feed.FeedPage
import com.weissev.stagecall.ui.notifications.NotificationsPage
import com.weissev.stagecall.ui.organizations.DirectoryPage
import com.weissev.stagecall.ui.organizations.OrganizationMembersPage
import com.weissev.stagecall.ui.organizations.CreateOrganizationPostPage
import com.weissev.stagecall.ui.organizations.EditOrganizationPage
import com.weissev.stagecall.ui.organizations.OrganizationPage
import com.weissev.stagecall.ui.profile.ProfilePage
import com.weissev.stagecall.ui.registrations.OrganizationRegistrationPage
import com.weissev.stagecall.ui.registrations.OrganizationRegistrationReviewPage
import com.weissev.stagecall.ui.theme.StageCallTheme
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// Named pages prevent spelling mistakes when navigating between screens.
private enum class Screen {
    Main,
    Organization,
    EditOrganization,
    CreateOrganizationPost,
    OrganizationMembers,
    OrganizationMemberProfile,
    Apply,
    Status,
    Notifications,
    ApplicationHistory,
    ReviewApplications,
    OrganizationRegistration,
    OrganizationRegistrationDetails,
    OrganizationRegistrationReview,
}

private data class Route(
    val page: Screen = Screen.Main,
    val tab: Int = 0,
    val organizationId: String = "",
    val organizationPostId: Int? = null,
    val editingPostId: Int? = null,
    val memberId: String? = null,
    val applicationId: Int = 0,
    val registrationId: Int = 0,
)

/** One shared entry point for Android, iOS, desktop and web. */
@Composable
fun App(systemBackHandler: @Composable (onBack: () -> Unit) -> Unit = {}) {
    // AuthSession.kt owns the Supabase client. LoginScreen.kt gets the sign-in callback and any error to display.
    val client = AuthSession.client
    if (client == null) {
        StageCallTheme { LoginScreen("Supabase is not configured. Check local.properties and rebuild.", false) {} }
        return
    }
    val status by client.auth.sessionStatus.collectAsState() // Let Compose redraw when AuthSession.kt reports a session change.
    val scope = rememberCoroutineScope()
    var authError by remember { mutableStateOf<String?>(null) }
    var signingIn by remember { mutableStateOf(false) }
    when (status) {
        is SessionStatus.Initializing -> {
            StageCallLoadingScreen("Restoring your session…") // StageCallLoadingScreen.kt displays this while sign-in state is being restored.
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
    if (userId.isBlank() || accountEmail.count { it == '@' } != 1 || accountEmail.substringBefore('@').isEmpty() ||
        !(emailDomain == "ubaguio.edu" || emailDomain.endsWith(".ubaguio.edu"))) {
        StageCallTheme {
            UniversityAccountRequiredScreen(error = authError) {
                scope.launch {
                    authError = null
                    try {
                        AuthSession.signOut()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        authError = e.message ?: "Could not sign out. Please try again."
                    }
                }
            }
        }
        return
    }
    val accountName = user?.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull.orEmpty()
    val accountPicture = user?.userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
        ?: user?.userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull.orEmpty()
    // Keep the screens, stores, and image cache tied to this account when someone switches users.
    key(userId) { // Start fresh screen state when another account signs in.
        val storage = rememberCacheStorage()
        val media = remember(client, storage) { MediaCache(client, userId, storage) }
        CompositionLocalProvider(LocalMediaCache provides media) {
            SignedInApp(client, userId, accountEmail, accountName, accountPicture, systemBackHandler)
        }
    }
}

@Composable
private fun SignedInApp(
    client: SupabaseClient,
    userId: String,
    accountEmail: String,
    accountName: String,
    accountPicture: String,
    systemBackHandler: @Composable (onBack: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // ProfileStore.kt exposes saved preferences first, then checks for changes using CollectionCache.kt.
    val profileCache = rememberCollectionCache(client, userId, "profile")
    val profileStore = remember(client, userId, profileCache) { ProfileStore(client, userId, profileCache) }
    LaunchedEffect(profileStore) { profileStore.load() } // Ask ProfileStore.kt for the latest profile once for this store.
    if (!profileStore.loaded) { // Wait here only if ProfileStore.kt has no usable profile yet.
        val error = profileStore.error
        if (profileStore.loading || error == null) {
            StageCallLoadingScreen("Loading your profile…")
        } else {
            StageCallTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusPanel(error, isError = true, actionLabel = "Retry", onAction = { scope.launch { profileStore.load() } })
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
    val darkTheme = profileStore.darkTheme
    val repository = remember(userId) { StageCallRepository() }
    val cache = rememberRepositoryCache("catalog-$cacheProjectKey")
    val organizations = rememberCollectionCache(client, userId, "organizations", shared = true)
    val publicPosts = rememberCollectionCache(client, userId, "public_posts", shared = true)
    // SupabaseStateStore.kt reads catalog rows. RepositoryPersistence.kt restores them into StageCallRepository.kt.
    val remote = remember(client, organizations, publicPosts) { SupabaseStateStore(client, organizations, publicPosts) }
    val persistence = remember(repository, cache, remote) {
        RepositoryPersistence(repository, cache, remote).also {
            repository.setAccount(Student(accountName.ifBlank { accountEmail }, accountEmail, userId))
        }
    }
    remember(persistence, profileStore) {
        repository.bookmarks.clear()
        repository.bookmarks.addAll(profileStore.bookmarks)
        repository.onBookmarksChanged = {
            val requested = repository.bookmarks.toList() // Copy the current bookmarks before sending them to ProfileStore.kt.
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
    // Apply preferences from ProfileStore.kt without replacing bookmark taps that are still being saved.
    var confirmedBookmarks by remember(repository) { mutableStateOf(profileStore.bookmarks) }
    LaunchedEffect(repository, profileStore.bookmarks) {
        val saved = profileStore.bookmarks
        if (saved != confirmedBookmarks && repository.bookmarks.toList() == confirmedBookmarks) { // Only apply server changes if there are no newer local taps to keep.
            repository.bookmarks.clear()
            repository.bookmarks.addAll(saved)
        }
        confirmedBookmarks = saved
    }
    val applications = rememberCollectionCache(client, userId, "applications")
    val memberships = rememberCollectionCache(client, userId, "memberships")
    val memberPosts = rememberCollectionCache(client, userId, "member_posts")
    val registrations = rememberCollectionCache(client, userId, "registrations")
    val notifications = rememberCollectionCache(client, userId, "notifications")
    // These stores give the screens observable data. ApplicationStore.kt and OrganizationRoleStore.kt also update StageCallRepository.kt.
    val applicationStore = remember(client, repository, applications) { ApplicationStore(client, repository, applications) }
    val roleStore = remember(client, repository, memberships, memberPosts) {
        OrganizationRoleStore(client, repository, memberships, memberPosts)
    }
    val registrationStore = remember(client, userId, registrations) { OrganizationRegistrationStore(client, userId, registrations) }
    val notificationStore = remember(client, notifications) { NotificationStore(client, notifications) }
    // The app starts background refreshes here. Each store publishes its results to the screens that read it.
    LaunchedEffect(notificationStore) {
        while (true) {
            notificationStore.refresh() // NotificationStore.kt updates the unread count and list.
            delay(10_000)
        }
    }
    LaunchedEffect(roleStore) {
        while (true) {
            roleStore.refreshRoles() // OrganizationRoleStore.kt checks this student’s current roles.
            if (roleStore.loaded) roleStore.refreshMemberPosts() // Check member posts only after the role check succeeds.
            delay(30_000)
        }
    }
    LaunchedEffect(applicationStore) {
        while (true) {
            applicationStore.refresh() // ApplicationStore.kt updates the requests displayed in ApplicationsScreen.kt.
            delay(30_000)
        }
    }
    LaunchedEffect(persistence) {
        try {
            persistence.run()
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { remote.close() }
        }
    }
    val backStack = remember { mutableStateListOf(Route()) }
    val route = backStack.last()
    val page = route.page
    val organizationId = route.organizationId
    val editingPostId = route.editingPostId
    val publishingPost = page == Screen.CreateOrganizationPost && roleStore.creatingPost
    val registrationPageOpen = page == Screen.OrganizationRegistration || page == Screen.OrganizationRegistrationDetails
    val registrationReviewOpen = page == Screen.OrganizationRegistrationReview
    LaunchedEffect(registrationStore, registrationPageOpen, registrationReviewOpen) {
        if (registrationPageOpen || registrationReviewOpen) {
            while (true) {
                registrationStore.refresh(reviewing = registrationReviewOpen)
                delay(15_000)
            }
        }
    }
    var dialog by remember { mutableStateOf<String?>(null) }
    fun navigate(destination: Route) {
        if (destination != backStack.last()) backStack.add(destination)
    }

    fun openTab(tab: Int) = navigate(Route(tab = tab))

    fun openOrganization(id: String, postId: Int? = null) {
        navigate(Route(Screen.Organization, tab = route.tab, organizationId = id, organizationPostId = postId))
    }

    fun goBack() {
        if (!publishingPost && backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    systemBackHandler { goBack() }

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
                                    if (publishingPost) return@TopHeader
                                    openTab(1)
                                },
                            ) {
                                if (publishingPost) return@TopHeader
                                navigate(Route(Screen.Notifications, tab = route.tab))
                            }
                            HorizontalDivider()
                            if (persistence.hasData && persistence.refreshFailed) {
                                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Connection lost. Showing previously loaded data.", Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = persistence::retry, enabled = !persistence.loading) { Text(if (persistence.loading) "Retrying…" else "Retry") }
                                }
                            }
                            if (backStack.size > 1) {
                                TextButton(
                                    onClick = { goBack() },
                                    enabled = !publishingPost,
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
                            NavigationBar(if (page == Screen.ReviewApplications) 1 else route.tab) {
                                if (publishingPost) return@NavigationBar
                                openTab(it)
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        if (!persistence.hasData) {
                            if (persistence.loading) {
                                StageCallLoadingScreen("Loading campus data…")
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    StatusPanel(persistence.status, isError = true, actionLabel = "Retry connection", onAction = persistence::retry)
                                }
                            }
                        } else when (page) {
                            // NotificationsScreen.kt reports the selected notification here. Its IDs lead to OrganizationScreen.kt or ApplicationStatusScreen.kt.
                            Screen.Notifications -> NotificationsPage(
                                repository = repository,
                                store = notificationStore,
                                onOpenNotification = { notification ->
                                    if (!notification.isRead) {
                                        scope.launch { notificationStore.markRead(notification.id) }
                                    }
                                    if (notification.type == NotificationType.RegistrationReview) {
                                        navigate(route.copy(page = Screen.OrganizationRegistrationReview))
                                    } else if (notification.type == NotificationType.RegistrationDecision && notification.registrationId != null) {
                                        navigate(route.copy(page = Screen.OrganizationRegistrationDetails, registrationId = notification.registrationId))
                                    } else if (notification.type == NotificationType.ApplicationReview) {
                                        navigate(route.copy(page = Screen.ReviewApplications, organizationId = notification.organizationId))
                                        scope.launch { roleStore.refreshRoles() }
                                    } else if (notification.applicationId != null) { // Choose the application details route when the notification has an application ID.
                                        navigate(route.copy(page = Screen.Status, applicationId = notification.applicationId))
                                    } else {
                                        scope.launch {
                                            if (notification.postId != null) {
                                                try { persistence.synchronize(); roleStore.refreshMemberPosts() }
                                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                                catch (_: Exception) { persistence.retry() }
                                            }
                                            if (backStack.last() === route) {
                                                openOrganization(notification.organizationId, notification.postId)
                                            }
                                        }
                                    }
                                },
                                onOpenDirectory = {
                                    openTab(1)
                                },
                            )
                            Screen.OrganizationRegistration -> OrganizationRegistrationPage(
                                store = registrationStore,
                                onSubmitted = { showSuccess("Organization registration submitted") },
                                onOpenAdminReview = {
                                    if (registrationStore.isAdmin) navigate(route.copy(page = Screen.OrganizationRegistrationReview))
                                },
                                onOpenRegistration = { id ->
                                    navigate(route.copy(page = Screen.OrganizationRegistrationDetails, registrationId = id))
                                },
                            )
                            Screen.OrganizationRegistrationDetails -> OrganizationRegistrationDetailsPage(
                                registration = registrationStore.own.firstOrNull { it.id == route.registrationId },
                                onOpenOrganization = { id ->
                                    try {
                                        if (repository.organizations.none { it.id == id }) persistence.synchronize()
                                        roleStore.refreshRoles()
                                        if (roleStore.loaded && repository.organizations.any { it.id == id }) {
                                            if (backStack.last() === route) openOrganization(id)
                                            true
                                        } else false
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        false
                                    }
                                },
                            )
                            Screen.OrganizationRegistrationReview -> {
                                if (registrationStore.isAdmin) {
                                    OrganizationRegistrationReviewPage(
                                        store = registrationStore,
                                        onDecisionSaved = { approved ->
                                            if (approved) persistence.retry()
                                            showSuccess(if (approved) "Organization approved" else "Registration rejected")
                                        },
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        StatusPanel(registrationStore.accessError ?: "Registration review requires admin access.")
                                    }
                                }
                            }
                            Screen.Organization ->
                                OrganizationPage(
                                    repository = repository,
                                    onCreatePost = {
                                        roleStore.clearPostError()
                                        navigate(route.copy(page = Screen.CreateOrganizationPost, editingPostId = null))
                                    },
                                    onEditPost = { postId ->
                                        roleStore.clearPostError()
                                        navigate(route.copy(page = Screen.CreateOrganizationPost, editingPostId = postId))
                                    },
                                    onEdit = {
                                        roleStore.clearSettingsError()
                                        navigate(route.copy(page = Screen.EditOrganization))
                                    },
                                    applicationsReady = applicationStore.loaded,
                                    rolesReady = roleStore.loaded,
                                    roleError = roleStore.error,
                                    isPresident = roleStore.roles[organizationId] == "President",
                                    onRetryRoles = { scope.launch { roleStore.refreshRoles() } },
                                    onReview = { navigate(route.copy(page = Screen.ReviewApplications)) },
                                    onOpenMembers = { navigate(route.copy(page = Screen.OrganizationMembers)) },
                                    id = organizationId,
                                    initialPostId = route.organizationPostId,
                                    onApply = { navigate(route.copy(page = Screen.Apply)) },
                                )
                            Screen.CreateOrganizationPost -> {
                                val organization = repository.organizations.firstOrNull { it.id == organizationId }
                                val post = editingPostId?.let { id ->
                                    repository.posts.firstOrNull { it.id == id && it.organizationId == organizationId }
                                }
                                when {
                                    organization == null -> Text("This organization is no longer available.", Modifier.padding(24.dp))
                                    roleStore.roles[organizationId] != "President" ->
                                        Text("President access is required to manage posts.", Modifier.padding(24.dp))
                                    editingPostId != null && post == null ->
                                        Text("This post is no longer available.", Modifier.padding(24.dp))
                                    else -> CreateOrganizationPostPage(
                                        organization = organization,
                                        store = roleStore,
                                        post = post,
                                        onCancel = { goBack() },
                                        onPublished = { postId ->
                                            if (backStack.last() === route) {
                                                backStack.removeAt(backStack.lastIndex)
                                                val organizationRoute = backStack.last()
                                                backStack[backStack.lastIndex] = organizationRoute.copy(organizationPostId = postId)
                                            }
                                            showSuccess(if (post == null) "Post published" else "Post updated")
                                        },
                                    )
                                }
                            }
                            Screen.EditOrganization -> {
                                val organization = repository.organizations.firstOrNull { it.id == organizationId }
                                when {
                                    organization == null -> Text("This organization is no longer available.", Modifier.padding(24.dp))
                                    roleStore.roles[organizationId] != "President" ->
                                        Text("President access is required to edit this organization.", Modifier.padding(24.dp))
                                    else -> EditOrganizationPage(
                                        organization = organization,
                                        store = roleStore,
                                        onCancel = { goBack() },
                                        onSaved = {
                                            if (backStack.last() === route) goBack()
                                            showSuccess("Organization settings saved")
                                        },
                                    )
                                }
                            }
                            Screen.OrganizationMembers, Screen.OrganizationMemberProfile ->
                                OrganizationMembersPage(
                                    organizationId = organizationId,
                                    organizationName = repository.organizations.firstOrNull { it.id == organizationId }?.name.orEmpty(),
                                    selectedMemberId = route.memberId.takeIf { page == Screen.OrganizationMemberProfile },
                                    viewerId = userId,
                                    store = roleStore,
                                    onOpenMember = {
                                        navigate(route.copy(page = Screen.OrganizationMemberProfile, memberId = it))
                                    },
                                    onMembershipChanged = {
                                        scope.launch { runCatching { persistence.synchronize() } }
                                    },
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
                                    defaultStudentNumber = profileStore.studentNumber.ifBlank {
                                        defaultStudentNumber(accountEmail)
                                    },
                                    defaultYearLevel = profileStore.yearLevel,
                                    onSubmitted = { id ->
                                        if (backStack.last() === route) {
                                            backStack[backStack.lastIndex] = Route(Screen.Status, tab = 2, applicationId = id)
                                        }
                                        showSuccess("Application submitted")
                                    },
                                )
                            Screen.ApplicationHistory -> ApplicationHistoryPage(
                                repository = repository,
                                onOpenApplication = { id ->
                                    navigate(route.copy(page = Screen.Status, applicationId = id))
                                },
                                onOpenDirectory = {
                                    openTab(1)
                                },
                                onOpenOrganization = { id -> openOrganization(id) },
                            )
                            // Send the selected application ID to ApplicationStatusScreen.kt so it can read the current record.
                            Screen.Status -> ApplicationPage(repository = repository, id = route.applicationId)
                            Screen.Main ->
                                when (route.tab) {
                                    0 -> FeedPage(
                                        repository = repository,
                                        onOpenOrganization = { id -> openOrganization(id) },
                                        onOpenPost = { id, postId -> openOrganization(id, postId) },
                                    )
                                    1 ->
                                        DirectoryPage(
                                            repository = repository,
                                            onRegisterOrganization = { navigate(route.copy(page = Screen.OrganizationRegistration)) },
                                            onOpenOrganization = { id -> openOrganization(id) },
                                        )
                                    2 ->
                                        if (!applicationStore.loaded) {
                                            val error = applicationStore.error
                                            if (error == null) {
                                                StageCallLoadingScreen("Loading applications…")
                                            } else {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    StatusPanel(error, isError = true, actionLabel = "Retry", onAction = { scope.launch { applicationStore.refresh() } })
                                                }
                                            }
                                        } else ApplicationsPage(
                                            repository = repository,
                                            refreshError = applicationStore.error,
                                            onRetry = { scope.launch { applicationStore.refresh() } },
                                            onOpenHistory = { navigate(route.copy(page = Screen.ApplicationHistory)) },
                                            onOpenApplication = { id ->
                                                navigate(route.copy(page = Screen.Status, applicationId = id))
                                            },
                                            onOpenDirectory = { openTab(1) },
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
                                                scope.launch {
                                                    if (!profileStore.saveTheme(requested)) {
                                                        preferenceError = "Could not save your theme. Check your connection and try again."
                                                    }
                                                }
                                            },
                                            onOpenOrganization = { id -> openOrganization(id) },
                                            onOpenDirectory = { openTab(1) },
                                            onOpenApplications = { openTab(2) },
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
