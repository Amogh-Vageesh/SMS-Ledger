# SMS Ledger v1.136

## Targeted fixes
- Normalize transaction date keys before applying My Ledger period bounds, avoiding mismatches when stored dates include alternate separators or time suffixes.
- Harden recurring transaction matching across explicit subscription keys, recurring keys, and normalized merchant names.
- Keep linked recurring payment history discoverable when the selected period has no payments.
- Place a Protection Share action in the top-right of the Protection summary header, matching the Total Balance header pattern.
- Increase Protection summary metric value weight/size for closer consistency with the other summary cards.

## Validation note
Source-level checks only; Android runtime behavior and full APK build must still be verified in Android Studio/GitHub Actions.
