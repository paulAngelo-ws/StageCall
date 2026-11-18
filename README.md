# StageCall

StageCall helps university students discover organizations, apply for membership,
read campus updates, and manage their communities. It uses Kotlin and Compose
Multiplatform to share one interface across Android, desktop, web, and iOS.

[Download the Android APK](https://github.com/paulAngelo-ws/StageCall/releases/latest)

## Start reading here

Most app code is in `shared/src/commonMain/kotlin/com/weissev/stagecall/`.
Read these files in this order:

1. `screens/PublicFeed.kt`: a complete screen with state, filter buttons, and post cards.
2. `screens/ProfileScreen.kt`: text fields and buttons for editing a profile.
3. `App.kt`: creates the shared data once and chooses which screen to display.
4. `data/Models.kt`: the information the app stores, such as students and applications.
5. `data/Organizations.kt`: the organization data model.
6. `data/StageCallRepository.kt`: the shared data and functions that change it.
7. `components/StageCallTheme.kt`: the app's colors and rounded shapes.

Each screen has its own file. Its layout is written directly using standard Compose
elements such as `Column`, `Card`, `Text`, `Button`, and `OutlinedTextField`.
There is no separate `UiComponents.kt` to look up when reading a screen.

- Discovery: `PublicFeed.kt`, `DirectoryScreen.kt`, and `OrgDetailScreen.kt`.
- Applications: `ApplicationsScreen.kt`, `ApplyScreen.kt`, and `ApplicationStatusScreen.kt`.
- Profile and appearance: `ProfileScreen.kt`, `ThemeSettings.kt`, and `components/StageCallTheme.kt`.

Read a screen from top to bottom: the variables above the main `Column` hold its
state, and the elements inside the `Column` appear in that order. A `for` loop draws
one card or filter chip for each item. The same layout code appears in several files
so you can edit a screen without following custom layout functions elsewhere.

## Kotlin and Compose basics

- `val` is a value you do not reassign. `var` is a value you can change.
- A `data class` groups related information. `copy` creates an updated version.
- An `enum class` lists a fixed set of choices, such as application statuses or pages.
- A function marked `@Composable` draws part of the interface.
- `Column` places items vertically. `Row` places them horizontally. `Box` stacks items.
- `Modifier` controls an element's size, padding, and other layout properties.
- `remember { mutableStateOf(...) }` keeps a screen value and redraws the UI when it changes.
- `by` lets you read and write that state like a normal variable.
- A `mutableStateListOf` is a list that also tells the UI when items change.
- `{ ... }` can be a small function. Buttons call these functions when clicked.
- Parameters starting with `on`, such as `onOpenOrganization`, are functions that
  the screen calls after an action. `App.kt` supplies them to change the current page.
- `it` refers to the one input of a short function. Longer conditions use named inputs.
- `filter` keeps matching items, `map` transforms items, and `any` checks for a match.

For example, clicking Apply calls `repository.submit(...)`. The repository checks
whether the student can apply, adds the application, and returns `true` if it worked.
The screen then opens the application's status page. All screens share the same
repository, so the Applications and Profile pages see the new application too.

## Change the design

Edit `components/StageCallTheme.kt` to change colors across the whole app.
In Profile → Account → Theme, switch between Light and Dark. The choice applies
throughout the app and is saved on the device (or in browser local storage).
To change a card, button, text field, or spacing, edit it directly in its screen file.
The layout stays centered on desktop and fills the available width on small screens.

## Run the app

Run these commands from the project folder. On Windows use `gradlew.bat`;
on macOS or Linux use `./gradlew`.

```text
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

For iOS, open `iosApp` in Xcode on a Mac.
The platform folders contain startup code; the shared folder contains the app itself.
Gradle's `.kts` files configure the build and dependencies; beginners usually do not
need to edit them to change a screen.

## Try the membership workflow

1. Open Directory and select Google Developer Groups.
2. Apply with a reason, year level, and consent.
3. Open Applications to see the Pending application.
4. Open the application to see its status and details.
5. Open Profile to see current memberships and switch between light and dark themes.

Applications use Pending, Accepted, and Rejected statuses. Duplicate applications
and reapplications after rejection are unavailable in this demo. Members-only posts
are visible to accepted, active members. Events require a date, time, and location.

## Run the existing tests

```text
./gradlew :shared:jvmTest :desktopApp:compileKotlin
```

For a Windows network drive, invoke Java directly. Disabling incremental compilation
avoids conflicts between mapped drive paths and their underlying network paths:

```text
java -jar gradle/wrapper/gradle-wrapper.jar :shared:jvmTest :desktopApp:compileKotlin --no-configuration-cache --no-watch-fs "-Pkotlin.incremental=false"
```

The tests cover application validation, duplicate submissions, closed recruitment,
approval, rejection, duplicate decisions, and required event details.

## Supabase and local caching

Put your project configuration in **`local.properties`** in the project root
(next to `gradlew.bat`). Keep any existing `sdk.dir` entry.

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
```

Use the publishable key (or legacy `anon` key), never a secret or `service_role` key.
`local.properties` is ignored by Git. These are public client credentials embedded
in the app; row level security controls database access. Nonblank environment
variables with the same names take precedence. Rebuild after changing the values.

1. Run the entire **`supabase/setup.sql`** file in your Supabase SQL Editor.
2. Its result should show **5 rows in `public.organizations`** and **3 rows in
   `public.campus_posts`**, immediately, even with zero Auth users.
3. Save your URL and publishable key in `local.properties` on disk.
4. Check configuration with
   `java -jar gradle/wrapper/gradle-wrapper.jar :shared:checkSupabaseConfig`.
5. Rebuild and relaunch. For Android, install the newly built APK.

**No sign-in is required. Do not enable anonymous sign-ins for this setup.**
The app queries the public tables directly using the publishable key. It does not
create Auth users or read/write private `stagecall_state` snapshots. Google login
will be added separately. Until then, profile editing, membership applications,
and bookmarks are disabled; the Profile screen shows Guest.

In the Table Editor, choose schema `public` and open `organizations` or
`campus_posts`. Each organization or post is a real row with editable columns.
Edit a name or post in Supabase; the app refreshes automatically every 30 seconds
while it is open.
The SQL Editor and Table Editor can manage data. Guest clients can only read
organizations and public posts; RLS hides members-only posts and denies writes.
No sample student profiles, memberships, or applications are exposed publicly.

`setup.sql` is also available as migration
`supabase/migrations/202611180003_public_catalog.sql`. Use either one, not both
as separate setup steps. Both are safe to rerun: existing row values are preserved.
The script disables the previous automatic private-snapshot signup trigger and
leaves any old `stagecall_state` data untouched. Migrations `001` and `002` are
legacy files for the earlier anonymous-session prototype and are not needed now.

The repository starts empty. All displayed catalog records come from the database
or its verified local cache; no sample records are compiled into the app. Caches
from the previous private-snapshot implementation are ignored. Background refresh
runs without a connection-status banner. Failed reads keep the previous catalog;
initial connection failures show **No database data loaded**.

The cache uses Android preferences, a desktop file in `~/.stagecall/`, iOS user
defaults, or browser local storage. Theme settings remain separate. No guest edits
or uploads are queued. Without project configuration, only a previously verified
public catalog cache can be displayed.

## Demo limitations

No account login screen, file uploads, or device notifications are connected.
Officer management screens are not part of the student interface. Repository
operations for decisions and organization updates remain covered by logic tests;
shared organization updates and role-based permissions still need implementation.
