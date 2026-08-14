# Flow

Flow is a private, offline-first reminder and follow-through companion for Android. It is built around one idea — making showing up feel good — so the interface stays quiet, tells you what happens next, and never scolds you for what you missed.

Everything lives in a local Room database on the device. There is no account, no sync, and no networking dependency in the build at all: the only libraries in use are AndroidX, Compose, Room, kotlinx.serialization, and kotlinx.coroutines.

## Screenshots

Not yet included.

## Features

**Reminders.** Each reminder has a title, a category, a time of day, a start date, and an optional end date. Categories are Health, Fitness, Study, Work, Personal, and Custom, where Custom takes a free-text name of your own. The model and the scheduling engine both handle several times per day, but the create screen currently exposes one.

**Flexible scheduling.** Five repeat types, presented in the UI as "Every day", "Every week", "Every month", "Every few days", and "Every few hours".

| Repeat type    | What it needs                                | Notes                                                              |
| -------------- | -------------------------------------------- | ------------------------------------------------------------------ |
| `Daily`        | at least one time of day                     | Fires every day from the start date onward                          |
| `Weekly`       | a set of weekdays                            | At least one day must stay selected                                 |
| `Monthly`      | a day of the month (1–31)                    | Clamped to the last day in shorter months, so 31 becomes 28/29/30   |
| `EveryXDays`   | an interval (1–365) plus an anchor date/time | Occurrences are counted from `startDate`, not from the current day  |
| `EveryXHours`  | an interval (1–168) plus an anchor date/time | Anchored on `startDate` + the earliest reminder time                |

For the two interval types the create screen lets you either start the cycle from now — the anchor becomes the current date and minute — or pick a custom start date and time.

**Active hours.** An optional waking window per reminder, for example 08:00 to 23:00. Windows that cross midnight (22:00 to 06:00) are handled, and a window whose start equals its end means "all day". The two families of schedule treat the window differently, because that is what each one should sensibly do: fixed-time schedules skip any reminder time that falls outside the window, while hourly interval schedules push the occurrence forward to the next time the window opens.

**Notification note.** The per-reminder note is what appears as the notification body. Leave it blank and Flow falls back to "Time to show up."

**Why.** An optional, private field for the reason behind a reminder. It is stored with the reminder and is not shown in the notification.

**Managing reminders.** The home screen lists everything, shows the single next occurrence across all enabled reminders under a "Next up" heading, and lets you toggle a reminder on or off inline, tap through to edit it, or delete it behind a confirmation dialog. Disabled reminders stay in the list, dimmed, with their alarm cancelled.

**Onboarding.** A single screen on first launch that asks for a name and an optional nickname, both skippable. The nickname is what shows up in the home greeting ("Good morning, Alex"), which switches between morning, afternoon, and evening.

**Settings and navigation.** A modal navigation drawer moves between Reminders, Settings, and About. Settings holds the profile fields, a shortcut into the system notification permission screen that re-reads the current state on resume, a count of stored reminders, and a delete-all action guarded by a confirmation dialog. About shows the version, the tagline, and the privacy statement.

## Tech stack

- **Kotlin** 2.2.10, Java 21 toolchain
- **Jetpack Compose** (BOM 2026.02.01) with **Material 3** as the foundation beneath a custom design system
- **Navigation Compose** 2.9.0 with type-safe `@Serializable` routes
- **Room** 2.7.1 with KSP for local persistence
- **AlarmManager** plus `BroadcastReceiver`s for delivery
- **ViewModel** / **StateFlow** / **Coroutines** for presentation state
- **kotlinx.serialization** for the schedule and time payloads stored in the database

| | |
| --- | --- |
| `applicationId` | `com.deepak.flow` |
| `minSdk` | 30 |
| `targetSdk` / `compileSdk` | 37 |
| `versionName` | 1.0.0 (`versionCode` 1) |
| AGP / Gradle | 9.3.1 / 9.5.1 |

## Architecture

