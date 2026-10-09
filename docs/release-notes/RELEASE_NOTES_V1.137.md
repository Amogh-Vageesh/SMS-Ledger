# SMS Ledger v1.137 — My Ledger period and recurring-payment correction

## Source changes
- My Ledger period selection now filters all dated transactions for the selected calendar month, quarter, year, or all years. Transaction categories excluded from income/expense totals are still available in the appropriate transaction/transfer lists.
- Income and expense totals only include countable income/expense records; internal transfers are not treated as income or expense.
- Date comparisons normalize ISO dates, slash-separated dates, and ISO date-time strings before filtering.
- Recurring transaction matching now checks explicit subscription/recurring keys, normalized service names, merchant-name containment, and overlapping merchant/service tokens. Linked transaction date filtering uses the same normalized date key.
- Protection's Share control uses the same compact share-button sizing as the Total Balance header; summary metric values have stronger typography while insurance cover, premiums, warranty cost, and maintenance cost remain distinct.

## Validation performed
- JavaScript syntax check passed for the embedded application script.
- Targeted runtime tests passed for Month, Quarter, Yearly, All Years date filtering, and matching a recurring subscription to a normalized merchant variant.
- ZIP integrity check passed after packaging.

## Limitations
- An Android APK was not built in this environment: Gradle and an Android SDK are not installed, and the project has no Gradle wrapper. The Android/GitHub Actions build and on-device UI behavior still require verification.
- These tests cover the targeted date/matching functions; they do not prove every app requirement or cloud-family sharing workflow end-to-end.
