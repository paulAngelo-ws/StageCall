# StageCall

StageCall helps university students discover campus organizations and updates, apply for membership, and follow their applications. Organization presidents can review applications. The app uses Kotlin and Compose Multiplatform for Android, desktop, web, and iOS, with Supabase for authentication and data.

[Download the Android APK](https://github.com/paulAngelo-ws/StageCall/releases/latest)

## Features

- Browse the organization directory and public campus feed, view organization details, and bookmark organizations.
- Sign in with Google, submit membership applications, and see their status. An accepted application creates a membership.
- Review pending applications as an organization's president, with an optional decision note.
- View memberships, edit a profile bio, choose a light or dark theme, and see in-app notifications.

## Run the app

The project uses the Gradle wrapper and a Java 21 Gradle toolchain. Android builds also require the Android SDK; iOS builds require Xcode on a Mac. Configure Supabase and Google sign-in as described below before using the app. On Windows, use `gradlew.bat` instead of `./gradlew`.

```sh
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

For iOS, open `iosApp` in Xcode. Platform folders provide startup code; the shared UI and logic live in `shared`.

## Set up Supabase

1. Create a Supabase project. Run `supabase/setup.sql` in its SQL Editor to create and seed the public catalog.
2. Run these files in order in the SQL Editor:
   - `supabase/migrations/202611180004_membership_applications.sql`
   - `supabase/migrations/202611180005_organization_roles.sql`
   - `supabase/migrations/202611180006_user_profiles.sql`
   - `supabase/migrations/202611180007_organization_settings.sql`
   - `supabase/migrations/202611180008_profile_preferences.sql`
   - `supabase/migrations/202611180009_application_length_limits.sql`
3. Copy the entries from `local.properties.example` to the root `local.properties`, keeping any existing `sdk.dir` entry:

   ```properties
   SUPABASE_URL=https://YOUR_PROJECT.supabase.co
   SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
   ```

4. Run `./gradlew :shared:checkSupabaseConfig`, then rebuild and relaunch the app. Install the rebuilt APK for Android.

Use a publishable key or legacy `anon` key, never a secret or `service_role` key. Client credentials are embedded in the app; database row level security (RLS) controls access. `local.properties` is ignored by Git, and nonblank environment variables with the same names take precedence.

`setup.sql` and migration `202611180003_public_catalog.sql` provide the same catalog setup; use either, not both as separate steps. The setup is safe to rerun and preserves existing catalog values. Migrations `001` and `002` belong to an earlier prototype and are not required for a new setup. The catalog seed provides five organizations and three campus posts.

### Google sign-in

1. In Supabase, open **Authentication → Sign In / Providers → Google** and copy the callback URL shown there.
2. In Google Cloud Console, configure the OAuth consent screen and add test users if the app is in testing. Create a **Web application** OAuth client and add the Supabase callback URL to its authorized redirect URIs.
3. Enable Google in Supabase and enter the client ID and secret. Keep the secret in Supabase, not in the app or repository.
4. In Supabase **Authentication → URL Configuration**, allow `stagecall://auth` for Android and iOS, the exact web origin including its port and your production URL, and `http://localhost:*` for desktop.
5. Rebuild and sign in with Google. Check **Authentication → Users** in Supabase, restart to test session restoration, and use **Profile → Sign out** to test sign-out.

### Assign a president

After migrations `004` and `005`, a project administrator can assign a president once that person has signed in. Replace the example organization ID and email before running this in the Supabase SQL Editor:

```sql
insert into public.organization_memberships (organization_id, user_id, role)
select '1', id, 'President' from auth.users where email = 'president@example.com'
on conflict (organization_id, user_id) do update set role = excluded.role;
```

If no row is inserted, check the email in **Authentication → Users**. `supabase/admin/assign_game_club_president.sql` is a specific assignment script; review its email and organization name before running it. A president can open their organization and select **Review applications**. The database checks their organization when recording a decision. Presidents can also use **Edit organization** to change the About heading and description, open or close registration, and choose a banner color.

## Try the membership workflow

1. Sign in and open an organization in **Directory** whose recruitment is open.
2. Apply with a student ID, year level, reason, and consent.
3. Open **Applications** to see the pending application and its details.
4. Sign in as the assigned president to accept or reject it. An acceptance creates a membership.
5. Return to the student account to see the decision and membership in the app.

The database prevents duplicate applications and closed-recruitment submissions. Reapplication after rejection is currently unavailable. Applications and roles refresh while the app is open.

## Data and limitations

The catalog is read from Supabase and refreshed approximately every 30 seconds. A verified public catalog cache can be used after a failed read; an initial failure shows **No database data loaded**. The cache uses Android preferences, a desktop file in `~/.stagecall/`, iOS user defaults, or browser local storage. Bookmarks, theme settings, applications, memberships, and the profile bio are tied to the signed-in account; the bio is limited to 500 characters.

Catalog data can be edited through the Supabase Table Editor. Clients can read organizations and public posts; RLS hides members-only posts and denies catalog writes. Organization member counts and members-only posts are not yet connected to memberships. File uploads, device notifications, and organization update management are not connected.

## Project structure

Shared Kotlin code is under `shared/src/commonMain/kotlin/com/weissev/stagecall/`:

- `App.kt` connects authentication, navigation, stores, and screens.
- `screens/` contains the feed, directory, organization, application, review, notification, and profile screens.
- `data/` contains models, repository state, Supabase stores, and caching.
- `components/` contains shared UI components and `StageCallTheme.kt`.
- `supabase/` contains catalog setup, migrations, and administrator scripts.

## Tests

```sh
./gradlew :shared:jvmTest :desktopApp:compileKotlin
```

The shared tests cover membership workflows, persistence, notifications, theme settings, and seed data. For a Windows network drive, disabling incremental compilation and file watching can avoid path conflicts:

```text
java -jar gradle/wrapper/gradle-wrapper.jar :shared:jvmTest :desktopApp:compileKotlin --no-configuration-cache --no-watch-fs "-Pkotlin.incremental=false"
```
