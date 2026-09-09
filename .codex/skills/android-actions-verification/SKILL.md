---
name: android-actions-verification
description: Validate Android changes in BemfaControl through GitHub Actions instead of local Gradle builds.
---

## Verification Boundary

Do not run `./gradlew` or `gradlew.bat` locally for Android compilation, tests, or APK assembly. GitHub Actions is the source of truth for build and test validation; local workspaces are not required to provide a JDK or Android SDK.

Before handing off, run only lightweight checks that do not require the Android toolchain, such as `git diff --check` and focused review of the touched Kotlin or Compose declarations.

When implementation is ready, commit and push so the configured GitHub Actions workflow can build and test it. Treat a failed workflow as a blocking verification result. Do not report the work as fully verified until the workflow succeeds or the user explicitly accepts an unverified handoff.
