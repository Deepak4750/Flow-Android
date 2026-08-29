# Contributing

Thank you for looking at Flow.

## Before you start

- Flow is a personal, offline-first Android app. Changes should respect privacy, minimal UI, and calm copy.
- Flow is licensed under the [Apache License, Version 2.0](LICENSE).
- Do not commit secrets, keystores, `local.properties`, APKs, databases, or personal device dumps.

## Development setup

1. Install JDK 21 and Android SDK platform 37.
2. Clone this repository.
3. Create `local.properties` with `sdk.dir` pointing at your Android SDK (not committed).
4. Build and test:

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease
```

## Pull requests

1. Keep changes focused. One concern per PR when possible.
2. Match existing Kotlin and Compose style in the module.
3. Run `.\gradlew.bat testDebugUnitTest` before opening a PR.
4. Describe user-visible behavior, not only implementation detail.
5. Do not bump `versionCode` / `versionName` unless asked.

## UI and copy

- Dark mode only. No light theme toggle.
- Prefer hierarchy from typography and spacing, not extra cards.
- Copy should never shame the user for missing a day.

## Questions

Open a GitHub issue for bugs, questions, or small improvements. Use a security advisory for sensitive reports (see [SECURITY.md](SECURITY.md)).