Single Gradle module (`:app`), MVVM, layered by package rather than by Gradle project.

The UI layer is Compose screens paired with `AndroidViewModel`s that expose a single immutable `UiState` as a `StateFlow`. Screens collect with `collectAsStateWithLifecycle` and call methods on the ViewModel; they never touch Room or AlarmManager.

The data layer is two repository interfaces — `ReminderRepository` and `ProfileRepository` — with Room-backed implementations. `ReminderRepositoryImpl` is deliberately the place where persistence and alarms are kept in step: inserting, updating, enabling, disabling, or deleting a reminder also schedules or cancels its alarm, so no caller has to remember to do both.

Dependency injection is manual. `FlowApplication` builds the database, the `NotificationScheduler`, and both repositories once, and `FlowViewModelFactory` hands them to ViewModels. There is no DI framework.

`SchedulingEngine` is a pure Kotlin class with no Android dependencies — it takes a reminder, a reference `Instant`, and a `ZoneId` and returns the next occurrence, which is why it is the most heavily tested part of the codebase.

```
app/src/main/java/com/deepak/flow/
├── MainActivity.kt              edge-to-edge host
├── FlowApplication.kt           manual DI container
├── FlowViewModelFactory.kt
├── app/
│   ├── FlowApp.kt               onboarding gate + NavHost
│   ├── navigation/              routes, drawer content
│   ├── theme/                   Color, Type, Theme, FlowTokens
│   └── components/              FlowDesignSystem.kt
├── core/
│   ├── model/                   Reminder, Schedule, ActiveHours, Category, UserProfile
│   ├── database/                FlowDatabase, entities, DAOs, TypeConverters
│   ├── repository/              interfaces + Room-backed implementations
│   ├── scheduling/              SchedulingEngine
│   └── notification/            NotificationScheduler, channel, receivers
└── feature/
    ├── home/presentation/       list, next-up, greeting
    ├── reminder/presentation/   create/edit, schedule controls
    ├── onboarding/presentation/
    └── settings/presentation/   settings, about
```

### Scheduling and alarms

Flow keeps exactly one pending alarm per reminder, keyed by the reminder id, rather than pre-scheduling a series. The chain works like this:

1. `NotificationScheduler` asks `SchedulingEngine` for the next occurrence and registers a single `setAndAllowWhileIdle(RTC_WAKEUP, …)` alarm with a `PendingIntent` carrying the reminder id and the scheduled epoch millis.
2. `AlarmReceiver` fires, calls `goAsync()`, loads the reminder, and asks the engine whether the occurrence is still valid — a reminder edited or disabled in the meantime, or an alarm delivered more than 15 minutes late, is dropped rather than posted stale.
3. If valid it posts the notification, then immediately cancels and re-registers the alarm for the following occurrence. That self-rescheduling step is what keeps a repeating reminder alive.

Because Android drops all alarms on reboot and because a pending alarm computed in one timezone is wrong in another, two receivers rebuild the whole set:

- `BootReceiver` listens for both `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED` and reschedules every enabled reminder.
- `TimezoneChangedReceiver` listens for `TIMEZONE_CHANGED` and does the same, so a flight does not silently shift every reminder by the offset difference.

The engine itself walks forward day by day for fixed-time schedules (bounded by the end date, or two years out if there is none) and computes intervals arithmetically from the anchor for `EveryXHours`. All arithmetic goes through `ZonedDateTime`, so DST transitions keep local wall-clock times intact.

### Persistence

`FlowDatabase` is at version 3 with two entities. `ReminderEntity` stores the schedule, the reminder times, and the active-hours window as serialized JSON columns alongside the scalar fields; `UserProfileEntity` holds the single-row profile and the onboarding flag. `ReminderDao` exposes the list as a `Flow` plus suspend functions for the rest, and `DatabaseConverters` supplies the Room `TypeConverter`s. The database is built with `fallbackToDestructiveMigration(dropAllTables = true)`, so a schema change wipes local data instead of migrating it — fine for a personal app, worth knowing before you bump the version.

