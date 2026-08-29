# Security

## Reporting a vulnerability

If you believe you have found a security issue in Flow, please report it responsibly.

**Preferred:** open a [GitHub Security Advisory](https://github.com/Deepak4750/Flow-Android/security/advisories/new) on this repository (private report to the maintainer).

**Alternative:** open a GitHub issue only if the report is not sensitive. Do not post exploit details publicly before a fix is available.

Please include:

- Flow version (`Settings` → `About`)
- Android version and device model (if relevant)
- Steps to reproduce
- Expected vs actual behavior
- Impact assessment, if known

## Scope

In scope:

- Flow Android app (`com.deepak.flow`)
- This source repository
- The public update manifest and APK distribution used by the in-app updater (`Flow-Releases`)

Out of scope:

- Third-party devices, ROMs, or sideload tooling
- Social engineering
- Denial-of-service against GitHub infrastructure

## What we protect

Flow is offline-first. The highest-impact issues are usually:

- local data exposure on a shared device
- unintended network transmission of personal data
- unsafe backup/restore behavior
- update mechanism tampering (manifest or APK integrity)

## Response

The maintainer will acknowledge reports as soon as practicable. Fixes for confirmed issues will ship through the normal release process.
