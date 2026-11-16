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
5. `data/Organizations.kt`: the sample organizations and their descriptions.
6. `data/StageCallRepository.kt`: the shared data and functions that change it.
7. `components/StageCallTheme.kt`: the app's colors and rounded shapes.

Each screen has its own file. Its layout is written directly using standard Compose
elements such as `Column`, `Card`, `Text`, `Button`, and `OutlinedTextField`.
There is no separate `UiComponents.kt` to look up when reading a screen.

- Discovery: `PublicFeed.kt`, `DirectoryScreen.kt`, `OrgDetailScreen.kt`, and `MyOrganizationsScreen.kt`.
- Applications: `ApplicationsScreen.kt`, `ApplyScreen.kt`, and `ApplicationStatusScreen.kt`.
- Officers: `OfficerScreens.kt`, `ReviewApplicationScreen.kt`, `MembersScreen.kt`, and `PublishScreen.kt`.

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
repository, so the Applications and Officer pages see the new application too.

## Change the design

Edit `components/StageCallTheme.kt` to change colors across the whole app.
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
4. Open Profile, then the demo Officer Portal, and select the same organization.
5. Review the application and accept or reject it.
6. Acceptance adds the student to the member directory and My organizations.
7. Publish an announcement and return to Feed to see it.

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

## Demo limitations

Data and saved drafts last only until the app closes. No database, API, login,
file uploads, or device notifications are connected. The Officer Portal is a demo
role switch. Real authentication and permissions still need a backend.
