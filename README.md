# StageCall

StageCall helps university students discover campus organizations and updates, apply for membership, and follow their applications. Organization presidents can review membership applications, and StageCall admins can approve new organization registrations. The app uses Kotlin and Compose Multiplatform for Android, desktop, web, and iOS, with Supabase for authentication and data.

[![Download Android APK](docs/assets/download-android.svg)](https://github.com/paulAngelo-ws/StageCall/releases/latest)

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

For iOS, open `iosApp` in Xcode.

## Project layout

- `shared/src/commonMain/kotlin/com/weissev/stagecall/` contains the shared app entry point, `App.kt`, with feature screens under `ui/`, repositories and models under `data/`, and Supabase stores under `data/remote/`.
- Platform source sets in `shared/src/` implement catalog caching. Shared behavior tests live in `shared/src/commonTest/`.
- `androidApp/`, `desktopApp/`, `webApp/`, and `iosApp/` contain platform entry points and packaging.
- `supabase/migrations/` contains numbered database changes. `supabase/admin/` contains scripts intended for manual administration.

## Follow the data through the app

- **Signing in** follows the callback from `LoginScreen.kt` through `App.kt` to `AuthSession.kt`. Android handles the return URL in `MainActivity.kt`. iOS sends it from `iOSApp.swift` to `MainViewController.kt`.
- **Loading the feed** starts in `SupabaseStateStore.kt`. `CollectionCache.kt` gives migration 017 the saved row versions and merges the returned changes. `RepositoryPersistence.kt` then updates `StageCallRepository.kt`, which supplies the posts to `FeedScreen.kt`.
- **Submitting an application** sends the form from `ApplyScreen.kt` to `ApplicationStore.kt`. The store updates `StageCallRepository.kt` and returns the ID. `App.kt` sends that ID to `ApplicationStatusScreen.kt`.
- **Loading images** sends a post’s storage path from `FeedScreen.kt` or a Google photo URL from `ProfileScreen.kt` to `MediaCache.kt`. The platform implementations of `RepositoryCache.kt` handle local storage.
- **Packaging** uses the Android drawable and mipmap resources referenced by `AndroidManifest.xml`. Xcode uses the images listed in `Assets.xcassets` and the settings in `Config.xcconfig` and `Info.plist`.

## Set up Supabase

1. Create a Supabase project. Run `supabase/migrations/202611180003_public_catalog.sql` in its SQL Editor to create and seed the public catalog.
2. Run these files in order in the SQL Editor:
   - `supabase/migrations/202611180004_membership_applications.sql`
   - `supabase/migrations/202611180005_organization_roles.sql`
   - `supabase/migrations/202611180006_user_profiles.sql`
   - `supabase/migrations/202611180007_organization_settings.sql`
   - `supabase/migrations/202611180008_profile_preferences.sql`
   - `supabase/migrations/202611180009_application_length_limits.sql`
   - `supabase/migrations/202611180010_university_auth.sql`
   - `supabase/migrations/202611180011_organization_posts.sql`
   - `supabase/migrations/202611180012_post_audiences.sql`
   - `supabase/migrations/202611180013_post_images.sql`
   - `supabase/migrations/202611180014_organization_notifications.sql`
   - `supabase/migrations/202611180015_organization_registrations.sql`
   - `supabase/migrations/202611180016_allow_admin_registration_review.sql`
   - `supabase/migrations/202611180017_incremental_sync.sql`
3. Copy the entries from `local.properties.example` to the root `local.properties`, keeping any existing `sdk.dir` entry:

   ```properties
   SUPABASE_URL=https://YOUR_PROJECT.supabase.co
   SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
   ```

4. Run `./gradlew :shared:checkSupabaseConfig`, then rebuild and relaunch the app. Install the rebuilt APK for Android.

Use a publishable key or legacy `anon` key, never a secret or `service_role` key. Client credentials are embedded in the app; database row level security (RLS) controls access. `local.properties` is ignored by Git, and nonblank environment variables with the same names take precedence.

Migration `003` is safe to rerun and preserves existing catalog values. Migrations `001` and `002` belong to an earlier prototype and are not required for a new setup. The catalog seed provides five organizations and three campus posts.

### Google sign-in

1. In Supabase, open **Authentication → Sign In / Providers → Google** and copy the callback URL shown there.
2. In Google Cloud Console, configure the OAuth consent screen and add test users if the app is in testing. Create a **Web application** OAuth client and add the Supabase callback URL to its authorized redirect URIs.
3. Enable Google in Supabase and enter the client ID and secret. Keep the secret in Supabase, not in the app or repository.
4. In Supabase **Authentication → URL Configuration**, allow `stagecall://auth` for Android and iOS, the exact web origin including its port and your production URL, and `http://localhost:*` for desktop.
5. In **Authentication → Hooks**, enable the **Before User Created** hook with the Postgres function `public.stagecall_before_user_created`, and the **Custom Access Token** hook with `public.stagecall_restrict_access_token`. Applying the migration alone does not enable these hooks. If either hook is already in use, combine its existing logic with these functions instead of replacing it.
6. Rebuild and sign in with Google. Only addresses at `ubaguio.edu` or its subdomains (such as `20255821@s.ubaguio.edu`) are allowed. Test an unrelated Google account as well. Existing access tokens remain valid until they expire; the token hook blocks the next token issuance or refresh. Check **Authentication → Users**, restart to test session restoration, and use **Profile → Sign out** to test sign-out.

### Assign a president

After migrations `004` and `005`, a project administrator can assign a president once that person has signed in. Replace the example organization ID and email before running this in the Supabase SQL Editor:

```sql
insert into public.organization_memberships (organization_id, user_id, role)
select '1', id, 'President' from auth.users where email = 'president@example.com'
on conflict (organization_id, user_id) do update set role = excluded.role;
```

If no row is inserted, check the email in **Authentication → Users**. `supabase/admin/assign_game_club_president.sql` is a specific assignment script; review its email and organization name before running it. A president can open their organization and select **Review applications**. The database checks their organization when recording a decision. Presidents can also use **Edit organization** to change the About heading and description, open or close registration, and choose a banner color.

### Assign a registration admin

After migration `015`, have the intended admin sign in with their university account at least once. Replace the example email with their sign-in email and run the following in the Supabase SQL Editor.

```sql
insert into public.stagecall_admins (user_id)
select id from auth.users where lower(email) = lower('admin@ubaguio.edu')
on conflict (user_id) do nothing;
```

If **Admin review** is missing, check that the insert added a row for the exact account under **Authentication → Users**. Running it with the example address adds no row unless that account exists. The app checks access when you open the registration page and checks again while it stays open. To revoke access, delete that account's row from `public.stagecall_admins`.

## Register a club or organization

1. Open **Directory → Register an organization** and submit the organization's name, category, tagline, and description.
2. From an admin account, open **Directory → Register an organization → Admin review**. The queue includes all pending requests, including ones the admin submitted. Approve or reject the registration and optionally include a message for the applicant.
3. The applicant can see the decision in **My registrations**, which updates automatically. Approval publishes the listing and assigns the applicant as its president. Recruitment starts closed and the president can open it under **Edit organization**.

## Try the membership workflow

1. Sign in and open an organization in **Directory** whose recruitment is open.
2. Apply with a student ID, year level, reason, and consent.
3. Open **Applications** to see the pending application and its details.
4. Sign in as the assigned president to accept or reject it. An acceptance creates a membership.
5. Return to the student account to see the decision and membership in the app.

The database prevents duplicate applications and closed-recruitment submissions. Reapplication after rejection is currently unavailable. Applications and roles refresh while the app is open.

## Data and limitations

StageCall opens the saved public catalog and the signed-in account’s saved profile, preferences, and applications while it syncs with Supabase. With migration 017 applied, refreshes download new or changed rows and remove rows that have been deleted or are no longer visible. Members-only posts and notifications are checked against current access before they are displayed. Recently viewed post images and profile photos are stored on the device subject to available storage. A failed refresh can still show the saved public catalog and the account’s own saved data.

The in-app notification feed refreshes approximately every 10 seconds while the app is open. It includes public posts, members-only posts from organizations the user belongs to, organization profile changes, and changes to application availability. Read status is saved per account. Existing posts are included when migration 014 is applied; earlier organization changes are not backfilled.

Catalog data can be edited through the Supabase Table Editor. Clients can read organizations and public posts; RLS restricts members-only posts and denies direct catalog writes. Presidents can attach one JPEG or PNG photo up to 5 MB to a post. Photos are stored in the private `post-images` bucket and use the post's audience for access. Failed post submissions can leave unused uploads that an administrator may need to remove. Organization member counts and device notifications are not connected.

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
