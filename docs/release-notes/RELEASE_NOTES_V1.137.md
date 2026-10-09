# SMS Ledger v1.137

## Focused functional corrections
- Ledger period filtering now reads from the saved transaction store and applies canonical YYYY-MM-DD date keys for Month, Quarter, Yearly, and All Years.
- All Years derives its boundaries from saved transactions rather than a potentially pre-filtered display list.
- Subscription linked-transaction lookup now reads saved transactions and supports normalized merchant-name matching when exact merchant text differs.
- Subscription period totals use the same canonical date normalization as My Ledger.

## Validation notes
- Source-level checks and ZIP integrity are run for this package.
- Android compilation and device-level behavior still require a real Gradle/Android build and runtime test.
