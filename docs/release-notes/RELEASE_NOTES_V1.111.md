# SMS Ledger v1.111

## Insurance
- Added accidental/risk coverage as a first-class policy field alongside sum assured.
- Insurance policy rows now show sum assured and accidental coverage.
- Added bulk policy import/review for multiple policy documents.
- Added parser for LIC-style Comprehensive Insurance Chart reports, including policy holder, policy number, sum assured, premium, nominee, plan and accidental risk cover.
- Duplicate policy numbers are skipped during bulk import.

## Performance
- Removed the full SMS inbox scan from every app resume.
- New SMS continue to arrive through the SMS receiver; background inbox reconciliation is delayed and throttled to avoid slowing app startup.
- Existing SMS reconciliation remains available from Settings.

## Security
- Added native Android app lock using fingerprint/face/device PIN through BiometricPrompt.
- App lock is enabled by default and can be changed in Settings → App security.
- No fingerprint, face data or PIN is stored by SMS Ledger.

## Build
- versionName: 1.111
- versionCode: 106
- Java/Kotlin target: 17
- Android Gradle Plugin project remains compatible with the existing Gradle 8.9 workflow.