## Design principles

The visual system is unusually constrained on purpose. If you are modifying the UI, these are the rules it is built on:

- **Dark only.** One `darkColorScheme`, no light theme, no theme toggle, no dynamic colour.
- **Monochrome.** Greys carry all hierarchy. `FlowAccent` (a cool blue) is the only chromatic colour in the product and it is reserved for a single semantic job — marking what happens next, as the "Next up" dot on home and the "Next" label on the corresponding row. The same dot marks the selected row in an option sheet. `FlowError` exists for destructive actions. Nothing else is coloured, ever.
- **Documented contrast ramp.** The four text colours sit at roughly 21:1, 12.6:1, 9.4:1, and 4.9:1 against the black background, so every step — including disabled text — clears WCAG AA. Interactive outlines clear the 3:1 non-text floor. The ratios are recorded in `Color.kt`; keep them true if you change a value.
- **Custom components, not stock Material.** `FlowDesignSystem.kt` provides Flow's own switch, dialog, stepper, chips, buttons, text field, FAB, option sheet, drawer item, and rows. Material 3 supplies the theme, scaffolding, and a few structural pieces, but the visible controls are Flow's. Semantics — roles, headings, content descriptions, 48dp minimum touch targets from `FlowSizes.touchTarget` — are built into those components rather than added per screen.
- **Two type families.** A light-weight sans with tight negative tracking for statements and titles, and a monospace with wide positive tracking for uppercase labels and metadata. Nothing else.
- **Tokens, not magic numbers.** Spacing, sizes, and motion durations come from `FlowTokens.kt`. Animations are 120–220ms; they exist to explain a state change, not to decorate one.
- **Progressive disclosure.** The create screen shows task, category, when, and note. Start date, end date, why, and active hours live behind a single "Advanced" action.

## Building and running

Prerequisites:

- JDK 21 — this project is built with `C:\Users\dammy\.jdks\jdk-21.0.12+8`
- Android SDK with platform 37
- `local.properties` pointing at the SDK via `sdk.dir` (Android Studio writes this for you)

On Windows PowerShell, set both environment variables first. Note that PowerShell uses `;` as the statement separator — `&&` will not work:

```powershell
$env:JAVA_HOME = "C:\Users\dammy\.jdks\jdk-21.0.12+8"
$env:ANDROID_HOME = "C:\Users\dammy\AppData\Local\Android\Sdk"
cd C:\Users\dammy\Flow; .\gradlew.bat assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device with:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Opening the project in Android Studio and running the `app` configuration works too; the wrapper pins Gradle 9.5.1 either way.

`build-and-package.ps1` at the repo root runs the tests, builds the debug APK, and zips a copy of the source tree next to the project. It contains hard-coded local paths, so treat it as a personal convenience script rather than part of the build.

## Testing

```powershell
cd C:\Users\dammy\Flow; .\gradlew.bat test
```

All tests are local JVM unit tests under `app/src/test/`; there are no instrumented tests in the project yet, though the androidTest dependencies are declared.

- **`SchedulingEngineTest`** — the bulk of the suite. Next-occurrence resolution for all five schedule types; multiple times per day rolling forward to the next day; monthly clamping in short months and on a leap year; `EveryXDays` counting from the start date; start-date and end-date boundaries; disabled reminders returning null; active hours skipping inactive fixed times and shifting hourly intervals into the window; an end date suppressing a shifted occurrence; a DST spring-forward date; and the rule that the next occurrence must be strictly after the reference instant.
- **`ActiveHoursTest`** — the window predicate for a normal window, a window crossing midnight, and equal start/end meaning 24 hours.
- **`ScheduleSerializationTest`** — JSON round-trips for every `Schedule` variant and for `ActiveHours`, which matters because these are the on-disk formats.
- **`ReminderModelTest`** — model defaults and category display names.
- **`GreetingTest`** — greeting selection by hour, with and without a nickname.

## License

No license has been chosen for this project yet.
