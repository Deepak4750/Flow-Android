# Flow

A private, offline-first companion for Android.

Flow helps you follow through on what you decided matters: tasks, water, gym workouts, and quiet daily progress. It is built around one idea: **make showing up feel good.**

There is no account, no cloud sync, and no in-app analytics. Your data stays on your phone unless you explicitly use the network to check for app updates.

**Current stable release: [1.3.3](https://github.com/Deepak4750/Flow-Releases/releases/latest)**

[Download the APK](https://github.com/Deepak4750/Flow-Releases/releases/download/v1.3.3/Flow-1.3.3.apk) · [All releases](https://github.com/Deepak4750/Flow-Releases/releases) · [Source](https://github.com/Deepak4750/Flow-Android)

Requires Android 11 or later (API 30+). Install over a previous Flow build to keep your data.

---

## Features

### Home

- Personal greeting through the day, optionally using your nickname.
- **Next up** for the soonest task.
- Quiet daily follow-through count, not a guilt dashboard.

### Tasks

- Calm vertical list: what, when, and status.
- Create and edit with progressive disclosure instead of a giant form.
- Built-in and custom categories.
- Optional **Why** field (private motivation, not shown in notifications).
- Scheduling: daily, weekly, monthly, every N days, every N hours.
- Active hours ("only remind me while I'm awake").
- Notifications with Complete, Dismiss, and optional Snooze.
- Home screen **Today** and **Progress** widgets.

### H₂O

- Fast water logging (250 ml, 500 ml, 1 L, custom amounts).
- Daily goal and progress.
- Drink reminders with quiet notifications.
- Widget support for quick logging.

### Gym

- **Routines** with multi-day plans and a routine builder.
- **Free Workout** for ad-hoc sessions.
- Active workout UI focused on the current set.
- Rest timer with up-next preview.
- Weight and rep tracking with previous performance context.
- History of completed workouts.

### History

- Day-by-day view of tasks, water, and gym activity.
- Read-only look back without turning Home into a dashboard.

### Onboarding

- First-run feature selection (Tasks, H₂O, Gym).
- Optional name and nickname.
- Sparse, intentional empty states.

### Settings

- Notification permission and snooze controls.
- **Keep data** toggle for local persistence across reinstall (see Privacy).
- In-app update check (stable channel).
- About screen with version and privacy summary.
- Preview update channel (hidden; tap version seven times in About).

---

## Privacy / Data

Flow is **offline-first**. You can verify this in the source code.

### What is stored locally

All personal data lives in a Room SQLite database (`flow_database`) in the app's private storage. Tables include:

| Area | Stored locally |
| --- | --- |
| Tasks | Reminders, schedules, completions, categories |
| Profile | Name, nickname, feature toggles, preferences |
| H₂O | Daily intake and water reminder settings |
| Gym | Routines, workouts, sets, exercise notes |
| History | Derived from the above tables |

Room schema JSON is exported under `app/schemas/` for migration review.

### Keep Data

When **Keep data** is enabled in Settings (default on), Flow writes a local copy of the database to:

`Documents/Flow/flow-keep.db`

plus a small metadata file (`flow-keep.meta`) scoped to your Android user profile. On reinstall, Flow can restore from this copy if the private database is missing.

- Nothing is uploaded.
- Backups are scoped per Android user profile (clones/work profiles do not inherit another profile's backup).
- Turning Keep data off deletes the public Documents copy.

See `KeepDataStore.kt` and `FlowApplication.kt`.

### Backup / restore

Flow does **not** use Android cloud backup for task data in a way that syncs to a server. The user-controlled mechanism is **Keep data** (local file on device).

There is no export-to-cloud feature in the app today.

### Network use

The app makes outbound network requests only for **in-app updates**:

1. Fetch `latest.json` from the public [Flow-Releases](https://github.com/Deepak4750/Flow-Releases) repository.
2. Download the APK URL named in that manifest when you choose to install an update.

No other routine network calls for tasks, gym, water, or profile data were found in the application source.

### Analytics and tracking

- No Firebase, Crashlytics, Sentry, or third-party analytics SDKs are included in this project.
- No advertising IDs are collected by Flow.
- GitHub may log access when you download releases or when the app fetches `latest.json`; that is outside the app binary.

### What you can verify from this repository

- Room entities, DAOs, and migrations (`app/src/main/java/com/deepak/flow/core/database/`)
- Keep Data read/write logic (`KeepDataStore.kt`)
- Update manifest URLs (`UpdateChannel.kt`, `AppUpdateRepository.kt`)
- Notification and scheduling code
- UI and feature logic for Tasks, H₂O, Gym, and History

### What you cannot verify from this repository alone

- Behavior of GitHub's hosting and release CDN
- Whether a specific APK on GitHub was built from a given commit (release builds are debug-key signed for sideloading; compare `versionCode` / `versionName` in the installed app)
- Future infrastructure not present in this repo (e.g. private download analytics)

---

## Download

1. Download [Flow-1.3.3.apk](https://github.com/Deepak4750/Flow-Releases/releases/download/v1.3.3/Flow-1.3.3.apk).
2. Allow install from that source if Android asks.
3. Open the APK. Installing over an older Flow keeps data in place when `versionCode` increases.

After install, Flow can also offer updates in-app when a newer stable build is published.

Stable APKs and OTA manifests are published at [Deepak4750/Flow-Releases](https://github.com/Deepak4750/Flow-Releases). The app reads `latest.json` there and downloads the APK from the matching GitHub Release.

---

## Build

Prerequisites: **JDK 21**, **Android SDK platform 37**, and `local.properties` with `sdk.dir`.

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease
```

The sideloadable release APK is written to:

`app/build/outputs/apk/release/app-release.apk`

| | |
| --- | --- |
| `applicationId` | `com.deepak.flow` |
| `minSdk` | 30 |
| `targetSdk` / `compileSdk` | 37 |
| `versionName` | 1.3.3 (`versionCode` 180) |
| Kotlin / AGP / Gradle | 2.2.10 / 9.3.1 / 9.5.1 |

Release builds are signed with the debug key so they can be sideloaded without a private keystore in the tree. Do not publish a Play Store build from this configuration.

### Architecture

Single Gradle module (`:app`), MVVM, Compose UI, Room persistence, manual DI in `FlowApplication` (no Hilt/Koin). `SchedulingEngine` is pure Kotlin for next-fire time calculation.

### Testing

```powershell
.\gradlew.bat testDebugUnitTest
```

JVM unit tests cover scheduling, widgets, notifications, gym logic, Keep Data, and update manifests.

---

## License

**No license has been chosen for this project yet.** You may inspect the source, but redistribution terms are not defined until a `LICENSE` file is added.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Security

See [SECURITY.md](SECURITY.md) for how to report vulnerabilities.
