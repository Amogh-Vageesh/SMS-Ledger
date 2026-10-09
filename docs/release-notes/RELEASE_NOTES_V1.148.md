# SMS Ledger v1.148

## Fixes
- Year summary filters transactions using the actual calendar year from the transaction date, rather than the month-start-adjusted period key.
- Protection list prioritizes the covered property, vehicle, item, or product name; provider remains secondary metadata, and expiry date remains visible.
- Liability list shows the remaining loan amount immediately below the pending amount on the right; loan original amount remains available as secondary detail.

## Validation
- Inline JavaScript syntax checked with Node.js.
- ZIP integrity checked.
- Android Gradle build and on-device runtime tests were not run in this environment.
