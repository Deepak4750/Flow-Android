# Flow

A private, offline-first reminder companion for Android.

Flow is built around one idea: **make showing up feel good.** It remembers what you decided matters, tells you what happens next, and never scolds you for what you missed.

There is no account, no cloud sync, and no analytics. Reminders live only on the phone. The network is used only to check for app updates.

**Current stable release: [1.1.1](https://github.com/Deepak4750/Flow-Releases/releases/latest)**

[Download the APK](https://github.com/Deepak4750/Flow-Releases/releases/download/v1.1.1/Flow-1.1.1.apk) · [All releases](https://github.com/Deepak4750/Flow-Releases/releases)

Requires Android 11 or later (API 30+). Install over a previous Flow build to keep your reminders.

---

## Features

### Home

- A personal greeting that changes through the day — Good morning, Good afternoon, Good evening — and can include your nickname.
- A calm vertical list of reminders: what, when, and status.
- **Next up** called out so the soonest reminder is obvious in under two seconds.
- Mark a reminder complete for today with the same outlined check used in the rest of the app.
- Daily progress as a quiet count, not a guilt dashboard.
- Filter by category, including names you created yourself.
- Enable, disable, edit, or delete a reminder without leaving the list.

### Create and edit

- Progressive disclosure instead of a giant form: what, when, how often, then optional advanced settings.
- Title, category, time, and an optional notification note.
- Built-in categories: Health, Fitness, Study, Work, Personal.
- **Custom categories** you name yourself. Saved names reappear as chips the next time you create a reminder, and drop away when the last reminder with that name is permanently deleted.
- Optional **Why** — a private reason Flow keeps with the reminder, never shown in the notification.
- Optional start date, end date, and **active hours** (“only remind me while I’m awake”).
- Time defaults to now when you open create, so a new reminder is ready in seconds.

### Scheduling

Five repeat types, shown as ordinary choices rather than internal types:

| You pick | What Flow does |
| --- | --- |
| Every day | Fires each day at the chosen time |
| Every week | Fires on the weekdays you select |
| Every month | Fires on that day of the month (clamped in shorter months) |
| Every few days | Repeats every N days from the start you choose |
| Every few hours | Repeats every N hours, still respecting active hours |

Active hours that cross midnight are supported. A window whose start equals its end means all day.

### Notifications

- Quiet, high-importance reminders — a tap on the shoulder, not an alarm-clock shout.
- Actions: **Complete**, **Dismiss**, and optional **Snooze**.
- Swiping the shade item brings it back immediately until you Complete, Dismiss, or Snooze.
- Complete from the notification, Home, or the Today widget clears it and it stays cleared.
- The notification body is only the note you wrote. Placeholders never appear as if you wrote them.
- Exact alarms reschedule after boot and timezone changes.

### Home screen widgets

- **Today** — a rounded dark tile with today’s reminders. Scroll the list, mark complete with the in-app tick, tap a row to open Flow. The next reminder’s time is shown in Flow’s accent.
- **Progress** — today’s follow-through as a 7×7 dot matrix. Swipe to the percentage. Tap anywhere to open Flow.

### Settings and About

- Name and optional nickname.
- Notification permission, with the current state re-read when you return.
- Snooze on or off, and how long.
- Count of stored reminders, and delete-all behind a confirmation.
- Check for update in Settings. When a newer build is ready, Flow offers Install or Later.
- About: version, tagline, and a plain privacy statement.
- Preview channel (tap the version seven times on About) so one phone can see builds before they go to everyone else.

### Privacy

- Offline-first. Reminders never leave the device.
- No account, no sync, no tracking.
- Network is used only for the in-app updater.

---

## Install

1. Download [Flow-1.1.1.apk](https://github.com/Deepak4750/Flow-Releases/releases/download/v1.1.1/Flow-1.1.1.apk).
2. Allow install from that source if Android asks.
3. Open the APK. Installing over an older Flow keeps reminders in place.

After that, open Flow when a newer version is ready and tap **Install** on the update prompt.

Updates are published at [Deepak4750/Flow-Releases](https://github.com/Deepak4750/Flow-Releases). The app reads `latest.json` there and downloads the APK from the matching GitHub Release.

---

## Building from source

Prerequisites: JDK 21, Android SDK platform 37, and `local.properties` with `sdk.dir`.

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease
```

The sideloadable release APK is written to `app/build/outputs/apk/release/app-release.apk`.

| | |
| --- | --- |
| `applicationId` | `com.deepak.flow` |
| `minSdk` | 30 |
| `targetSdk` / `compileSdk` | 37 |
| `versionName` | 1.1.1 (`versionCode` 26) |
| Kotlin / AGP / Gradle | 2.2.10 / 9.3.1 / 9.5.1 |

## Architecture

Single Gradle module (`:app`), MVVM, packages rather than extra Gradle modules. Compose screens collect one `UiState` from an `AndroidViewModel`. Repositories sit on Room. `ReminderRepositoryImpl` keeps persistence and alarms in step. `FlowApplication` is the manual DI container — no Hilt or Koin.

`SchedulingEngine` is pure Kotlin: given a reminder, an instant, and a zone, it returns the next occurrence. Flow keeps one exact alarm per reminder and reschedules after each fire, after boot, and after a timezone change.

## Design

- Dark only. No light theme, no theme toggle, no dynamic colour.
- Hierarchy from type and space, not from extra cards.
- A restrained cool accent marks what happens next — not rainbow category chrome.
- Custom components in `FlowDesignSystem.kt` rather than stock Material controls everywhere.
- Copy never shames. Coming back counts.

## Testing

```powershell
.\gradlew.bat testDebugUnitTest
```

Local JVM tests cover scheduling, active hours, widgets, notification actions, custom categories, and update manifests. There are no instrumented tests yet.

## License

No license has been chosen for this project yet.
