# SMS Ledger v1.156 — Reference Design First Pass

This is a separate native Android source package based on the uploaded v1.153 source project.

## First-pass UI changes
- Shared warm cream background, white cards, soft borders/shadows, compact typography, and amber gradient primary actions.
- Redesigned Home summary hero, financial metric cards, colorful quick-action tiles, and Family Hub layout.
- Five-item bottom navigation: Home, Ledger, Add, Assets, More.
- More screen links to existing Protection, Subscriptions, Loans/Assets, Events, Family Hub, and Settings areas.

## Build
The project uses Android Gradle Plugin 8.7.3 and Java 17. This archive does not include a Gradle wrapper. Build in an Android/Gradle environment using Gradle 8.9:

```bash
gradle --no-daemon assembleRelease --stacktrace --console=plain
```

Expected release APK location:
`app/build/outputs/apk/release/app-release.apk`

## Validation status
Inline JavaScript syntax check passed. APK compilation and Android device testing were not possible in the current environment because Gradle and the Android SDK are unavailable and external download access failed. This is a source package, not an APK. See `docs/release-notes/RELEASE_NOTES_V1.156.md`.
